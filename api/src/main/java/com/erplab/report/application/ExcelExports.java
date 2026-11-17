package com.erplab.report.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Excel exports: each is one SQL query written to a sheet, column names as headers. */
@Service
public class ExcelExports {

	public enum Report {

		STOCK("Stock", """
				SELECT p.sku AS "SKU", p.name AS "Product", w.code AS "Warehouse", u.code AS "Unit",
				       s.quantity AS "Quantity", s.average_cost AS "Average cost",
				       round(s.quantity * s.average_cost, 2) AS "Value", p.min_stock AS "Min stock"
				FROM stock_item s JOIN product p ON p.id = s.product_id JOIN warehouse w ON w.id = s.warehouse_id
				LEFT JOIN unit u ON u.id = p.unit_id ORDER BY p.sku, w.code"""),
		SALES_ORDERS("Sales orders", """
				SELECT o.number AS "Number", o.order_date AS "Date", c.name AS "Customer", o.status AS "Status",
				       o.subtotal AS "Subtotal", o.tax_amount AS "Tax", o.total AS "Total"
				FROM sales_order o JOIN partner c ON c.id = o.customer_id WHERE o.deleted_at IS NULL ORDER BY o.id DESC"""),
		PURCHASE_ORDERS("Purchase orders", """
				SELECT o.number AS "Number", o.order_date AS "Date", s.name AS "Supplier", o.status AS "Status",
				       o.total AS "Total"
				FROM purchase_order o JOIN partner s ON s.id = o.supplier_id WHERE o.deleted_at IS NULL ORDER BY o.id DESC"""),
		INVOICES("Invoices", """
				SELECT i.number AS "Number", i.issue_date AS "Date", i.due_date AS "Due", c.name AS "Customer",
				       i.status AS "Status", i.total AS "Total", i.paid_amount AS "Paid", i.total - i.paid_amount AS "Balance"
				FROM invoice i JOIN partner c ON c.id = i.customer_id ORDER BY i.id DESC"""),
		TRIAL_BALANCE("Trial balance", """
				SELECT a.code AS "Code", a.name AS "Account", a.type AS "Type",
				       coalesce(sum(l.debit), 0) AS "Debit", coalesce(sum(l.credit), 0) AS "Credit"
				FROM account a LEFT JOIN journal_line l ON l.account_id = a.id
				GROUP BY a.id ORDER BY a.code""");

		final String title;

		final String sql;

		Report(String title, String sql) {
			this.title = title;
			this.sql = sql;
		}

		public String fileName() {
			return name().toLowerCase().replace('_', '-') + "-" + LocalDate.now() + ".xlsx";
		}

	}

	private final JdbcClient jdbc;

	ExcelExports(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public byte[] export(Report report) {
		try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var sheet = wb.createSheet(report.title);
			var bold = wb.createCellStyle();
			var font = wb.createFont();
			font.setBold(true);
			bold.setFont(font);
			var date = wb.createCellStyle();
			date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
			var number = wb.createCellStyle();
			number.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
			jdbc.sql(report.sql).query((ResultSet rs) -> {
				write(sheet, rs, bold, date, number);
			});
			sheet.createFreezePane(0, 1);
			wb.write(out);
			return out.toByteArray();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void write(Sheet sheet, ResultSet rs, CellStyle bold, CellStyle date, CellStyle number)
			throws SQLException {
		var meta = rs.getMetaData();
		int cols = meta.getColumnCount();
		Row header = sheet.createRow(0);
		for (int c = 0; c < cols; c++) {
			var cell = header.createCell(c);
			cell.setCellValue(meta.getColumnLabel(c + 1));
			cell.setCellStyle(bold);
		}
		int r = 1;
		while (rs.next()) {
			Row row = sheet.createRow(r++);
			for (int c = 0; c < cols; c++) {
				var cell = row.createCell(c);
				switch (meta.getColumnType(c + 1)) {
					case Types.NUMERIC, Types.DECIMAL, Types.INTEGER, Types.BIGINT -> {
						BigDecimal v = rs.getBigDecimal(c + 1);
						if (v != null) {
							cell.setCellValue(v.doubleValue());
							cell.setCellStyle(number);
						}
					}
					case Types.DATE -> {
						var v = rs.getDate(c + 1);
						if (v != null) {
							cell.setCellValue(v.toLocalDate());
							cell.setCellStyle(date);
						}
					}
					default -> cell.setCellValue(rs.getString(c + 1));
				}
			}
		}
		for (int c = 0; c < cols; c++) {
			sheet.autoSizeColumn(c);
		}
	}

}
