package com.erplab.masterdata.application.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erplab.common.domain.NotFoundException;
import com.erplab.common.http.PageResult;
import com.erplab.common.infrastructure.Params;
import com.erplab.common.infrastructure.SqlPage;

@Service
public class MasterdataQueries {

	public record UnitView(Long id, String code, String name) {
	}

	public record CategoryView(Long id, String name, String description) {
	}

	public record ProductView(Long id, String sku, String name, String type, Long unitId, String unitCode,
			Long categoryId, String categoryName, BigDecimal purchasePrice, BigDecimal salePrice, BigDecimal minStock,
			boolean active) {
	}

	public record BomLineView(Long componentId, String componentSku, String componentName, String unitCode,
			BigDecimal quantity) {
	}

	public record ProductDetail(ProductView product, List<BomLineView> bom) {
	}

	public record WarehouseView(Long id, String code, String name, String address, boolean active) {
	}

	public record PartnerView(Long id, String code, String name, boolean customer, boolean supplier, String taxCode,
			String email, String phone, String address, int paymentTermDays, boolean active) {
	}

	private static final String PRODUCT_COLUMNS = """
			p.id, p.sku, p.name, p.type, p.unit_id, u.code AS unit_code, p.category_id, c.name AS category_name,
			p.purchase_price, p.sale_price, p.min_stock, p.active""";

	private static final String PRODUCT_FROM = """
			product p LEFT JOIN unit u ON u.id = p.unit_id LEFT JOIN product_category c ON c.id = p.category_id
			WHERE p.deleted_at IS NULL""";

	private final JdbcClient jdbc;

	private final SqlPage sql;

	MasterdataQueries(JdbcClient jdbc, SqlPage sql) {
		this.jdbc = jdbc;
		this.sql = sql;
	}

	public PageResult<UnitView> units(String q, int page, int size) {
		return sql.query("id, code, name",
				"unit WHERE deleted_at IS NULL AND (code ILIKE :q OR name ILIKE :q)",
				Map.of("q", SqlPage.like(q)), "code", page, size, UnitView.class);
	}

	public PageResult<CategoryView> categories(String q, int page, int size) {
		return sql.query("id, name, description", "product_category WHERE deleted_at IS NULL AND name ILIKE :q",
				Map.of("q", SqlPage.like(q)), "name", page, size, CategoryView.class);
	}

	public PageResult<ProductView> products(String q, String type, Boolean active, int page, int size) {
		return sql.query(PRODUCT_COLUMNS, PRODUCT_FROM + " " + """
				 AND (p.sku ILIKE :q OR p.name ILIKE :q)
				 AND (CAST(:type AS text) IS NULL OR p.type = :type)
				 AND (CAST(:active AS boolean) IS NULL OR p.active = :active)""",
				Params.of("q", SqlPage.like(q), "type", type, "active", active), "p.sku", page, size,
				ProductView.class);
	}

	public ProductDetail product(Long id) {
		var product = jdbc.sql("SELECT " + PRODUCT_COLUMNS + " FROM " + PRODUCT_FROM + " AND p.id = :id")
			.param("id", id)
			.query(ProductView.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Product", id));
		var bom = jdbc.sql("""
				SELECT b.component_id, c.sku AS component_sku, c.name AS component_name, u.code AS unit_code, b.quantity
				FROM product_bom_line b JOIN product c ON c.id = b.component_id LEFT JOIN unit u ON u.id = c.unit_id
				WHERE b.product_id = :id ORDER BY b.position""")
			.param("id", id)
			.query(BomLineView.class)
			.list();
		return new ProductDetail(product, bom);
	}

	public PageResult<WarehouseView> warehouses(String q, int page, int size) {
		return sql.query("id, code, name, address, active",
				"warehouse WHERE deleted_at IS NULL AND (code ILIKE :q OR name ILIKE :q)",
				Map.of("q", SqlPage.like(q)), "code", page, size, WarehouseView.class);
	}

	/** @param role "customer", "supplier" or null for both */
	public PageResult<PartnerView> partners(String q, String role, int page, int size) {
		String roleFilter = switch (role == null ? "" : role) {
			case "customer" -> " AND customer";
			case "supplier" -> " AND supplier";
			default -> "";
		};
		return sql.query(
				"id, code, name, customer, supplier, tax_code, email, phone, address, payment_term_days, active",
				"partner WHERE deleted_at IS NULL AND (code ILIKE :q OR name ILIKE :q)" + roleFilter,
				Map.of("q", SqlPage.like(q)), "code", page, size, PartnerView.class);
	}

	public UnitView unit(Long id) {
		return get("SELECT id, code, name FROM unit", id, UnitView.class, "Unit");
	}

	public CategoryView category(Long id) {
		return get("SELECT id, name, description FROM product_category", id, CategoryView.class, "Category");
	}

	public WarehouseView warehouse(Long id) {
		return get("SELECT id, code, name, address, active FROM warehouse", id, WarehouseView.class, "Warehouse");
	}

	public PartnerView partner(Long id) {
		return get("""
				SELECT id, code, name, customer, supplier, tax_code, email, phone, address, payment_term_days, active
				FROM partner""", id, PartnerView.class, "Partner");
	}

	private <T> T get(String select, Long id, Class<T> type, String what) {
		return jdbc.sql(select + " WHERE id = :id AND deleted_at IS NULL")
			.param("id", id)
			.query(type)
			.optional()
			.orElseThrow(() -> new NotFoundException(what, id));
	}

}
