package com.erplab.masterdata.domain;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** A customer, a supplier, or both. */
@Getter
@Entity
@Table(name = "partner")
public class Partner extends BaseEntity {

	private String code;

	private String name;

	private boolean customer;

	private boolean supplier;

	private String taxCode;

	private String email;

	private String phone;

	private String address;

	private int paymentTermDays;

	private boolean active = true;

	protected Partner() {
	}

	public Partner(String code) {
		this.code = code.trim();
	}

	public void update(String name, boolean customer, boolean supplier, String taxCode, String email, String phone,
			String address, int paymentTermDays, boolean active) {
		if (!customer && !supplier) {
			throw new DomainException("partner_role_required", "A partner must be a customer, a supplier or both");
		}
		if (paymentTermDays < 0) {
			throw new DomainException("negative_value", "Payment term cannot be negative");
		}
		this.name = name;
		this.customer = customer;
		this.supplier = supplier;
		this.taxCode = taxCode;
		this.email = email;
		this.phone = phone;
		this.address = address;
		this.paymentTermDays = paymentTermDays;
		this.active = active;
	}

	public void delete() {
		active = false;
		markDeleted();
	}

}
