package com.erplab.masterdata.application;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.erplab.common.domain.NotFoundException;
import com.erplab.masterdata.MasterdataApi;

@Service
class MasterdataApiImpl implements MasterdataApi {

	private final JdbcClient jdbc;

	MasterdataApiImpl(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public ProductRef product(Long id) {
		return jdbc.sql("""
				SELECT p.id, p.sku, p.name, p.type, u.code AS unit_code, p.purchase_price, p.sale_price,
					   p.type <> 'SERVICE' AS stocked, p.active
				FROM product p LEFT JOIN unit u ON u.id = p.unit_id
				WHERE p.id = ? AND p.deleted_at IS NULL""")
			.param(id)
			.query(ProductRef.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Product", id));
	}

	@Override
	public PartnerRef partner(Long id) {
		return jdbc.sql("""
				SELECT id, code, name, customer, supplier, payment_term_days
				FROM partner WHERE id = ? AND deleted_at IS NULL AND active""")
			.param(id)
			.query(PartnerRef.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Partner", id));
	}

	@Override
	public WarehouseRef warehouse(Long id) {
		return jdbc.sql("SELECT id, code, name, active FROM warehouse WHERE id = ? AND deleted_at IS NULL")
			.param(id)
			.query(WarehouseRef.class)
			.optional()
			.orElseThrow(() -> new NotFoundException("Warehouse", id));
	}

}
