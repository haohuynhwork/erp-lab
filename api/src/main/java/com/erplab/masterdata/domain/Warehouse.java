package com.erplab.masterdata.domain;

import com.erplab.common.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "warehouse")
public class Warehouse extends BaseEntity {

	private String code;

	private String name;

	private String address;

	private boolean active = true;

	protected Warehouse() {
	}

	public Warehouse(String code) {
		this.code = code.trim();
	}

	public void update(String name, String address, boolean active) {
		this.name = name;
		this.address = address;
		this.active = active;
	}

	public void delete() {
		active = false;
		markDeleted();
	}

}
