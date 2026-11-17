package com.erplab.sales.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erplab.common.domain.NotFoundException;
import com.erplab.common.http.PageResult;
import com.erplab.common.infrastructure.Params;
import com.erplab.common.infrastructure.SqlPage;

@Service
public class SalesQueries {

	public record SalesOrderView(Long id, String number, Long customerId, String customerName, Long warehouseId,
			String warehouseCode, LocalDate orderDate, LocalDate deliveryDate, LocalDate validUntil, String status, BigDecimal taxRate,
			BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total, String note) {
	}

	public record SalesOrderLineView(Long id, Long productId, String sku, String productName, String unitCode,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, BigDecimal deliveredQuantity,
			BigDecimal invoicedQuantity) {
	}

	public record InvoiceRef(Long id, String number, LocalDate issueDate, BigDecimal total, String status) {
	}

	public record SalesOrderDetail(SalesOrderView order, List<SalesOrderLineView> lines, List<InvoiceRef> invoices) {
	}

	public record InvoiceView(Long id, String number, Long salesOrderId, String salesOrderNumber, Long customerId,
			String customerName, LocalDate issueDate, LocalDate dueDate, BigDecimal taxRate, BigDecimal subtotal,
			BigDecimal taxAmount, BigDecimal total, BigDecimal paidAmount, BigDecimal creditedAmount, BigDecimal balance,
			String status, boolean overdue) {
	}

	public record InvoiceLineView(int position, Long productId, String sku, String productName, String unitCode,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, BigDecimal creditedQuantity) {
	}

	public record CreditNoteRef(Long id, String number, LocalDate issueDate, String reason, BigDecimal total) {
	}

	public record CreditNoteView(Long id, String number, Long invoiceId, String invoiceNumber, Long customerId,
			String customerName, LocalDate issueDate, String reason, BigDecimal subtotal, BigDecimal taxAmount,
			BigDecimal total) {
	}

	public record InvoicePaymentView(Long id, LocalDate paymentDate, BigDecimal amount, String method, String reference) {
	}

	public record InvoiceDetail(InvoiceView invoice, List<InvoiceLineView> lines, List<InvoicePaymentView> payments,
			List<CreditNoteRef> creditNotes) {
	}

	private static final String ORDER_COLUMNS = """
			o.id, o.number, o.customer_id, c.name AS customer_name, o.warehouse_id, w.code AS warehouse_code, o.order_date,
			o.delivery_date, o.valid_until, o.status, o.tax_rate, o.subtotal, o.tax_amount, o.total, o.note""";

	private static final String ORDER_FROM = """
			sales_order o JOIN partner c ON c.id = o.customer_id JOIN warehouse w ON w.id = o.warehouse_id
			WHERE o.deleted_at IS NULL""";

	private static final String INVOICE_COLUMNS = """
			i.id, i.number, i.sales_order_id, o.number AS sales_order_number, i.customer_id, c.name AS customer_name,
			i.issue_date, i.due_date, i.tax_rate, i.subtotal, i.tax_amount, i.total, i.paid_amount, i.credited_amount,
			i.total - i.paid_amount - i.credited_amount AS balance, i.status,
			(i.status IN ('ISSUED', 'PARTIALLY_PAID') AND i.due_date < current_date) AS overdue""";

	private static final String INVOICE_FROM = """
			invoice i JOIN partner c ON c.id = i.customer_id JOIN sales_order o ON o.id = i.sales_order_id
			WHERE i.deleted_at IS NULL""";

	private final JdbcClient jdbc;

	private final SqlPage sql;

	SalesQueries(JdbcClient jdbc, SqlPage sql) {
		this.jdbc = jdbc;
		this.sql = sql;
	}

	public PageResult<SalesOrderView> orders(String q, String status, Long customerId, int page, int size) {
		return sql.query(ORDER_COLUMNS, ORDER_FROM + " " + """
				 AND (o.number ILIKE :q OR c.name ILIKE :q)
				 AND (CAST(:status AS text) IS NULL OR o.status = :status)
				 AND (CAST(:customerId AS bigint) IS NULL OR o.customer_id = :customerId)""",
				Params.of("q", SqlPage.like(q), "status", status, "customerId", customerId), "o.id DESC", page, size,
				SalesOrderView.class);
	}

