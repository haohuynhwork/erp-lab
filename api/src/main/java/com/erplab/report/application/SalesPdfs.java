package com.erplab.report.application;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
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

/** Printable sales documents: invoice, and sales order (a quotation while Draft, else an order confirmation). */
@Service
public class SalesPdfs {

	public record Pdf(String fileName, byte[] content) {
	}

	record Party(String name, String address, String taxCode) {
	}

	record Line(String sku, String name, String unitCode, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
	}

	record Total(String label, BigDecimal value, boolean bold) {
	}

	record InvoiceHeader(String number, LocalDate issueDate, LocalDate dueDate, String customerName,
			String customerAddress, String customerTaxCode, String salesOrderNumber, BigDecimal taxRate,
			BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total, BigDecimal paidAmount,
			BigDecimal creditedAmount) {
	}

	record OrderHeader(String number, String status, LocalDate orderDate, LocalDate deliveryDate, LocalDate validUntil,
			String customerName, String customerAddress, String customerTaxCode, BigDecimal taxRate, BigDecimal subtotal,
			BigDecimal taxAmount, BigDecimal total, String note) {
	}

	private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

	private static final DecimalFormat QTY = new DecimalFormat("#,##0.####");

	private final JdbcClient jdbc;

	SalesPdfs(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Pdf invoice(Long id) {
		var h = jdbc.sql("""
				SELECT i.number, i.issue_date, i.due_date, c.name AS customer_name, c.address AS customer_address,
				       c.tax_code AS customer_tax_code, o.number AS sales_order_number, i.tax_rate, i.subtotal, i.tax_amount,
				       i.total, i.paid_amount, i.credited_amount
				FROM invoice i JOIN partner c ON c.id = i.customer_id JOIN sales_order o ON o.id = i.sales_order_id
				WHERE i.id = :id""")
			.param("id", id)
			.query(InvoiceHeader.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Invoice", id));
		var lines = lines("invoice_line", "invoice_id", "l.position", id);
		var totals = new ArrayList<>(List.of(new Total("Subtotal", h.subtotal(), false),
				new Total("VAT " + QTY.format(h.taxRate()) + "%", h.taxAmount(), false),
				new Total("Total", h.total(), true), new Total("Paid", h.paidAmount(), false)));
		if (h.creditedAmount().signum() > 0) {
			totals.add(new Total("Credited", h.creditedAmount(), false));
		}
		totals.add(new Total("Balance due", h.total().subtract(h.paidAmount()).subtract(h.creditedAmount()), true));
		var content = render("INVOICE " + h.number(), new Party(h.customerName(), h.customerAddress(), h.customerTaxCode()),
				"Issue date: " + h.issueDate() + "\nDue date: " + h.dueDate() + "\nOrder: " + h.salesOrderNumber(), lines,
				totals, null);
		return new Pdf(h.number() + ".pdf", content);
	}

	public Pdf salesOrder(Long id) {
		var h = jdbc.sql("""
				SELECT o.number, o.status, o.order_date, o.delivery_date, o.valid_until, c.name AS customer_name,
				       c.address AS customer_address, c.tax_code AS customer_tax_code, o.tax_rate, o.subtotal, o.tax_amount,
				       o.total, o.note
				FROM sales_order o JOIN partner c ON c.id = o.customer_id WHERE o.id = :id AND o.deleted_at IS NULL""")
			.param("id", id)
			.query(OrderHeader.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Sales order", id));
		boolean quotation = "DRAFT".equals(h.status());
		var meta = new StringBuilder("Date: " + h.orderDate());
		if (quotation && h.validUntil() != null) {
			meta.append("\nValid until: ").append(h.validUntil());
		}
		if (h.deliveryDate() != null) {
			meta.append("\nDelivery: ").append(h.deliveryDate());
		}
		var content = render((quotation ? "QUOTATION " : "ORDER CONFIRMATION ") + h.number(),
				new Party(h.customerName(), h.customerAddress(), h.customerTaxCode()), meta.toString(),
				lines("sales_order_line", "sales_order_id", "l.id", id),
				List.of(new Total("Subtotal", h.subtotal(), false),
						new Total("VAT " + QTY.format(h.taxRate()) + "%", h.taxAmount(), false),
						new Total("Total", h.total(), true)),
				h.note());
		return new Pdf((quotation ? "quotation-" : "order-") + h.number() + ".pdf", content);
	}

	private List<Line> lines(String table, String fk, String order, Long id) {
		return jdbc.sql("""
				SELECT p.sku, p.name, u.code AS unit_code, l.quantity, l.unit_price, round(l.quantity * l.unit_price, 2) AS amount
				FROM %s l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.%s = :id ORDER BY %s""".formatted(table, fk, order))
			.param("id", id)
			.query(Line.class)
			.list();
	}

	private static byte[] render(String title, Party party, String meta, List<Line> lines, List<Total> totals,
			String note) {
		var out = new ByteArrayOutputStream();
		var doc = new Document(PageSize.A4, 40, 40, 40, 40);
		PdfWriter.getInstance(doc, out);
		doc.open();
		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
		Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
		Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
		Font muted = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

		doc.add(new Paragraph(title, titleFont));
		doc.add(new Paragraph("ERP Lab Manufacturing", muted));
		doc.add(new Paragraph(" "));

		var header = new PdfPTable(2);
		header.setWidthPercentage(100);
		header.addCell(plain("Customer\n" + party.name() + "\n" + nz(party.address())
				+ (party.taxCode() != null ? "\nTax code: " + party.taxCode() : ""), normal));
		header.addCell(plain(meta, normal));
		doc.add(header);
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

		var sums = new PdfPTable(new float[] { 8, 2 });
		sums.setWidthPercentage(100);
		for (var t : totals) {
			sums.addCell(right(t.label(), t.bold() ? bold : normal));
			sums.addCell(right(MONEY.format(t.value()), t.bold() ? bold : normal));
		}
		doc.add(sums);
		if (note != null && !note.isBlank()) {
			doc.add(new Paragraph(" "));
			doc.add(new Paragraph(note, muted));
		}
		doc.close();
		return out.toByteArray();
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
