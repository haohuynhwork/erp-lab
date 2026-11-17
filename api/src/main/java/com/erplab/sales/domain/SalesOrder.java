package com.erplab.sales.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DocumentStatus;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.Quantities;
import com.erplab.sales.DeliveryShipped;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;

/** Draft → Confirmed → Done (fully delivered and invoiced); Cancelled before anything is delivered. */
@Getter
@Entity
@Table(name = "sales_order")
public class SalesOrder extends BaseEntity {

	public record LineInput(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
	}

	private String number;

	private Long customerId;

	private Long warehouseId;

	private LocalDate orderDate;

	private LocalDate deliveryDate;

	@Enumerated(EnumType.STRING)
	private DocumentStatus status = DocumentStatus.DRAFT;

	private String note;

	/** VAT in percent, e.g. 10. */
	private BigDecimal taxRate = BigDecimal.ZERO;

	private BigDecimal subtotal = BigDecimal.ZERO;

	private BigDecimal taxAmount = BigDecimal.ZERO;

	private BigDecimal total = BigDecimal.ZERO;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "sales_order_id", nullable = false)
	@OrderBy("id")
	private List<SalesOrderLine> lines = new ArrayList<>();

	protected SalesOrder() {
	}

	public SalesOrder(String number) {
		this.number = number;
	}

	public void edit(Long customerId, Long warehouseId, LocalDate orderDate, LocalDate deliveryDate, BigDecimal taxRate,
			String note, List<LineInput> newLines) {
		status.requireIs(DocumentStatus.DRAFT, "edit");
		if (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new DomainException("invalid_tax_rate", "Tax rate must be between 0 and 100");
		}
		this.customerId = customerId;
		this.warehouseId = warehouseId;
		this.orderDate = orderDate;
		this.deliveryDate = deliveryDate;
		this.taxRate = taxRate;
		this.note = note;
		lines.clear();
		newLines.forEach(l -> lines.add(new SalesOrderLine(l.productId(), l.quantity(), l.unitPrice())));
		subtotal = lines.stream().map(SalesOrderLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		taxAmount = Invoice.tax(subtotal, taxRate);
		total = subtotal.add(taxAmount);
	}

	public void confirm() {
		status.requireIs(DocumentStatus.DRAFT, "confirm");
		if (lines.isEmpty()) {
			throw new DomainException("no_lines", "Add at least one line before confirming");
		}
		status = DocumentStatus.CONFIRMED;
	}

	public void cancel() {
		if (!status.isOpen() || lines.stream().anyMatch(l -> l.getDeliveredQuantity().signum() > 0)) {
			throw new DomainException("cannot_cancel", "Only orders with nothing delivered can be cancelled");
		}
		status = DocumentStatus.CANCELLED;
	}

	/** @param quantities line id → quantity shipped now */
	public DeliveryShipped deliver(Map<Long, BigDecimal> quantities, LocalDate date) {
		status.requireIs(DocumentStatus.CONFIRMED, "deliver");
		var shipped = new ArrayList<DeliveryShipped.Line>();
		for (var entry : quantities.entrySet()) {
			var qty = entry.getValue();
			if (qty == null || qty.signum() == 0) {
				continue;
			}
			Quantities.requirePositive(qty, "quantity");
			var line = line(entry.getKey());
			if (qty.compareTo(line.toDeliver()) > 0) {
				throw new DomainException("over_delivery", "Cannot deliver more than ordered on line " + line.getId());
			}
			line.deliver(qty);
			shipped.add(new DeliveryShipped.Line(line.getProductId(), qty));
		}
		if (shipped.isEmpty()) {
			throw new DomainException("nothing_delivered", "Enter a quantity for at least one line");
		}
		completeIfDone();
		return new DeliveryShipped(getId(), number, customerId, warehouseId, date, shipped);
	}

	/** Invoices everything delivered but not yet invoiced. */
	public Invoice invoice(String invoiceNumber, LocalDate date, int paymentTermDays) {
		if (status != DocumentStatus.CONFIRMED && status != DocumentStatus.DONE) {
			throw new DomainException("invalid_status", "Only confirmed orders can be invoiced");
		}
		var invoiceLines = new ArrayList<Invoice.Line>();
		for (var line : lines) {
			var qty = line.toInvoice();
			if (qty.signum() > 0) {
				line.invoiced(qty);
				invoiceLines.add(new Invoice.Line(line.getProductId(), qty, line.getUnitPrice()));
			}
		}
		if (invoiceLines.isEmpty()) {
			throw new DomainException("nothing_to_invoice", "Nothing delivered is waiting to be invoiced");
		}
		completeIfDone();
		return new Invoice(invoiceNumber, getId(), customerId, date, date.plusDays(paymentTermDays), taxRate,
				invoiceLines);
	}

	private void completeIfDone() {
		if (lines.stream().allMatch(l -> l.toDeliver().signum() == 0 && l.toInvoice().signum() == 0)) {
			status = DocumentStatus.DONE;
		}
	}

	private SalesOrderLine line(Long id) {
		return lines.stream().filter(l -> l.getId().equals(id)).findFirst()
			.orElseThrow(() -> new DomainException("unknown_line", "Line " + id + " is not on this order"));
	}

}
