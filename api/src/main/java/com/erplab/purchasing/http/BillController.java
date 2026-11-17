package com.erplab.purchasing.http;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.common.http.PageResult;
import com.erplab.purchasing.application.command.PurchasingCommandHandler;
import com.erplab.purchasing.application.command.PurchasingCommands.PayBill;
import com.erplab.purchasing.application.query.PurchasingQueries;
import com.erplab.purchasing.application.query.PurchasingQueries.BillDetail;
import com.erplab.purchasing.application.query.PurchasingQueries.BillView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "purchasing")
@RestController
@RequestMapping("/bills")
@PreAuthorize("hasAuthority('purchasing:read') or hasAuthority('accounting:read')")
class BillController {

	private final PurchasingCommandHandler commands;

	private final PurchasingQueries queries;

	BillController(PurchasingCommandHandler commands, PurchasingQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping
	PageResult<BillView> list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
			@RequestParam(required = false) Long supplierId, @RequestParam(defaultValue = "false") boolean overdueOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.bills(q, status, supplierId, overdueOnly, page, size);
	}

	@GetMapping("/{id}")
	BillDetail get(@PathVariable Long id) {
		return queries.bill(id);
	}

	/** Supplier payment: accounting books payables against cash. */
	@PostMapping("/{id}/payments")
	@PreAuthorize("hasAuthority('accounting:write')")
	BillDetail pay(@PathVariable Long id, @Valid @RequestBody PayBill cmd) {
		commands.pay(id, cmd);
		return queries.bill(id);
	}

}
