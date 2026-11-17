package com.erplab.report.application;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erplab.common.domain.NotFoundException;

/** Printable customer invoice. */
@Service
public class InvoicePdf {

	record Header(String number, LocalDate issueDate, LocalDate dueDate, String customerName, String customerAddress,
			String customerTaxCode, String salesOrderNumber, BigDecimal taxRate, BigDecimal subtotal, BigDecimal taxAmount,
			BigDecimal total, BigDecimal paidAmount) {
	}

	record Line(String sku, String name, String unitCode, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
	}

	private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

	private static final DecimalFormat QTY = new DecimalFormat("#,##0.####");

	private final JdbcClient jdbc;

	InvoicePdf(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public String fileName(Long id) {
		return header(id).number() + ".pdf";
	}

	public byte[] render(Long id) {
		var h = header(id);
		List<Line> lines = jdbc.sql("""
				SELECT p.sku, p.name, u.code AS unit_code, l.quantity, l.unit_price, round(l.quantity * l.unit_price, 2) AS amount
				FROM invoice_line l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.invoice_id = :id ORDER BY l.position""")
			.param("id", id)
			.query(Line.class)
			.list();

		var out = new ByteArrayOutputStream();
		var doc = new Document(PageSize.A4, 40, 40, 40, 40);
		PdfWriter.getInstance(doc, out);
		doc.open();
		Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
		Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
		Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
		Font muted = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

		doc.add(new Paragraph("INVOICE " + h.number(), title));
		doc.add(new Paragraph("ERP Lab Manufacturing", muted));
		doc.add(new Paragraph(" "));

		var meta = new PdfPTable(2);
		meta.setWidthPercentage(100);
		meta.addCell(plain("Bill to\n" + h.customerName() + "\n" + nz(h.customerAddress())
				+ (h.customerTaxCode() != null ? "\nTax code: " + h.customerTaxCode() : ""), normal));
		meta.addCell(plain("Issue date: " + h.issueDate() + "\nDue date: " + h.dueDate() + "\nOrder: "
				+ h.salesOrderNumber(), normal));
		doc.add(meta);
		doc.add(new Paragraph(" "));

		var table = new PdfPTable(new float[] { 2, 5, 1, 2, 2, 2 });
		table.setWidthPercentage(100);
		for (var col : List.of("SKU", "Product", "Unit", "Qty", "Unit price", "Amount")) {
			var cell = new PdfPCell(new Phrase(col, bold));
			cell.setBackgroundColor(new Color(240, 236, 248));
			table.addCell(cell);
		}
		for (var l : lines) {
			table.addCell(new Phrase(l.sku(), normal));
			table.addCell(new Phrase(l.name(), normal));
			table.addCell(new Phrase(nz(l.unitCode()), normal));
			table.addCell(right(QTY.format(l.quantity()), normal));
			table.addCell(right(MONEY.format(l.unitPrice()), normal));
			table.addCell(right(MONEY.format(l.amount()), normal));
		}
		doc.add(table);

		var totals = new PdfPTable(new float[] { 8, 2 });
		totals.setWidthPercentage(100);
		addTotal(totals, "Subtotal", h.subtotal(), normal);
		addTotal(totals, "VAT " + QTY.format(h.taxRate()) + "%", h.taxAmount(), normal);
		addTotal(totals, "Total", h.total(), bold);
		addTotal(totals, "Paid", h.paidAmount(), normal);
		addTotal(totals, "Balance due", h.total().subtract(h.paidAmount()), bold);
		doc.add(totals);
		doc.close();
		return out.toByteArray();
	}

	private Header header(Long id) {
		return jdbc.sql("""
				SELECT i.number, i.issue_date, i.due_date, c.name AS customer_name, c.address AS customer_address,
				       c.tax_code AS customer_tax_code, o.number AS sales_order_number, i.tax_rate, i.subtotal, i.tax_amount,
				       i.total, i.paid_amount
				FROM invoice i JOIN partner c ON c.id = i.customer_id JOIN sales_order o ON o.id = i.sales_order_id
				WHERE i.id = :id""")
			.param("id", id)
			.query(Header.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Invoice", id));
	}

	private static void addTotal(PdfPTable table, String label, BigDecimal value, Font font) {
		table.addCell(right(label, font));
		table.addCell(right(MONEY.format(value), font));
	}

	private static PdfPCell plain(String text, Font font) {
		var cell = new PdfPCell(new Phrase(text, font));
		cell.setBorder(0);
		return cell;
	}

	private static PdfPCell right(String text, Font font) {
		var cell = new PdfPCell(new Phrase(text, font));
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setBorder(0);
		return cell;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

}
