package com.erplab.masterdata.domain;

import com.erplab.common.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** Unit of measure, e.g. pcs, kg, m. */
@Getter
@Entity
@Table(name = "unit")
public class Unit extends BaseEntity {

	private String code;

	private String name;

	protected Unit() {
	}

	public Unit(String code, String name) {
		update(code, name);
	}

	public void update(String code, String name) {
		this.code = code.trim();
		this.name = name;
	}

	public void delete() {
		markDeleted();
	}

}
