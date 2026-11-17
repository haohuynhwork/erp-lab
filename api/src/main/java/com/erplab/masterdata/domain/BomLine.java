package com.erplab.masterdata.domain;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

/** One component of a bill of materials: {@code quantity} of {@code componentId} per 1 unit of the product. */
@Embeddable
public record BomLine(Long componentId, BigDecimal quantity) {
}
