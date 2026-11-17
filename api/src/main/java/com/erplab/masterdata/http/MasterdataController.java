package com.erplab.masterdata.http;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.common.http.PageResult;
import com.erplab.masterdata.application.command.MasterdataCommandHandler;
import com.erplab.masterdata.application.command.MasterdataCommands.*;
import com.erplab.masterdata.application.query.MasterdataQueries;
import com.erplab.masterdata.application.query.MasterdataQueries.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "masterdata")
@RestController
@PreAuthorize("hasAuthority('masterdata:read')")
class MasterdataController {

	private static final String WRITE = "hasAuthority('masterdata:write')";

	private final MasterdataCommandHandler commands;

	private final MasterdataQueries queries;

	MasterdataController(MasterdataCommandHandler commands, MasterdataQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	private static Map<String, Long> created(Long id) {
		return Map.of("id", id);
	}

	// --- units

	@GetMapping("/units")
	PageResult<UnitView> units(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.units(q, page, size);
	}

	@GetMapping("/units/{id}")
	UnitView unit(@PathVariable Long id) {
		return queries.unit(id);
	}

	@PostMapping("/units")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> createUnit(@Valid @RequestBody SaveUnit cmd) {
		return created(commands.createUnit(cmd));
	}

	@PutMapping("/units/{id}")
	@PreAuthorize(WRITE)
	UnitView updateUnit(@PathVariable Long id, @Valid @RequestBody SaveUnit cmd) {
		commands.updateUnit(id, cmd);
		return queries.unit(id);
	}

	@DeleteMapping("/units/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void deleteUnit(@PathVariable Long id) {
		commands.deleteUnit(id);
	}

	// --- categories

	@GetMapping("/categories")
	PageResult<CategoryView> categories(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.categories(q, page, size);
	}

	@GetMapping("/categories/{id}")
	CategoryView category(@PathVariable Long id) {
		return queries.category(id);
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> createCategory(@Valid @RequestBody SaveCategory cmd) {
		return created(commands.createCategory(cmd));
	}

	@PutMapping("/categories/{id}")
	@PreAuthorize(WRITE)
	CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody SaveCategory cmd) {
		commands.updateCategory(id, cmd);
		return queries.category(id);
	}

	@DeleteMapping("/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void deleteCategory(@PathVariable Long id) {
		commands.deleteCategory(id);
	}

	// --- products

	@GetMapping("/products")
	PageResult<ProductView> products(@RequestParam(required = false) String q,
			@RequestParam(required = false) String type, @RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.products(q, type, active, page, size);
	}

	@GetMapping("/products/{id}")
	ProductDetail product(@PathVariable Long id) {
		return queries.product(id);
	}

	@PostMapping("/products")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> createProduct(@Valid @RequestBody SaveProduct cmd) {
		return created(commands.createProduct(cmd));
	}

	@PutMapping("/products/{id}")
	@PreAuthorize(WRITE)
	ProductDetail updateProduct(@PathVariable Long id, @Valid @RequestBody SaveProduct cmd) {
		commands.updateProduct(id, cmd);
		return queries.product(id);
	}

	@DeleteMapping("/products/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void deleteProduct(@PathVariable Long id) {
		commands.deleteProduct(id);
	}

	// --- warehouses

	@GetMapping("/warehouses")
	PageResult<WarehouseView> warehouses(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.warehouses(q, page, size);
	}

	@GetMapping("/warehouses/{id}")
	WarehouseView warehouse(@PathVariable Long id) {
		return queries.warehouse(id);
	}

	@PostMapping("/warehouses")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> createWarehouse(@Valid @RequestBody SaveWarehouse cmd) {
		return created(commands.createWarehouse(cmd));
	}

	@PutMapping("/warehouses/{id}")
	@PreAuthorize(WRITE)
	WarehouseView updateWarehouse(@PathVariable Long id, @Valid @RequestBody SaveWarehouse cmd) {
		commands.updateWarehouse(id, cmd);
		return queries.warehouse(id);
	}

	@DeleteMapping("/warehouses/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void deleteWarehouse(@PathVariable Long id) {
		commands.deleteWarehouse(id);
	}

	// --- partners (customers and suppliers)

	@GetMapping("/partners")
	PageResult<PartnerView> partners(@RequestParam(required = false) String q,
			@RequestParam(required = false) String role, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return queries.partners(q, role, page, size);
	}

	@GetMapping("/partners/{id}")
	PartnerView partner(@PathVariable Long id) {
		return queries.partner(id);
	}

	@PostMapping("/partners")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> createPartner(@Valid @RequestBody SavePartner cmd) {
		return created(commands.createPartner(cmd));
	}

	@PutMapping("/partners/{id}")
	@PreAuthorize(WRITE)
	PartnerView updatePartner(@PathVariable Long id, @Valid @RequestBody SavePartner cmd) {
		commands.updatePartner(id, cmd);
		return queries.partner(id);
	}

	@DeleteMapping("/partners/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void deletePartner(@PathVariable Long id) {
		commands.deletePartner(id);
	}

}
