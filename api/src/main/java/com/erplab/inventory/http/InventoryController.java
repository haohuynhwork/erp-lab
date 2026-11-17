package com.erplab.inventory.http;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.common.http.PageResult;
import com.erplab.inventory.application.command.InventoryCommandHandler;
import com.erplab.inventory.application.command.InventoryCommands.*;
import com.erplab.inventory.application.query.InventoryQueries;
import com.erplab.inventory.application.query.InventoryQueries.MovementView;
import com.erplab.inventory.application.query.InventoryQueries.StockView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "inventory")
@RestController
@RequestMapping("/inventory")
@PreAuthorize("hasAuthority('inventory:read')")
class InventoryController {

	private static final String WRITE = "hasAuthority('inventory:write')";

	private final InventoryCommandHandler commands;

	private final InventoryQueries queries;

	InventoryController(InventoryCommandHandler commands, InventoryQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping("/stock")
	PageResult<StockView> stock(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long warehouseId, @RequestParam(required = false) Long productId,
			@RequestParam(defaultValue = "false") boolean lowOnly, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.stock(q, warehouseId, productId, lowOnly, page, size);
	}

	@GetMapping("/movements")
	PageResult<MovementView> movements(@RequestParam(required = false) Long productId,
			@RequestParam(required = false) Long warehouseId, @RequestParam(required = false) String type,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.movements(productId, warehouseId, type, page, size);
	}

	@PostMapping("/receipts")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void receive(@Valid @RequestBody ReceiveStock cmd) {
		commands.handle(cmd);
	}

	@PostMapping("/issues")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void issue(@Valid @RequestBody IssueStock cmd) {
		commands.handle(cmd);
	}

	@PostMapping("/transfers")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void transfer(@Valid @RequestBody TransferStock cmd) {
		commands.handle(cmd);
	}

	@PostMapping("/counts")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void count(@Valid @RequestBody CountStock cmd) {
		commands.handle(cmd);
	}

}
