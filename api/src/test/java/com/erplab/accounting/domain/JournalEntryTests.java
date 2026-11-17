package com.erplab.accounting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.erplab.common.domain.DomainException;

class JournalEntryTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

	private static JournalEntry entry(List<JournalLine> lines) {
		return new JournalEntry("JE-1", TODAY, "test", "MANUAL", null, null, lines);
	}

	@Test
	void debitsMustEqualCredits() {
		assertThatThrownBy(() -> entry(List.of(JournalLine.debit(1L, null, new BigDecimal("10")),
				JournalLine.credit(2L, null, new BigDecimal("9")))))
			.isInstanceOf(DomainException.class)
			.hasMessageContaining("must equal");
	}

	@Test
	void eachLineIsEitherDebitOrCredit() {
		var both = new JournalLine(1L, null, BigDecimal.ONE, BigDecimal.ONE, null);
		assertThatThrownBy(() -> entry(List.of(both, JournalLine.credit(2L, null, BigDecimal.ZERO))))
			.isInstanceOf(DomainException.class);
	}

	@Test
	void reversalSwapsSidesOnce() {
		var original = entry(List.of(JournalLine.debit(1L, 7L, new BigDecimal("10")),
				JournalLine.credit(2L, null, new BigDecimal("10"))));

		var reversal = original.reverse("JE-2", TODAY);

		assertThat(reversal.getLines().get(0).credit()).isEqualByComparingTo("10");
		assertThat(reversal.getLines().get(0).partnerId()).isEqualTo(7L);
		assertThat(original.isReversed()).isTrue();
		assertThatThrownBy(() -> original.reverse("JE-3", TODAY)).isInstanceOf(DomainException.class);
	}

}
