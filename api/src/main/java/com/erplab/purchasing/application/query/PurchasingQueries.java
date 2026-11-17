package com.erplab.purchasing.application.query;

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
public class PurchasingQueries {

	public record PurchaseOrderView(Long id, String number, Long supplierId, String supplierName, Long warehouseId,
			String warehouseCode, LocalDate orderDate, LocalDate expectedDate, String status, BigDecimal total,
			String note) {
	}

	public record PurchaseOrderLineView(Long id, Long productId, String sku, String productName, String unitCode,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, BigDecimal receivedQuantity,
			BigDecimal billedQuantity) {
	}

	public record BillRef(Long id, String number, LocalDate billDate, BigDecimal total, String status) {
	}

	public record PurchaseOrderDetail(PurchaseOrderView order, List<PurchaseOrderLineView> lines, List<BillRef> bills) {
	}

	public record BillView(Long id, String number, String supplierReference, Long purchaseOrderId,
			String purchaseOrderNumber, Long supplierId, String supplierName, LocalDate billDate, LocalDate dueDate,
			BigDecimal total, BigDecimal paidAmount, BigDecimal balance, String status, boolean overdue) {
	}

	public record BillLineView(Long productId, String sku, String productName, String unitCode, BigDecimal quantity,
			BigDecimal unitPrice, BigDecimal amount) {
	}

	public record BillPaymentView(Long id, LocalDate paymentDate, BigDecimal amount, String method, String reference) {
	}

	public record BillDetail(BillView bill, List<BillLineView> lines, List<BillPaymentView> payments) {
	}

	private static final String BILL_COLUMNS = """
			b.id, b.number, b.supplier_reference, b.purchase_order_id, o.number AS purchase_order_number, b.supplier_id,
			s.name AS supplier_name, b.bill_date, b.due_date, b.total, b.paid_amount, b.total - b.paid_amount AS balance,
			b.status, (b.status <> 'PAID' AND b.due_date < current_date) AS overdue""";

	private static final String BILL_FROM = """
			supplier_bill b JOIN partner s ON s.id = b.supplier_id JOIN purchase_order o ON o.id = b.purchase_order_id
			WHERE b.deleted_at IS NULL""";

	private static final String COLUMNS = """
			o.id, o.number, o.supplier_id, s.name AS supplier_name, o.warehouse_id, w.code AS warehouse_code,
			o.order_date, o.expected_date, o.status, o.total, o.note""";

	private static final String FROM = """
			purchase_order o JOIN partner s ON s.id = o.supplier_id JOIN warehouse w ON w.id = o.warehouse_id
			WHERE o.deleted_at IS NULL""";

	private final JdbcClient jdbc;

	private final SqlPage sql;

	PurchasingQueries(JdbcClient jdbc, SqlPage sql) {
		this.jdbc = jdbc;
		this.sql = sql;
	}

	public PageResult<PurchaseOrderView> list(String q, String status, Long supplierId, int page, int size) {
		return sql.query(COLUMNS, FROM + " " + """
				 AND (o.number ILIKE :q OR s.name ILIKE :q)
				 AND (CAST(:status AS text) IS NULL OR o.status = :status)
				 AND (CAST(:supplierId AS bigint) IS NULL OR o.supplier_id = :supplierId)""",
				Params.of("q", SqlPage.like(q), "status", status, "supplierId", supplierId), "o.id DESC", page, size,
				PurchaseOrderView.class);
	}

	public PurchaseOrderDetail get(Long id) {
		var order = jdbc.sql("SELECT " + COLUMNS + " FROM " + FROM + " AND o.id = :id")
			.param("id", id)
			.query(PurchaseOrderView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Purchase order", id));
		var lines = jdbc.sql("""
				SELECT l.id, l.product_id, p.sku, p.name AS product_name, u.code AS unit_code, l.quantity, l.unit_price,
				       round(l.quantity * l.unit_price, 2) AS amount, l.received_quantity,
				       l.billed_quantity
				FROM purchase_order_line l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.purchase_order_id = :id ORDER BY l.id""")
			.param("id", id)
			.query(PurchaseOrderLineView.class)
			.list();
		var bills = jdbc.sql("SELECT id, number, bill_date, total, status FROM supplier_bill WHERE purchase_order_id = :id ORDER BY id")
			.param("id", id)
			.query(BillRef.class)
			.list();
		return new PurchaseOrderDetail(order, lines, bills);
	}

	public PageResult<BillView> bills(String q, String status, Long supplierId, boolean overdueOnly, int page, int size) {
		return sql.query(BILL_COLUMNS, BILL_FROM + " " + """
				 AND (b.number ILIKE :q OR s.name ILIKE :q OR o.number ILIKE :q OR coalesce(b.supplier_reference, '') ILIKE :q)
				 AND (CAST(:status AS text) IS NULL OR b.status = :status)
				 AND (CAST(:supplierId AS bigint) IS NULL OR b.supplier_id = :supplierId)
				 AND (NOT :overdueOnly OR (b.status <> 'PAID' AND b.due_date < current_date))""",
				Params.of("q", SqlPage.like(q), "status", status, "supplierId", supplierId, "overdueOnly", overdueOnly),
				"b.id DESC", page, size, BillView.class);
	}

	public BillDetail bill(Long id) {
		var bill = jdbc.sql("SELECT " + BILL_COLUMNS + " FROM " + BILL_FROM + " AND b.id = :id")
			.param("id", id)
			.query(BillView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Bill", id));
		var lines = jdbc.sql("""
				SELECT l.product_id, p.sku, p.name AS product_name, u.code AS unit_code, l.quantity, l.unit_price,
				       round(l.quantity * l.unit_price, 2) AS amount
				FROM supplier_bill_line l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.bill_id = :id ORDER BY l.position""")
			.param("id", id)
			.query(BillLineView.class)
			.list();
		var payments = jdbc.sql("SELECT id, payment_date, amount, method, reference FROM bill_payment WHERE bill_id = :id ORDER BY id")
			.param("id", id)
			.query(BillPaymentView.class)
			.list();
		return new BillDetail(bill, lines, payments);
	}

}
