package com.erplab.sales.http;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.common.http.PageResult;
import com.erplab.sales.application.command.SalesCommandHandler;
import com.erplab.sales.application.command.SalesCommands.CreateCreditNote;
import com.erplab.sales.application.command.SalesCommands.RecordPayment;
import com.erplab.sales.application.query.SalesQueries;
import com.erplab.sales.application.query.SalesQueries.CreditNoteView;
import com.erplab.sales.application.query.SalesQueries.InvoiceDetail;
import com.erplab.sales.application.query.SalesQueries.InvoiceView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "sales")
@RestController
@RequestMapping("/invoices")
@PreAuthorize("hasAuthority('sales:read') or hasAuthority('accounting:read')")
class InvoiceController {

	private final SalesCommandHandler commands;

	private final SalesQueries queries;

	InvoiceController(SalesCommandHandler commands, SalesQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping
	PageResult<InvoiceView> list(@RequestParam(required = false) String q,
			@RequestParam(required = false) String status, @RequestParam(required = false) Long customerId,
			@RequestParam(defaultValue = "false") boolean overdueOnly, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.invoices(q, status, customerId, overdueOnly, page, size);
	}

	@GetMapping("/{id}")
	InvoiceDetail get(@PathVariable Long id) {
		return queries.invoice(id);
	}

	/** Customer payment: accounting books cash against accounts receivable. */
	@PostMapping("/{id}/payments")
	@PreAuthorize("hasAuthority('sales:write') or hasAuthority('accounting:write')")
	InvoiceDetail pay(@PathVariable Long id, @Valid @RequestBody RecordPayment cmd) {
		commands.recordPayment(id, cmd);
		return queries.invoice(id);
	}

	@PostMapping("/{id}/credit-notes")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('sales:write') or hasAuthority('accounting:write')")
	Map<String, Long> credit(@PathVariable Long id, @Valid @RequestBody CreateCreditNote cmd) {
		return Map.of("id", commands.credit(id, cmd));
	}

	@GetMapping("/credit-notes")
	PageResult<CreditNoteView> creditNotes(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long customerId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.creditNotes(q, customerId, page, size);
	}

}
