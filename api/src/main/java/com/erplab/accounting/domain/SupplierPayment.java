package com.erplab.accounting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.Quantities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "supplier_payment")
public class SupplierPayment extends BaseEntity {

	private String number;

	private Long supplierId;

	private LocalDate paymentDate;

	private BigDecimal amount;

	private String method;

	private String reference;

	protected SupplierPayment() {
	}

	public SupplierPayment(String number, Long supplierId, LocalDate paymentDate, BigDecimal amount, String method,
			String reference) {
		this.number = number;
		this.supplierId = supplierId;
		this.paymentDate = paymentDate;
		this.amount = Quantities.requirePositive(amount, "amount");
		this.method = method;
		this.reference = reference;
	}

}
