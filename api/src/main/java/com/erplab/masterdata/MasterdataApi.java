package com.erplab.masterdata;

import java.math.BigDecimal;

/** What other contexts may ask masterdata. Throws NotFoundException for unknown or deleted ids. */
public interface MasterdataApi {

	record ProductRef(Long id, String sku, String name, String type, String unitCode, BigDecimal purchasePrice,
			BigDecimal salePrice, boolean stocked, boolean active) {
	}

	record PartnerRef(Long id, String code, String name, boolean customer, boolean supplier, int paymentTermDays) {
	}

	record WarehouseRef(Long id, String code, String name, boolean active) {
	}

	ProductRef product(Long id);

	PartnerRef partner(Long id);

	WarehouseRef warehouse(Long id);

}
