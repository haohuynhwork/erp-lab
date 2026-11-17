package com.erplab.inventory.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.common.domain.DomainException;
import com.erplab.common.security.CurrentUser;
import com.erplab.inventory.StockValueChanged;
import com.erplab.inventory.StockValueChanged.Reason;
import com.erplab.inventory.application.command.InventoryCommands.*;
import com.erplab.inventory.domain.*;
import com.erplab.masterdata.MasterdataApi;
import com.erplab.purchasing.GoodsReceived;
import com.erplab.sales.DeliveryShipped;

/**
 * Changes stock. Purchasing receipts and sales deliveries arrive as events, handled synchronously in the
 * publisher's transaction: a delivery without enough stock fails as a whole.
 */
@Service
@Transactional
public class InventoryCommandHandler {

	private static final String MANUAL = "MANUAL";

	private final StockItemRepository items;

	private final StockMovementRepository movements;

	private final MasterdataApi masterdata;

	private final ApplicationEventPublisher events;

	InventoryCommandHandler(StockItemRepository items, StockMovementRepository movements, MasterdataApi masterdata,
			ApplicationEventPublisher events) {
		this.items = items;
		this.movements = movements;
		this.masterdata = masterdata;
		this.events = events;
	}

	public void handle(ReceiveStock cmd) {
		var item = item(cmd.productId(), cmd.warehouseId());
		var value = item.receive(cmd.quantity(), cmd.unitCost());
		record(item, MovementType.RECEIPT, cmd.quantity(), value, MANUAL, null, null, cmd.note());
		valueChanged(Reason.MANUAL_RECEIPT, value, MANUAL, null, null);
	}

	public void handle(IssueStock cmd) {
		var item = item(cmd.productId(), cmd.warehouseId());
		var value = item.issue(cmd.quantity());
		record(item, MovementType.ISSUE, cmd.quantity().negate(), value.negate(), MANUAL, null, null, cmd.note());
		valueChanged(Reason.MANUAL_ISSUE, value.negate(), MANUAL, null, null);
	}

	public void handle(TransferStock cmd) {
		if (cmd.fromWarehouseId().equals(cmd.toWarehouseId())) {
			throw new DomainException("same_warehouse", "Source and destination warehouse must differ");
		}
		var from = item(cmd.productId(), cmd.fromWarehouseId());
		var to = item(cmd.productId(), cmd.toWarehouseId());
		var cost = from.getAverageCost();
		var value = from.issue(cmd.quantity());
		to.receive(cmd.quantity(), cost);
		record(from, MovementType.TRANSFER_OUT, cmd.quantity().negate(), value.negate(), "TRANSFER", null, null,
				cmd.note());
		record(to, MovementType.TRANSFER_IN, cmd.quantity(), value, "TRANSFER", null, null, cmd.note());
	}

	public void handle(CountStock cmd) {
		var item = item(cmd.productId(), cmd.warehouseId());
		var before = item.getQuantity();
		var value = item.countTo(cmd.countedQuantity());
		var delta = cmd.countedQuantity().subtract(before);
		if (delta.signum() == 0) {
			return;
		}
		record(item, MovementType.ADJUSTMENT, delta, value, "COUNT", null, null, cmd.note());
		valueChanged(Reason.ADJUSTMENT, value, "COUNT", null, null);
	}

	@EventListener
	public void on(GoodsReceived e) {
		for (var line : e.lines()) {
			if (!masterdata.product(line.productId()).stocked()) {
				continue;
			}
			var item = item(line.productId(), e.warehouseId());
			var value = item.receive(line.quantity(), line.unitPrice());
			record(item, MovementType.RECEIPT, line.quantity(), value, "PURCHASE_ORDER", e.purchaseOrderId(),
					e.purchaseOrderNumber(), null);
		}
	}

	@EventListener
	public void on(DeliveryShipped e) {
		var total = BigDecimal.ZERO;
		for (var line : e.lines()) {
			if (!masterdata.product(line.productId()).stocked()) {
				continue;
			}
			var item = item(line.productId(), e.warehouseId());
			var value = item.issue(line.quantity());
			record(item, MovementType.ISSUE, line.quantity().negate(), value.negate(), "SALES_ORDER", e.salesOrderId(),
					e.salesOrderNumber(), null);
			total = total.add(value);
		}
		if (total.signum() != 0) {
			valueChanged(Reason.SALES_ISSUE, total.negate(), "SALES_ORDER", e.salesOrderId(), e.salesOrderNumber());
		}
	}

	private StockItem item(Long productId, Long warehouseId) {
		var product = masterdata.product(productId);
		if (!product.stocked()) {
			throw new DomainException("not_stocked", product.sku() + " is a service and has no stock");
		}
		if (!masterdata.warehouse(warehouseId).active()) {
			throw new DomainException("inactive_warehouse", "Warehouse is inactive");
		}
		return items.findByProductIdAndWarehouseId(productId, warehouseId)
			.orElseGet(() -> items.save(new StockItem(productId, warehouseId)));
	}

	private void record(StockItem item, MovementType type, BigDecimal qty, BigDecimal value, String refType, Long refId,
			String refNumber, String note) {
		movements.save(new StockMovement(item, type, qty, value, refType, refId, refNumber, note, CurrentUser.username()));
	}

	private void valueChanged(Reason reason, BigDecimal amount, String refType, Long refId, String refNumber) {
		events.publishEvent(new StockValueChanged(reason, amount, refType, refId, refNumber, LocalDate.now()));
	}

}
