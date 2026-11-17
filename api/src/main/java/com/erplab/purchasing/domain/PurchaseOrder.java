package com.erplab.purchasing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DocumentStatus;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.Quantities;
import com.erplab.purchasing.GoodsReceived;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;

/** Draft (editable) → Confirmed (sent to supplier) → Done (all received); Cancelled before anything arrives. */
@Getter
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseEntity {

	public record LineInput(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
	}

	private String number;

	private Long supplierId;

	private Long warehouseId;

	private LocalDate orderDate;

	private LocalDate expectedDate;

	@Enumerated(EnumType.STRING)
	private DocumentStatus status = DocumentStatus.DRAFT;

	private String note;

	private BigDecimal total = BigDecimal.ZERO;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "purchase_order_id", nullable = false)
	@OrderBy("id")
	private List<PurchaseOrderLine> lines = new ArrayList<>();

	protected PurchaseOrder() {
	}

	public PurchaseOrder(String number) {
		this.number = number;
	}

	public void edit(Long supplierId, Long warehouseId, LocalDate orderDate, LocalDate expectedDate, String note,
			List<LineInput> newLines) {
		status.requireIs(DocumentStatus.DRAFT, "edit");
		if (expectedDate != null && expectedDate.isBefore(orderDate)) {
			throw new DomainException("invalid_date", "Expected date is before the order date");
		}
		this.supplierId = supplierId;
		this.warehouseId = warehouseId;
		this.orderDate = orderDate;
		this.expectedDate = expectedDate;
		this.note = note;
		lines.clear();
		newLines.forEach(l -> lines.add(new PurchaseOrderLine(l.productId(), l.quantity(), l.unitPrice())));
		total = lines.stream().map(PurchaseOrderLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public void confirm() {
		status.requireIs(DocumentStatus.DRAFT, "confirm");
		if (lines.isEmpty()) {
			throw new DomainException("no_lines", "Add at least one line before confirming");
		}
		status = DocumentStatus.CONFIRMED;
	}

	public void cancel() {
		if (!status.isOpen() || lines.stream().anyMatch(l -> l.getReceivedQuantity().signum() > 0)) {
			throw new DomainException("cannot_cancel", "Only orders with nothing received can be cancelled");
		}
		status = DocumentStatus.CANCELLED;
	}

	/** Receives part or all of the remaining quantities. @param quantities line id → quantity received now */
	public GoodsReceived receive(Map<Long, BigDecimal> quantities, LocalDate date) {
		status.requireIs(DocumentStatus.CONFIRMED, "receive goods for");
		var received = new ArrayList<GoodsReceived.Line>();
		var amount = BigDecimal.ZERO;
		for (var entry : quantities.entrySet()) {
			var qty = entry.getValue();
			if (qty == null || qty.signum() == 0) {
				continue;
			}
			Quantities.requirePositive(qty, "quantity");
			var line = lines.stream().filter(l -> l.getId().equals(entry.getKey())).findFirst()
				.orElseThrow(() -> new DomainException("unknown_line", "Line " + entry.getKey() + " is not on this order"));
			if (qty.compareTo(line.remaining()) > 0) {
				throw new DomainException("over_receipt", "Cannot receive more than ordered on line " + line.getId());
			}
			line.receive(qty);
			received.add(new GoodsReceived.Line(line.getProductId(), qty, line.getUnitPrice()));
			amount = amount.add(Quantities.money(qty.multiply(line.getUnitPrice())));
		}
		if (received.isEmpty()) {
			throw new DomainException("nothing_received", "Enter a quantity for at least one line");
		}
		if (lines.stream().allMatch(l -> l.remaining().signum() == 0)) {
			status = DocumentStatus.DONE;
		}
		return new GoodsReceived(getId(), number, supplierId, warehouseId, date, received, amount);
	}

	/** Bills everything received but not yet billed. */
	public SupplierBill bill(String billNumber, String supplierReference, LocalDate date, int paymentTermDays) {
		if (status != DocumentStatus.CONFIRMED && status != DocumentStatus.DONE) {
			throw new DomainException("invalid_status", "Only confirmed orders can be billed");
		}
		var billLines = new ArrayList<SupplierBill.Line>();
		for (var line : lines) {
			var qty = line.toBill();
			if (qty.signum() > 0) {
				line.billed(qty);
				billLines.add(new SupplierBill.Line(line.getProductId(), qty, line.getUnitPrice()));
			}
		}
		if (billLines.isEmpty()) {
			throw new DomainException("nothing_to_bill", "Nothing received is waiting to be billed");
		}
		return new SupplierBill(billNumber, supplierReference, getId(), supplierId, date, date.plusDays(paymentTermDays),
				billLines);
	}

}
