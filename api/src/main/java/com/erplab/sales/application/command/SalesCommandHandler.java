package com.erplab.sales.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;
import com.erplab.common.infrastructure.DocumentNumbers;
import com.erplab.masterdata.MasterdataApi;
import com.erplab.sales.application.command.SalesCommands.*;
import com.erplab.sales.domain.Invoice;
import com.erplab.sales.domain.InvoiceRepository;
import com.erplab.sales.domain.SalesOrder;
import com.erplab.sales.domain.SalesOrderRepository;

@Service
@Transactional
public class SalesCommandHandler {

	private final SalesOrderRepository orders;

	private final InvoiceRepository invoices;

	private final MasterdataApi masterdata;

	private final DocumentNumbers numbers;

	private final ApplicationEventPublisher events;

	SalesCommandHandler(SalesOrderRepository orders, InvoiceRepository invoices, MasterdataApi masterdata,
			DocumentNumbers numbers, ApplicationEventPublisher events) {
		this.orders = orders;
		this.invoices = invoices;
		this.masterdata = masterdata;
		this.numbers = numbers;
		this.events = events;
	}

	public Long create(SaveSalesOrder cmd) {
		var order = new SalesOrder(numbers.next("SO", "sales_order_number_seq"));
		edit(order, cmd);
		return orders.save(order).getId();
	}

	public void update(Long id, SaveSalesOrder cmd) {
		edit(load(id), cmd);
	}

	private void edit(SalesOrder order, SaveSalesOrder cmd) {
		if (!masterdata.partner(cmd.customerId()).customer()) {
			throw new DomainException("not_a_customer", "The partner is not a customer");
		}
		if (!masterdata.warehouse(cmd.warehouseId()).active()) {
			throw new DomainException("inactive_warehouse", "Warehouse is inactive");
		}
		var lines = cmd.lines().stream().map(l -> {
			var product = masterdata.product(l.productId());
			if (!product.active()) {
				throw new DomainException("inactive_product", product.sku() + " is inactive");
			}
			return new SalesOrder.LineInput(l.productId(), l.quantity(),
					l.unitPrice() != null ? l.unitPrice() : product.salePrice());
		}).toList();
		order.edit(cmd.customerId(), cmd.warehouseId(), cmd.orderDate(), cmd.deliveryDate(), cmd.taxRate(), cmd.note(),
				lines);
	}

	public void confirm(Long id) {
		load(id).confirm();
	}

	public void cancel(Long id) {
		load(id).cancel();
	}

	/** Publishes DeliveryShipped; inventory issues the stock in this transaction (fails if not enough). */
	public void deliver(Long id, Deliver cmd) {
		var quantities = new LinkedHashMap<Long, BigDecimal>();
		cmd.lines().forEach(l -> quantities.merge(l.lineId(), l.quantity(), BigDecimal::add));
		events.publishEvent(load(id).deliver(quantities, cmd.date() != null ? cmd.date() : LocalDate.now()));
	}

	/** @return the new invoice id */
	public Long invoice(Long orderId, CreateInvoice cmd) {
		var order = load(orderId);
		var terms = masterdata.partner(order.getCustomerId()).paymentTermDays();
		var date = cmd.date() != null ? cmd.date() : LocalDate.now();
		var invoice = invoices.save(order.invoice(numbers.next("INV", "invoice_number_seq"), date, terms));
		events.publishEvent(invoice.issued());
		return invoice.getId();
	}

	public void recordPayment(Long invoiceId, RecordPayment cmd) {
		var invoice = invoices.findById(invoiceId).orElseThrow(() -> new NotFoundException("Invoice", invoiceId));
		events.publishEvent(invoice.registerPayment(cmd.amount(), cmd.date() != null ? cmd.date() : LocalDate.now(),
				cmd.method(), cmd.reference()));
	}

	private SalesOrder load(Long id) {
		return orders.findById(id).filter(o -> !o.isDeleted()).orElseThrow(() -> new NotFoundException("Sales order", id));
	}


}
