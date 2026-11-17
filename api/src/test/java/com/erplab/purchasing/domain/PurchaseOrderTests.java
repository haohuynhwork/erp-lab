package com.erplab.purchasing.domain;

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

class PurchaseOrderTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

	private static PurchaseOrder confirmedOrder() {
		var order = new PurchaseOrder("PO-1");
		order.edit(1L, 1L, TODAY, null, null, List.of(new PurchaseOrder.LineInput(5L, new BigDecimal("10"),
				new BigDecimal("2"))));
		ReflectionTestUtils.setField(order.getLines().get(0), "id", 100L);
		order.confirm();
		return order;
	}

	@Test
	void totalIsSumOfLines() {
		assertThat(confirmedOrder().getTotal()).isEqualByComparingTo("20.00");
	}

	@Test
	void partialReceiptsCompleteTheOrder() {
		var order = confirmedOrder();

		var first = order.receive(Map.of(100L, new BigDecimal("4")), TODAY);
		assertThat(first.amount()).isEqualByComparingTo("8.00");
		assertThat(order.getStatus()).isEqualTo(DocumentStatus.CONFIRMED);

		order.receive(Map.of(100L, new BigDecimal("6")), TODAY);
		assertThat(order.getStatus()).isEqualTo(DocumentStatus.DONE);
	}

	@Test
	void cannotReceiveMoreThanOrderedOrCancelAfterReceipt() {
		var order = confirmedOrder();
		assertThatThrownBy(() -> order.receive(Map.of(100L, new BigDecimal("11")), TODAY))
			.isInstanceOf(DomainException.class);

		order.receive(Map.of(100L, BigDecimal.ONE), TODAY);
		assertThatThrownBy(order::cancel).isInstanceOf(DomainException.class);
	}

	@Test
	void onlyDraftsCanBeEdited() {
		var order = confirmedOrder();
		assertThatThrownBy(() -> order.edit(1L, 1L, TODAY, null, null, List.of()))
			.isInstanceOf(DomainException.class)
			.hasMessageContaining("CONFIRMED");
	}

}
