package com.erplab.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.erplab.common.domain.DomainException;

class StockItemTests {

	@Test
	void receiptsUpdateMovingAverageCost() {
		var item = new StockItem(1L, 1L);
		item.receive(new BigDecimal("10"), new BigDecimal("10"));
		item.receive(new BigDecimal("30"), new BigDecimal("14"));

		assertThat(item.getQuantity()).isEqualByComparingTo("40");
		assertThat(item.getAverageCost()).isEqualByComparingTo("13");
	}

	@Test
	void issueIsValuedAtAverageCostAndNeverGoesNegative() {
		var item = new StockItem(1L, 1L);
		item.receive(new BigDecimal("5"), new BigDecimal("2.5"));

		assertThat(item.issue(new BigDecimal("2"))).isEqualByComparingTo("5.00");
		assertThatThrownBy(() -> item.issue(new BigDecimal("4"))).isInstanceOf(DomainException.class)
			.hasMessageContaining("Only 3 in stock");
		assertThat(item.getQuantity()).isEqualByComparingTo("3");
	}

	@Test
	void countReturnsSignedValueChange() {
		var item = new StockItem(1L, 1L);
		item.receive(new BigDecimal("10"), new BigDecimal("4"));

		assertThat(item.countTo(new BigDecimal("8"))).isEqualByComparingTo("-8.00");
		assertThat(item.countTo(new BigDecimal("9"))).isEqualByComparingTo("4.00");
	}

}
