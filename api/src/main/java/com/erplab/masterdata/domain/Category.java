package com.erplab.masterdata.domain;

import com.erplab.common.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "product_category")
public class Category extends BaseEntity {

	private String name;

	private String description;

	protected Category() {
	}

	public Category(String name, String description) {
		update(name, description);
	}

	public void update(String name, String description) {
		this.name = name.trim();
		this.description = description;
	}

	public void delete() {
		markDeleted();
	}

}