	public SalesOrderDetail order(Long id) {
		var order = jdbc.sql("SELECT " + ORDER_COLUMNS + " FROM " + ORDER_FROM + " AND o.id = :id")
			.param("id", id)
			.query(SalesOrderView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Sales order", id));
		var lines = jdbc.sql("""
				SELECT l.id, l.product_id, p.sku, p.name AS product_name, u.code AS unit_code, l.quantity, l.unit_price,
				       round(l.quantity * l.unit_price, 2) AS amount, l.delivered_quantity, l.invoiced_quantity
				FROM sales_order_line l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.sales_order_id = :id ORDER BY l.id""")
			.param("id", id)
			.query(SalesOrderLineView.class)
			.list();
		var invoices = jdbc.sql("SELECT id, number, issue_date, total, status FROM invoice WHERE sales_order_id = :id ORDER BY id")
			.param("id", id)
			.query(InvoiceRef.class)
			.list();
		return new SalesOrderDetail(order, lines, invoices);
	}

	public PageResult<InvoiceView> invoices(String q, String status, Long customerId, boolean overdueOnly, int page,
			int size) {
		return sql.query(INVOICE_COLUMNS, INVOICE_FROM + " " + """
				 AND (i.number ILIKE :q OR c.name ILIKE :q OR o.number ILIKE :q)
				 AND (CAST(:status AS text) IS NULL OR i.status = :status)
				 AND (CAST(:customerId AS bigint) IS NULL OR i.customer_id = :customerId)
				 AND (NOT :overdueOnly OR (i.status IN ('ISSUED', 'PARTIALLY_PAID') AND i.due_date < current_date))""",
				Params.of("q", SqlPage.like(q), "status", status, "customerId", customerId, "overdueOnly", overdueOnly),
				"i.id DESC", page, size, InvoiceView.class);
	}

	public InvoiceDetail invoice(Long id) {
		var invoice = jdbc.sql("SELECT " + INVOICE_COLUMNS + " FROM " + INVOICE_FROM + " AND i.id = :id")
			.param("id", id)
			.query(InvoiceView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Invoice", id));
		var lines = jdbc.sql("""
				SELECT l.position, l.product_id, p.sku, p.name AS product_name, u.code AS unit_code, l.quantity, l.unit_price,
				       round(l.quantity * l.unit_price, 2) AS amount, l.credited_quantity
				FROM invoice_line l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.invoice_id = :id ORDER BY l.position""")
			.param("id", id)
			.query(InvoiceLineView.class)
			.list();
		var payments = jdbc.sql("""
				SELECT id, payment_date, amount, method, reference FROM invoice_payment WHERE invoice_id = :id ORDER BY id""")
			.param("id", id)
			.query(InvoicePaymentView.class)
			.list();
		var notes = jdbc.sql("SELECT id, number, issue_date, reason, total FROM credit_note WHERE invoice_id = :id ORDER BY id")
			.param("id", id)
			.query(CreditNoteRef.class)
			.list();
		return new InvoiceDetail(invoice, lines, payments, notes);
	}

	public PageResult<CreditNoteView> creditNotes(String q, Long customerId, int page, int size) {
		return sql.query("""
				n.id, n.number, n.invoice_id, n.invoice_number, n.customer_id, c.name AS customer_name, n.issue_date,
				n.reason, n.subtotal, n.tax_amount, n.total""", """
				credit_note n JOIN partner c ON c.id = n.customer_id
				WHERE (n.number ILIKE :q OR n.invoice_number ILIKE :q OR c.name ILIKE :q)
				  AND (CAST(:customerId AS bigint) IS NULL OR n.customer_id = :customerId)""",
				Params.of("q", SqlPage.like(q), "customerId", customerId), "n.id DESC", page, size, CreditNoteView.class);
	}

}
