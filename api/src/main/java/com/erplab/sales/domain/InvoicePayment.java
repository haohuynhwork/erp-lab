package com.erplab.sales.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "invoice_payment")
public class InvoicePayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDate paymentDate;

	private BigDecimal amount;

	private String method;

	private String reference;

	protected InvoicePayment() {
	}

	InvoicePayment(LocalDate paymentDate, BigDecimal amount, String method, String reference) {
		this.paymentDate = paymentDate;
		this.amount = amount;
		this.method = method;
		this.reference = reference;
	}

}
