package com.erplab.inventory.application.query;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.erplab.common.http.PageResult;
import com.erplab.common.infrastructure.Params;
import com.erplab.common.infrastructure.SqlPage;

@Service
public class InventoryQueries {

	public record StockView(Long productId, String sku, String productName, String unitCode, Long warehouseId,
			String warehouseCode, BigDecimal quantity, BigDecimal reserved, BigDecimal available, BigDecimal averageCost,
			BigDecimal value, BigDecimal minStock, boolean low) {
	}

	public record MovementView(Long id, Instant movedAt, String type, Long productId, String sku, String productName,
			Long warehouseId, String warehouseCode, BigDecimal quantity, BigDecimal value, String refType, Long refId,
			String refNumber, String note, String movedBy) {
	}

	private final SqlPage sql;

	InventoryQueries(SqlPage sql) {
		this.sql = sql;
	}

	public PageResult<StockView> stock(String q, Long warehouseId, Long productId, boolean lowOnly, int page, int size) {
		return sql.query("""
				s.product_id, p.sku, p.name AS product_name, u.code AS unit_code, s.warehouse_id, w.code AS warehouse_code,
				s.quantity, s.reserved, s.quantity - s.reserved AS available, s.average_cost,
				round(s.quantity * s.average_cost, 2) AS value, p.min_stock, s.quantity - s.reserved < p.min_stock AS low""", """
				stock_item s JOIN product p ON p.id = s.product_id JOIN warehouse w ON w.id = s.warehouse_id
				LEFT JOIN unit u ON u.id = p.unit_id
				WHERE (p.sku ILIKE :q OR p.name ILIKE :q)
				  AND (CAST(:warehouseId AS bigint) IS NULL OR s.warehouse_id = :warehouseId)
				  AND (CAST(:productId AS bigint) IS NULL OR s.product_id = :productId)
				  AND (NOT :lowOnly OR s.quantity - s.reserved < p.min_stock)""",
				Params.of("q", SqlPage.like(q), "warehouseId", warehouseId, "productId", productId, "lowOnly", lowOnly),
				"p.sku, w.code", page, size, StockView.class);
	}

	public PageResult<MovementView> movements(Long productId, Long warehouseId, String type, int page, int size) {
		return sql.query("""
				m.id, m.moved_at, m.type, m.product_id, p.sku, p.name AS product_name, m.warehouse_id,
				w.code AS warehouse_code, m.quantity, m.value, m.ref_type, m.ref_id, m.ref_number, m.note, m.moved_by""", """
				stock_movement m JOIN product p ON p.id = m.product_id JOIN warehouse w ON w.id = m.warehouse_id
				WHERE (CAST(:productId AS bigint) IS NULL OR m.product_id = :productId)
				  AND (CAST(:warehouseId AS bigint) IS NULL OR m.warehouse_id = :warehouseId)
				  AND (CAST(:type AS text) IS NULL OR m.type = :type)""",
				Params.of("productId", productId, "warehouseId", warehouseId, "type", type), "m.id DESC", page, size,
				MovementView.class);
	}

}
