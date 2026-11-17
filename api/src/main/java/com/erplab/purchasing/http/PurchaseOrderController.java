package com.erplab.purchasing.http;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.common.http.PageResult;
import com.erplab.purchasing.application.command.PurchasingCommandHandler;
import com.erplab.purchasing.application.command.PurchasingCommands.CreateBill;
import com.erplab.purchasing.application.command.PurchasingCommands.ReceiveGoods;
import com.erplab.purchasing.application.command.PurchasingCommands.SavePurchaseOrder;
import com.erplab.purchasing.application.query.PurchasingQueries;
import com.erplab.purchasing.application.query.PurchasingQueries.PurchaseOrderDetail;
import com.erplab.purchasing.application.query.PurchasingQueries.PurchaseOrderView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "purchasing")
@RestController
@RequestMapping("/purchase-orders")
@PreAuthorize("hasAuthority('purchasing:read')")
class PurchaseOrderController {

	private static final String WRITE = "hasAuthority('purchasing:write')";

	private final PurchasingCommandHandler commands;

	private final PurchasingQueries queries;

	PurchaseOrderController(PurchasingCommandHandler commands, PurchasingQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping
	PageResult<PurchaseOrderView> list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
			@RequestParam(required = false) Long supplierId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.list(q, status, supplierId, page, size);
	}

	@GetMapping("/{id}")
	PurchaseOrderDetail get(@PathVariable Long id) {
		return queries.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> create(@Valid @RequestBody SavePurchaseOrder cmd) {
		return Map.of("id", commands.create(cmd));
	}

	@PutMapping("/{id}")
	@PreAuthorize(WRITE)
	PurchaseOrderDetail update(@PathVariable Long id, @Valid @RequestBody SavePurchaseOrder cmd) {
		commands.update(id, cmd);
		return queries.get(id);
	}

	@PostMapping("/{id}/confirm")
	@PreAuthorize(WRITE)
	PurchaseOrderDetail confirm(@PathVariable Long id) {
		commands.confirm(id);
		return queries.get(id);
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize(WRITE)
	PurchaseOrderDetail cancel(@PathVariable Long id) {
		commands.cancel(id);
		return queries.get(id);
	}

	/** Goods receipt: stock goes up, accounting books inventory against accounts payable. */
	@PostMapping("/{id}/receipts")
	@PreAuthorize("hasAuthority('purchasing:write') or hasAuthority('inventory:write')")
	PurchaseOrderDetail receive(@PathVariable Long id, @Valid @RequestBody ReceiveGoods cmd) {
		commands.receive(id, cmd);
		return queries.get(id);
	}

	@PostMapping("/{id}/bills")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('purchasing:write') or hasAuthority('accounting:write')")
	Map<String, Long> bill(@PathVariable Long id, @RequestBody(required = false) CreateBill cmd) {
		return Map.of("id", commands.bill(id, cmd != null ? cmd : new CreateBill(null, null)));
	}

}
