package com.erplab.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Quantities {

	private Quantities() {
	}

	public static BigDecimal requirePositive(BigDecimal value, String field) {
		if (value == null || value.signum() <= 0) {
			throw new DomainException("invalid_" + field, field + " must be greater than 0");
		}
		return value;
	}

	public static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
