package com.erplab.sales.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.erplab.common.domain.DocumentStatus;
import com.erplab.common.domain.DomainException;

class SalesOrderTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

	private static SalesOrder confirmedOrder() {
		var order = new SalesOrder("SO-1");
		order.edit(1L, 1L, TODAY, null, null, new BigDecimal("10"), null,
				List.of(new SalesOrder.LineInput(5L, new BigDecimal("4"), new BigDecimal("25"))));
		ReflectionTestUtils.setField(order.getLines().get(0), "id", 200L);
		order.confirm();
		return order;
	}

	@Test
	void totalsIncludeVat() {
		var order = confirmedOrder();
		assertThat(order.getSubtotal()).isEqualByComparingTo("100.00");
		assertThat(order.getTaxAmount()).isEqualByComparingTo("10.00");
		assertThat(order.getTotal()).isEqualByComparingTo("110.00");
	}

	@Test
	void invoicesOnlyWhatWasDeliveredAndCompletesWhenAllDone() {
		var order = confirmedOrder();
		assertThatThrownBy(() -> order.invoice("INV-1", TODAY, 30)).isInstanceOf(DomainException.class);

		order.deliver(Map.of(200L, new BigDecimal("3")), TODAY);
		var invoice = order.invoice("INV-1", TODAY, 30);
		assertThat(invoice.getTotal()).isEqualByComparingTo("82.50");
		assertThat(invoice.getDueDate()).isEqualTo(TODAY.plusDays(30));
		assertThat(order.getStatus()).isEqualTo(DocumentStatus.CONFIRMED);

		order.deliver(Map.of(200L, BigDecimal.ONE), TODAY);
		order.invoice("INV-2", TODAY, 30);
		assertThat(order.getStatus()).isEqualTo(DocumentStatus.DONE);
	}

	@Test
	void paymentsCannotExceedBalance() {
		var order = confirmedOrder();
		order.deliver(Map.of(200L, new BigDecimal("4")), TODAY);
		var invoice = order.invoice("INV-1", TODAY, 0);

		invoice.registerPayment(new BigDecimal("100"), TODAY, "BANK", null);
		assertThat(invoice.getStatus()).isEqualTo(Invoice.Status.PARTIALLY_PAID);
		assertThatThrownBy(() -> invoice.registerPayment(new BigDecimal("11"), TODAY, "BANK", null))
			.isInstanceOf(DomainException.class);
		invoice.registerPayment(new BigDecimal("10"), TODAY, "BANK", null);
		assertThat(invoice.getStatus()).isEqualTo(Invoice.Status.PAID);
	}

	@Test
	void confirmReservesAndCancelReleasesOnlyConfirmedOrders() {
		var order = confirmedOrder();
		var cancelled = order.cancel();
		assertThat(cancelled).isPresent();
		assertThat(cancelled.get().lines().get(0).quantity()).isEqualByComparingTo("4");

		var quotation = new SalesOrder("SO-2");
		quotation.edit(1L, 1L, TODAY, null, TODAY.plusDays(14), BigDecimal.ZERO, null,
				List.of(new SalesOrder.LineInput(5L, BigDecimal.ONE, BigDecimal.TEN)));
		assertThat(quotation.cancel()).isEmpty();
	}

	@Test
	void creditNotesReduceTheBalanceAndCannotExceedIt() {
		var order = confirmedOrder();
		order.deliver(Map.of(200L, new BigDecimal("4")), TODAY);
		var invoice = order.invoice("INV-1", TODAY, 0);
		invoice.registerPayment(new BigDecimal("55"), TODAY, "BANK", null);

		var note = invoice.credit("CN-1", TODAY, "damaged", Map.of(0, BigDecimal.ONE));
		assertThat(note.getTotal()).isEqualByComparingTo("27.50");
		assertThat(invoice.balance()).isEqualByComparingTo("27.50");
		assertThat(invoice.getLines().get(0).creditedQuantity()).isEqualByComparingTo("1");

		assertThatThrownBy(() -> invoice.credit("CN-2", TODAY, "x", Map.of(0, new BigDecimal("2"))))
			.isInstanceOf(DomainException.class)
			.hasMessageContaining("exceeds the unpaid balance");
		invoice.registerPayment(new BigDecimal("27.50"), TODAY, "BANK", null);
		assertThat(invoice.getStatus()).isEqualTo(Invoice.Status.PAID);
	}

}
