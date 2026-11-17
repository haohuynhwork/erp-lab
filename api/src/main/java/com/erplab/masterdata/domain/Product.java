package com.erplab.masterdata.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.Quantities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "product")
public class Product extends BaseEntity {

	private String sku;

	private String name;

	@Enumerated(EnumType.STRING)
	private ProductType type;

	private Long unitId;

	private Long categoryId;

	private BigDecimal purchasePrice = BigDecimal.ZERO;

	private BigDecimal salePrice = BigDecimal.ZERO;

	/** Low-stock alert threshold per warehouse. */
	private BigDecimal minStock = BigDecimal.ZERO;

	private boolean active = true;

	@ElementCollection
	@CollectionTable(name = "product_bom_line", joinColumns = @JoinColumn(name = "product_id"))
	@OrderColumn(name = "position")
	private List<BomLine> bom = new ArrayList<>();

	protected Product() {
	}

	public Product(String sku, ProductType type) {
		this.sku = sku.trim();
		this.type = type;
	}

	public void update(String name, Long unitId, Long categoryId, BigDecimal purchasePrice, BigDecimal salePrice,
			BigDecimal minStock, boolean active) {
		if (purchasePrice.signum() < 0 || salePrice.signum() < 0 || minStock.signum() < 0) {
			throw new DomainException("negative_value", "Prices and minimum stock cannot be negative");
		}
		this.name = name;
		this.unitId = unitId;
		this.categoryId = categoryId;
		this.purchasePrice = purchasePrice;
		this.salePrice = salePrice;
		this.minStock = minStock;
		this.active = active;
	}

	/** Only manufactured products have a BOM; a product cannot contain itself and each component appears once. */
	public void replaceBom(List<BomLine> lines) {
		if (!lines.isEmpty() && !type.manufactured()) {
			throw new DomainException("bom_not_allowed", "Only finished and semi-finished products have a BOM");
		}
		var seen = new HashSet<Long>();
		for (var line : lines) {
			Quantities.requirePositive(line.quantity(), "quantity");
			if (line.componentId().equals(getId())) {
				throw new DomainException("bom_self_reference", "A product cannot be a component of itself");
			}
			if (!seen.add(line.componentId())) {
				throw new DomainException("bom_duplicate", "Component " + line.componentId() + " is listed twice");
			}
		}
		bom.clear();
		bom.addAll(lines);
	}

	public void delete() {
		active = false;
		markDeleted();
	}

}
