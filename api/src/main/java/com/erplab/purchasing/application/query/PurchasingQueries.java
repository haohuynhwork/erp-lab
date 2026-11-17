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

	public record OrderView(Long id, String number, Long supplierId, String supplierName, Long warehouseId,
			String warehouseCode, LocalDate orderDate, LocalDate expectedDate, String status, BigDecimal total,
			String note) {
	}

	public record LineView(Long id, Long productId, String sku, String productName, String unitCode,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, BigDecimal receivedQuantity) {
	}

	public record OrderDetail(OrderView order, List<LineView> lines) {
	}

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

	public PageResult<OrderView> list(String q, String status, Long supplierId, int page, int size) {
		return sql.query(COLUMNS, FROM + " " + """
				 AND (o.number ILIKE :q OR s.name ILIKE :q)
				 AND (CAST(:status AS text) IS NULL OR o.status = :status)
				 AND (CAST(:supplierId AS bigint) IS NULL OR o.supplier_id = :supplierId)""",
				Params.of("q", SqlPage.like(q), "status", status, "supplierId", supplierId), "o.id DESC", page, size,
				OrderView.class);
	}

	public OrderDetail get(Long id) {
		var order = jdbc.sql("SELECT " + COLUMNS + " FROM " + FROM + " AND o.id = :id")
			.param("id", id)
			.query(OrderView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Purchase order", id));
		var lines = jdbc.sql("""
				SELECT l.id, l.product_id, p.sku, p.name AS product_name, u.code AS unit_code, l.quantity, l.unit_price,
				       round(l.quantity * l.unit_price, 2) AS amount, l.received_quantity
				FROM purchase_order_line l JOIN product p ON p.id = l.product_id LEFT JOIN unit u ON u.id = p.unit_id
				WHERE l.purchase_order_id = :id ORDER BY l.id""")
			.param("id", id)
			.query(LineView.class)
			.list();
		return new OrderDetail(order, lines);
	}

}
