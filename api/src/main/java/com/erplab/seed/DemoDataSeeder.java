package com.erplab.seed;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.erplab.auth.application.command.AuthCommandHandler;
import com.erplab.auth.application.command.AuthCommands.CreateUser;
import com.erplab.auth.domain.Role;
import com.erplab.common.domain.DomainException;
import com.erplab.inventory.application.command.InventoryCommandHandler;
import com.erplab.inventory.application.command.InventoryCommands.ReceiveStock;
import com.erplab.masterdata.application.command.MasterdataCommandHandler;
import com.erplab.masterdata.application.command.MasterdataCommands.*;
import com.erplab.masterdata.application.query.MasterdataQueries;
import com.erplab.masterdata.domain.ProductType;
import com.erplab.purchasing.application.command.PurchasingCommandHandler;
import com.erplab.purchasing.application.command.PurchasingCommands.*;
import com.erplab.purchasing.application.query.PurchasingQueries;
import com.erplab.sales.application.command.SalesCommandHandler;
import com.erplab.sales.application.command.SalesCommands.*;
import com.erplab.sales.application.query.SalesQueries;

/**
 * Demo data for an empty database: master data, one user per role, opening stock and about four months of
 * purchasing and sales history (with payments, a credit note, open quotations and reserved orders).
 * Everything goes through the command handlers, so it follows the same rules and postings as the app.
 * Run with {@code make seed} ({@code --app.seed-demo=true}); skipped when products already exist.
 */
