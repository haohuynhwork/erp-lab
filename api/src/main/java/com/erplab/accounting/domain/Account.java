package com.erplab.accounting.domain;

import com.erplab.common.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "account")
public class Account extends BaseEntity {

	/** Accounts used by automatic postings (seeded by Flyway). */
	public static final String CASH = "1110";
	public static final String RECEIVABLE = "1310";
	public static final String INVENTORY = "1410";
	public static final String PAYABLE = "2110";
	public static final String VAT_OUTPUT = "2210";
	public static final String OPENING_EQUITY = "3110";
	public static final String REVENUE = "4110";
	public static final String COGS = "5110";
	public static final String INVENTORY_ADJUSTMENT = "5210";

	private String code;

	private String name;

	@Enumerated(EnumType.STRING)
	private AccountType type;

	private boolean active = true;

	protected Account() {
	}

	public Account(String code, AccountType type) {
		this.code = code.trim();
		this.type = type;
	}

	public void update(String name, boolean active) {
		this.name = name;
		this.active = active;
	}

}
