package com.erplab.sales.http;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.common.http.PageResult;
import com.erplab.sales.application.command.SalesCommandHandler;
import com.erplab.sales.application.command.SalesCommands.CreateInvoice;
import com.erplab.sales.application.command.SalesCommands.Deliver;
import com.erplab.sales.application.command.SalesCommands.SaveSalesOrder;
import com.erplab.sales.application.query.SalesQueries;
import com.erplab.sales.application.query.SalesQueries.OrderDetail;
import com.erplab.sales.application.query.SalesQueries.OrderView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "sales")
@RestController
@RequestMapping("/sales-orders")
@PreAuthorize("hasAuthority('sales:read')")
class SalesOrderController {

	private static final String WRITE = "hasAuthority('sales:write')";

	private final SalesCommandHandler commands;

	private final SalesQueries queries;

	SalesOrderController(SalesCommandHandler commands, SalesQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping
	PageResult<OrderView> list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
			@RequestParam(required = false) Long customerId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.orders(q, status, customerId, page, size);
	}

	@GetMapping("/{id}")
	OrderDetail get(@PathVariable Long id) {
		return queries.order(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> create(@Valid @RequestBody SaveSalesOrder cmd) {
		return Map.of("id", commands.create(cmd));
	}

	@PutMapping("/{id}")
	@PreAuthorize(WRITE)
	OrderDetail update(@PathVariable Long id, @Valid @RequestBody SaveSalesOrder cmd) {
		commands.update(id, cmd);
		return queries.order(id);
	}

	@PostMapping("/{id}/confirm")
	@PreAuthorize(WRITE)
	OrderDetail confirm(@PathVariable Long id) {
		commands.confirm(id);
		return queries.order(id);
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize(WRITE)
	OrderDetail cancel(@PathVariable Long id) {
		commands.cancel(id);
		return queries.order(id);
	}

	/** Delivery: stock goes down, accounting books cost of goods sold. */
	@PostMapping("/{id}/deliveries")
	@PreAuthorize("hasAuthority('sales:write') or hasAuthority('inventory:write')")
	OrderDetail deliver(@PathVariable Long id, @Valid @RequestBody Deliver cmd) {
		commands.deliver(id, cmd);
		return queries.order(id);
	}

	@PostMapping("/{id}/invoices")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> invoice(@PathVariable Long id, @RequestBody(required = false) CreateInvoice cmd) {
		return Map.of("id", commands.invoice(id, cmd != null ? cmd : new CreateInvoice(null)));
	}

}
