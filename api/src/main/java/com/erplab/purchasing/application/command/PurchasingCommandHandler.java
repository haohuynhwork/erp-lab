package com.erplab.purchasing.application.command;

import java.time.LocalDate;
import java.util.LinkedHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;
import com.erplab.common.infrastructure.DocumentNumbers;
import com.erplab.masterdata.MasterdataApi;
import com.erplab.purchasing.application.command.PurchasingCommands.*;
import com.erplab.purchasing.domain.PurchaseOrder;
import com.erplab.purchasing.domain.PurchaseOrderRepository;

@Service
@Transactional
public class PurchasingCommandHandler {

	private final PurchaseOrderRepository orders;

	private final MasterdataApi masterdata;

	private final DocumentNumbers numbers;

	private final ApplicationEventPublisher events;

	PurchasingCommandHandler(PurchaseOrderRepository orders, MasterdataApi masterdata, DocumentNumbers numbers,
			ApplicationEventPublisher events) {
		this.orders = orders;
		this.masterdata = masterdata;
		this.numbers = numbers;
		this.events = events;
	}

	public Long create(SavePurchaseOrder cmd) {
		var order = new PurchaseOrder(numbers.next("PO", "purchase_order_number_seq"));
		edit(order, cmd);
		return orders.save(order).getId();
	}

	public void update(Long id, SavePurchaseOrder cmd) {
		edit(load(id), cmd);
	}

	private void edit(PurchaseOrder order, SavePurchaseOrder cmd) {
		if (!masterdata.partner(cmd.supplierId()).supplier()) {
			throw new DomainException("not_a_supplier", "The partner is not a supplier");
		}
		if (!masterdata.warehouse(cmd.warehouseId()).active()) {
			throw new DomainException("inactive_warehouse", "Warehouse is inactive");
		}
		var lines = cmd.lines().stream().map(l -> {
			var product = masterdata.product(l.productId());
			if (!product.active()) {
				throw new DomainException("inactive_product", product.sku() + " is inactive");
			}
			return new PurchaseOrder.LineInput(l.productId(), l.quantity(),
					l.unitPrice() != null ? l.unitPrice() : product.purchasePrice());
		}).toList();
		order.edit(cmd.supplierId(), cmd.warehouseId(), cmd.orderDate(), cmd.expectedDate(), cmd.note(), lines);
	}

	public void confirm(Long id) {
		load(id).confirm();
	}

	public void cancel(Long id) {
		load(id).cancel();
	}

	/** Publishes GoodsReceived; inventory and accounting book it in this transaction. */
	public void receive(Long id, ReceiveGoods cmd) {
		var quantities = new LinkedHashMap<Long, java.math.BigDecimal>();
		cmd.lines().forEach(l -> quantities.merge(l.lineId(), l.quantity(), java.math.BigDecimal::add));
		var event = load(id).receive(quantities, cmd.date() != null ? cmd.date() : LocalDate.now());
		events.publishEvent(event);
	}

	private PurchaseOrder load(Long id) {
		return orders.findById(id).filter(o -> !o.isDeleted())
			.orElseThrow(() -> new NotFoundException("Purchase order", id));
	}

}
