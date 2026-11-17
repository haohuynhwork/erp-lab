package com.erplab.masterdata.application.command;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;
import com.erplab.masterdata.application.command.MasterdataCommands.*;
import com.erplab.masterdata.domain.*;

@Service
@Transactional
public class MasterdataCommandHandler {

	private final UnitRepository units;

	private final CategoryRepository categories;

	private final ProductRepository products;

	private final WarehouseRepository warehouses;

	private final PartnerRepository partners;

	MasterdataCommandHandler(UnitRepository units, CategoryRepository categories, ProductRepository products,
			WarehouseRepository warehouses, PartnerRepository partners) {
		this.units = units;
		this.categories = categories;
		this.products = products;
		this.warehouses = warehouses;
		this.partners = partners;
	}

	// --- units

	public Long createUnit(SaveUnit cmd) {
		return units.save(new Unit(cmd.code(), cmd.name())).getId();
	}

	public void updateUnit(Long id, SaveUnit cmd) {
		load(units, id, "Unit").update(cmd.code(), cmd.name());
	}

	public void deleteUnit(Long id) {
		load(units, id, "Unit").delete();
	}

	// --- categories

	public Long createCategory(SaveCategory cmd) {
		return categories.save(new Category(cmd.name(), cmd.description())).getId();
	}

	public void updateCategory(Long id, SaveCategory cmd) {
		load(categories, id, "Category").update(cmd.name(), cmd.description());
	}

	public void deleteCategory(Long id) {
		load(categories, id, "Category").delete();
	}

	// --- products

	public Long createProduct(SaveProduct cmd) {
		var product = products.save(new Product(cmd.sku(), cmd.type()));
		apply(product, cmd);
		return product.getId();
	}

	public void updateProduct(Long id, SaveProduct cmd) {
		var product = load(products, id, "Product");
		if (!product.getSku().equals(cmd.sku().trim()) || product.getType() != cmd.type()) {
			throw new DomainException("immutable_field", "SKU and type cannot be changed");
		}
		apply(product, cmd);
	}

	private void apply(Product product, SaveProduct cmd) {
		load(units, cmd.unitId(), "Unit");
		if (cmd.categoryId() != null) {
			load(categories, cmd.categoryId(), "Category");
		}
		product.update(cmd.name(), cmd.unitId(), cmd.categoryId(), cmd.purchasePrice(), cmd.salePrice(),
				cmd.minStock(), MasterdataCommands.active(cmd.active()));
		List<BomLine> bom = cmd.bom() == null ? List.of()
				: cmd.bom().stream().map(l -> new BomLine(l.componentId(), l.quantity())).toList();
		bom.forEach(l -> load(products, l.componentId(), "Component"));
		product.replaceBom(bom);
	}

	public void deleteProduct(Long id) {
		load(products, id, "Product").delete();
	}

	// --- warehouses

	public Long createWarehouse(SaveWarehouse cmd) {
		var warehouse = new Warehouse(cmd.code());
		warehouse.update(cmd.name(), cmd.address(), MasterdataCommands.active(cmd.active()));
		return warehouses.save(warehouse).getId();
	}

	public void updateWarehouse(Long id, SaveWarehouse cmd) {
		load(warehouses, id, "Warehouse").update(cmd.name(), cmd.address(), MasterdataCommands.active(cmd.active()));
	}

	public void deleteWarehouse(Long id) {
		load(warehouses, id, "Warehouse").delete();
	}

	// --- partners

	public Long createPartner(SavePartner cmd) {
		var partner = new Partner(cmd.code());
		update(partner, cmd);
		return partners.save(partner).getId();
	}

	public void updatePartner(Long id, SavePartner cmd) {
		update(load(partners, id, "Partner"), cmd);
	}

	private static void update(Partner p, SavePartner c) {
		p.update(c.name(), c.customer(), c.supplier(), c.taxCode(), c.email(), c.phone(), c.address(),
				c.paymentTermDays(), MasterdataCommands.active(c.active()));
	}

	public void deletePartner(Long id) {
		load(partners, id, "Partner").delete();
	}

	private static <E extends BaseEntity> E load(JpaRepository<E, Long> repo, Long id, String what) {
		return repo.findById(id).filter(e -> !e.isDeleted())
			.orElseThrow(() -> new NotFoundException(what, id));
	}

}