@Component
@Order(10)
@ConditionalOnBooleanProperty("app.seed-demo")
class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private static final int DAYS = 120;

	private final MasterdataCommandHandler masterdata;

	private final MasterdataQueries masterdataQueries;

	private final AuthCommandHandler auth;

	private final InventoryCommandHandler inventory;

	private final PurchasingCommandHandler purchasing;

	private final PurchasingQueries purchasingQueries;

	private final SalesCommandHandler sales;

	private final SalesQueries salesQueries;

	private final String password;

	private final ApplicationContext context;

	/** Fixed seed: the same demo data every time. */
	private final Random random = new Random(42);

	private final LocalDate today = LocalDate.now();

	private final Map<String, Long> ids = new LinkedHashMap<>();

	DemoDataSeeder(MasterdataCommandHandler masterdata, MasterdataQueries masterdataQueries, AuthCommandHandler auth,
			InventoryCommandHandler inventory, PurchasingCommandHandler purchasing, PurchasingQueries purchasingQueries,
			SalesCommandHandler sales, SalesQueries salesQueries, @Value("${app.seed-password}") String password,
			ApplicationContext context) {
		this.context = context;
		this.masterdata = masterdata;
		this.masterdataQueries = masterdataQueries;
		this.auth = auth;
		this.inventory = inventory;
		this.purchasing = purchasing;
		this.purchasingQueries = purchasingQueries;
		this.sales = sales;
		this.salesQueries = salesQueries;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (masterdataQueries.products(null, null, null, 0, 1).total() > 0) {
			log.info("Products exist; demo data not seeded");
			exitWhenSeedingOnly();
			return;
		}
		users();
		masterData();
		openingStock();
		history();
		log.info("Demo data seeded");
		exitWhenSeedingOnly();
	}

	/** {@code make seed} runs without a web server: stop once done (a stray scheduler thread keeps the JVM up). */
	private void exitWhenSeedingOnly() {
		if (!(context instanceof WebServerApplicationContext)) {
			// own thread: exiting on the startup thread deadlocks with Spring's shutdown hook
			new Thread(() -> System.exit(SpringApplication.exit(context)), "seed-exit").start();
		}
	}

	private void users() {
		for (var role : List.of(Role.MANAGER, Role.SALES, Role.PURCHASING, Role.WAREHOUSE, Role.ACCOUNTANT)) {
			var name = role.name().toLowerCase();
			auth.handle(new CreateUser(name, password, Character.toUpperCase(name.charAt(0)) + name.substring(1) + " User",
					name + "@erplab.local", Set.of(role)));
		}
	}

	private void masterData() {
		ids.put("pcs", masterdata.createUnit(new SaveUnit("pcs", "Pieces")));
		ids.put("l", masterdata.createUnit(new SaveUnit("l", "Litre")));
		ids.put("m", masterdata.createUnit(new SaveUnit("m", "Metre")));
		ids.put("furniture", masterdata.createCategory(new SaveCategory("Furniture", "Finished furniture")));
		ids.put("materials", masterdata.createCategory(new SaveCategory("Raw materials", "Wood, glue, finishes")));
		ids.put("hardware", masterdata.createCategory(new SaveCategory("Hardware", "Screws, hinges, fittings")));
		ids.put("lighting", masterdata.createCategory(new SaveCategory("Lighting", "Trading goods")));

		product("OAK-BOARD", "Oak board 2m", ProductType.RAW, "m", "materials", "10", "0", "80", List.of());
		product("PINE-BOARD", "Pine board 2m", ProductType.RAW, "m", "materials", "6", "0", "120", List.of());
		product("GLUE-1L", "Wood glue 1 l", ProductType.RAW, "l", "materials", "8", "0", "10", List.of());
		product("VARNISH-1L", "Clear varnish 1 l", ProductType.RAW, "l", "materials", "12", "0", "10", List.of());
		product("SCREW-4X40", "Screw 4×40", ProductType.RAW, "pcs", "hardware", "0.1", "0", "1000", List.of());
		product("HINGE-35", "Cabinet hinge 35 mm", ProductType.RAW, "pcs", "hardware", "1.5", "0", "100", List.of());
		product("TOP-OAK", "Oak tabletop", ProductType.SEMI_FINISHED, "pcs", "furniture", "0", "0", "0",
				List.of(bom("OAK-BOARD", "3"), bom("GLUE-1L", "0.2")));
		product("TABLE-OAK", "Oak dining table", ProductType.FINISHED, "pcs", "furniture", "0", "320", "8",
				List.of(bom("TOP-OAK", "1"), bom("PINE-BOARD", "2"), bom("SCREW-4X40", "16"), bom("VARNISH-1L", "0.3")));
		product("CHAIR-PINE", "Pine chair", ProductType.FINISHED, "pcs", "furniture", "0", "85", "30",
				List.of(bom("PINE-BOARD", "2"), bom("SCREW-4X40", "12"), bom("GLUE-1L", "0.1")));
		product("CABINET-OAK", "Oak cabinet", ProductType.FINISHED, "pcs", "furniture", "0", "450", "5",
				List.of(bom("OAK-BOARD", "4"), bom("HINGE-35", "4"), bom("SCREW-4X40", "24"), bom("VARNISH-1L", "0.5")));
		product("LAMP-DESK", "Desk lamp", ProductType.GOODS, "pcs", "lighting", "18", "35", "20", List.of());
		product("ASSEMBLY", "Assembly service (per hour)", ProductType.SERVICE, "pcs", null, "0", "40", "0", List.of());

		ids.put("MAIN", masterdata.createWarehouse(new SaveWarehouse("MAIN", "Main warehouse", "12 Factory Rd", true)));
		ids.put("SHOP", masterdata.createWarehouse(new SaveWarehouse("SHOP", "City showroom", "5 Market St", true)));

		partner("C001", "Acme Retail", true, false, 30);
		partner("C002", "Bright Homes", true, false, 14);
		partner("C003", "Cosy Cafe Chain", true, false, 30);
		partner("C004", "Delta Offices", true, false, 45);
		partner("C005", "Evergreen Hotels", true, false, 30);
		partner("S001", "Timber Co", false, true, 30);
		partner("S002", "FixIt Hardware", false, true, 15);
		partner("S003", "Lumina Imports", false, true, 30);
	}

	/** Finished goods are made outside the system until MES exists: opening stock at estimated cost. */
	private void openingStock() {
		var date = today.minusDays(DAYS + 1);
		receive("TABLE-OAK", "MAIN", "70", "175", date);
		receive("CHAIR-PINE", "MAIN", "260", "42", date);
		receive("CABINET-OAK", "MAIN", "30", "255", date);
		receive("TABLE-OAK", "SHOP", "6", "175", date);
		receive("CHAIR-PINE", "SHOP", "24", "42", date);
		receive("OAK-BOARD", "MAIN", "200", "10", date);
		receive("PINE-BOARD", "MAIN", "300", "6", date);
	}

	private void history() {
		var customers = List.of("C001", "C002", "C003", "C004", "C005");
		for (var day = today.minusDays(DAYS); day.isBefore(today); day = day.plusDays(1)) {
			if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
				continue;
			}
			if (day.getDayOfWeek() == DayOfWeek.MONDAY && day.getDayOfMonth() <= 7) {
				monthlyPurchases(day);
			}
			if (random.nextInt(100) < 45) {
				sale(customers.get(random.nextInt(customers.size())), day);
			}
		}
		// still open: a quotation, a confirmed order waiting for delivery (reserves stock), an unpaid bill
		quotation("C004", today.minusDays(2));
		var open = salesOrder("C005", today.minusDays(1), Map.of("CABINET-OAK", "4", "TABLE-OAK", "6"));
		sales.confirm(open);
	}

	private void monthlyPurchases(LocalDate day) {
		var timber = purchaseOrder("S001", day, Map.of("OAK-BOARD", "120", "PINE-BOARD", "200"));
		var hardware = purchaseOrder("S002", day, Map.of("SCREW-4X40", "3000", "HINGE-35", "200", "GLUE-1L", "20",
				"VARNISH-1L", "20"));
		var lamps = purchaseOrder("S003", day, Map.of("LAMP-DESK", "40"));
		for (var po : List.of(timber, hardware, lamps)) {
			purchasing.confirm(po);
			var arrival = day.plusDays(3 + random.nextInt(5));
			if (arrival.isAfter(today)) {
				continue;
			}
			receiveAll(po, arrival);
			var billDate = arrival.plusDays(1);
			if (billDate.isAfter(today)) {
				continue;
			}
			var bill = purchasing.bill(po, new CreateBill("SUP-" + (1000 + random.nextInt(9000)), billDate));
			var payDate = billDate.plusDays(14 + random.nextInt(10));
			if (payDate.isBefore(today)) {
				var total = purchasingQueries.bill(bill).bill().total();
				purchasing.pay(bill, new PayBill(total, payDate, "BANK", null));
			}
		}
	}

	private void sale(String customer, LocalDate day) {
		var lines = new LinkedHashMap<String, String>();
		lines.put(pick("TABLE-OAK", "CHAIR-PINE", "CABINET-OAK"), String.valueOf(1 + random.nextInt(4)));
		if (random.nextBoolean()) {
			lines.put("CHAIR-PINE", String.valueOf(2 + random.nextInt(6)));
		}
		if (random.nextInt(4) == 0) {
			lines.put("LAMP-DESK", String.valueOf(1 + random.nextInt(5)));
		}
		if (random.nextInt(5) == 0) {
			lines.put("ASSEMBLY", String.valueOf(1 + random.nextInt(3)));
		}
		var order = salesOrder(customer, day, lines);
		sales.confirm(order);
		var shipDate = day.plusDays(1 + random.nextInt(4));
		if (shipDate.isAfter(today)) {
			return;
		}
		try {
			deliverAll(order, shipDate);
		}
		catch (DomainException e) {
			// out of stock: leave the order confirmed (it keeps its reservation)
			return;
		}
		var invoice = sales.invoice(order, new CreateInvoice(shipDate));
		var detail = salesQueries.invoice(invoice).invoice();
		if (random.nextInt(25) == 0) {
			sales.credit(invoice, new CreateCreditNote(shipDate.plusDays(3), "Scratched surface, price reduction",
					List.of(new CreditLine(0, BigDecimal.ONE))));
		}
		// most customers pay a bit late, some very late (overdue)
		var payDate = detail.dueDate().plusDays(random.nextInt(20) - 10);
		if (payDate.isBefore(today) && random.nextInt(10) > 0) {
			var balance = salesQueries.invoice(invoice).invoice().balance();
			if (balance.signum() > 0) {
				sales.recordPayment(invoice, new RecordPayment(balance, payDate, random.nextBoolean() ? "BANK" : "CARD",
						null));
			}
		}
	}

	private void quotation(String customer, LocalDate day) {
		var lines = Map.of("TABLE-OAK", "10", "CHAIR-PINE", "40", "ASSEMBLY", "8");
		sales.create(new SaveSalesOrder(id(customer), id("MAIN"), day, day.plusDays(21), day.plusDays(14),
				new BigDecimal("10"), "Office canteen fit-out", salesLines(lines)));
	}

	// --- helpers

	private Long salesOrder(String customer, LocalDate day, Map<String, String> lines) {
		return sales.create(new SaveSalesOrder(id(customer), id("MAIN"), day, day.plusDays(3), null, new BigDecimal("10"),
				null, salesLines(lines)));
	}

	private List<SalesLineInput> salesLines(Map<String, String> lines) {
		return lines.entrySet().stream()
			.map(e -> new SalesLineInput(id(e.getKey()), new BigDecimal(e.getValue()), null))
			.toList();
	}

	private Long purchaseOrder(String supplier, LocalDate day, Map<String, String> lines) {
		var input = new ArrayList<PurchaseLineInput>();
		lines.forEach((sku, qty) -> input.add(new PurchaseLineInput(id(sku), new BigDecimal(qty), null)));
		return purchasing.create(new SavePurchaseOrder(id(supplier), id("MAIN"), day, day.plusDays(7), null, input));
	}

	private void receiveAll(Long po, LocalDate date) {
		var lines = purchasingQueries.get(po).lines().stream()
			.map(l -> new ReceiptLine(l.id(), l.quantity().subtract(l.receivedQuantity())))
			.toList();
		purchasing.receive(po, new ReceiveGoods(date, lines));
	}

	private void deliverAll(Long so, LocalDate date) {
		var lines = salesQueries.order(so).lines().stream()
			.map(l -> new DeliveryLine(l.id(), l.quantity().subtract(l.deliveredQuantity())))
			.toList();
		sales.deliver(so, new Deliver(date, lines));
	}

	private void product(String sku, String name, ProductType type, String unit, String category, String cost,
			String price, String minStock, List<BomLineInput> bom) {
		ids.put(sku, masterdata.createProduct(new SaveProduct(sku, name, type, id(unit), category == null ? null : id(category),
				new BigDecimal(cost), new BigDecimal(price), new BigDecimal(minStock), true, bom)));
	}

	private BomLineInput bom(String sku, String qty) {
		return new BomLineInput(id(sku), new BigDecimal(qty));
	}

	private void partner(String code, String name, boolean customer, boolean supplier, int terms) {
		var email = name.toLowerCase().replaceAll("[^a-z]+", ".") + "example.com";
		ids.put(code, masterdata.createPartner(new SavePartner(code, name, customer, supplier, "0" + (310000000 + ids.size()),
				email.replace(".example", "@example"), "+84 28 " + (3000_0000 + random.nextInt(999_9999)),
				(10 + random.nextInt(200)) + " Le Loi, District 1, Ho Chi Minh City", terms, true)));
	}

	private void receive(String sku, String warehouse, String qty, String cost, LocalDate date) {
		inventory.handle(new ReceiveStock(id(sku), id(warehouse), new BigDecimal(qty), new BigDecimal(cost),
				"Opening balance", date));
	}

	@SafeVarargs
	private <T> T pick(T... options) {
		return options[random.nextInt(options.length)];
	}

	private Long id(String key) {
		var id = ids.get(key);
		if (id == null) {
			throw new IllegalStateException("Unknown seed key " + key);
		}
		return id;
	}


}
