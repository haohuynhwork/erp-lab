package com.erplab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The whole ERP flow over HTTP against a real TimescaleDB: master data → purchase → receipt → bill → payment →
 * sales order (reservation) → delivery → invoice → credit note → payment, then checks stock, the ledger and the
 * audit log. Business rules that must reject requests are checked on the way.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.api-url=http://localhost/api")
class BusinessFlowIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	JsonMapper json;

	RestClient http;

	record Response(int status, JsonNode body) {

		JsonNode data() {
			return body.get("data");
		}

		long id() {
			return data().get("id").asLong();
		}

		String code() {
			return body.path("code").asString();
		}

	}

	@BeforeEach
	void signIn() {
		http = RestClient.builder().baseUrl("http://localhost:" + port + "/api/v1").build();
		var token = call("POST", "/auth/login", Map.of("username", "admin", "password", "admin12345")).data()
			.get("accessToken")
			.asString();
		http = http.mutate().defaultHeader("Authorization", "Bearer " + token).build();
	}

	@Test
	void purchaseToCashKeepsStockAndLedgerConsistent() {
		long unit = post("/units", Map.of("code", "pcs-" + port, "name", "Pieces")).id();
		long product = post("/products", Map.of("sku", "IT-" + port, "name", "Widget", "type", "GOODS", "unitId", unit,
				"purchasePrice", 10, "salePrice", 25, "minStock", 0)).id();
		long warehouse = post("/warehouses", Map.of("code", "IT" + port, "name", "Test")).id();
		long supplier = post("/partners", Map.of("code", "S" + port, "name", "Supplier", "supplier", true,
				"paymentTermDays", 10)).id();
		long customer = post("/partners", Map.of("code", "C" + port, "name", "Customer", "customer", true,
				"paymentTermDays", 30)).id();

		// purchase 10 @ 10, receive, bill, pay
		long po = post("/purchase-orders", Map.of("supplierId", supplier, "warehouseId", warehouse, "orderDate",
				"2026-01-05", "lines", new Object[] { Map.of("productId", product, "quantity", 10) })).id();
		assertThat(post("/purchase-orders/" + po + "/receipts", receipt(po, 10)).code()).isEqualTo("invalid_status");
		post("/purchase-orders/" + po + "/confirm", Map.of());
		assertThat(post("/purchase-orders/" + po + "/receipts", receipt(po, 11)).code()).isEqualTo("over_receipt");
		post("/purchase-orders/" + po + "/receipts", receipt(po, 10));
		long bill = post("/purchase-orders/" + po + "/bills", Map.of("supplierReference", "S-1")).id();
		assertThat(post("/bills/" + bill + "/payments", Map.of("amount", 101, "method", "BANK")).code())
			.isEqualTo("overpayment");
		post("/bills/" + bill + "/payments", Map.of("amount", 100, "method", "BANK"));

		// sell 12 (only 10 in stock): confirm reserves 12, delivering 12 fails, delivering 8 works
		long so = post("/sales-orders", Map.of("customerId", customer, "warehouseId", warehouse, "orderDate", "2026-01-10",
				"taxRate", 10, "lines", new Object[] { Map.of("productId", product, "quantity", 12) })).id();
		post("/sales-orders/" + so + "/confirm", Map.of());
		var stock = get("/inventory/stock?productId=" + product).data().get("items").get(0);
		assertThat(stock.get("reserved").decimalValue()).isEqualByComparingTo("12");
		assertThat(stock.get("available").decimalValue()).isEqualByComparingTo("-2");
		assertThat(post("/sales-orders/" + so + "/deliveries", delivery(so, 12)).code()).isEqualTo("insufficient_stock");
		post("/sales-orders/" + so + "/deliveries", delivery(so, 8));

		// invoice 8 × 25 + 10% = 220, credit 1 (27.50), pay the rest
		long invoice = post("/sales-orders/" + so + "/invoices", Map.of()).id();
		post("/invoices/" + invoice + "/credit-notes", Map.of("reason", "damaged", "lines",
				new Object[] { Map.of("position", 0, "quantity", 1) }));
		var inv = get("/invoices/" + invoice).data().get("invoice");
		assertThat(inv.get("balance").decimalValue()).isEqualByComparingTo("192.50");
		post("/invoices/" + invoice + "/payments", Map.of("amount", 192.50, "method", "BANK"));
		assertThat(get("/invoices/" + invoice).data().get("invoice").get("status").asString()).isEqualTo("PAID");

		// stock: 2 left at cost 10, 4 still reserved
		stock = get("/inventory/stock?productId=" + product).data().get("items").get(0);
		assertThat(stock.get("quantity").decimalValue()).isEqualByComparingTo("2");
		assertThat(stock.get("reserved").decimalValue()).isEqualByComparingTo("4");

		// ledger: balanced, and the balance sheet adds up
		var tb = get("/accounting/trial-balance").data();
		assertThat(tb.get("totalDebit").decimalValue()).isEqualByComparingTo(tb.get("totalCredit").decimalValue());
		assertThat(get("/accounting/balance-sheet").data().get("balanced").asBoolean()).isTrue();
		var today = java.time.LocalDate.now();
		var pl = get("/accounting/profit-and-loss?from=" + today + "&to=" + today).data();
		// receipt, delivery, invoice and credit are dated today: revenue 200 - credited 25 = 175; COGS 8 × 10 = 80
		assertThat(pl.get("netProfit").decimalValue()).isEqualByComparingTo("95");

		// audit: logins and the delivery (with the order id) are logged
		assertThat(get("/audit-log?q=Login").data().get("total").asLong()).isPositive();
		var audit = get("/audit-log?context=sales&q=deliver").data().get("items");
		assertThat(audit.findValues("targetId").stream().map(JsonNode::asLong)).contains(so);
	}

	@Test
	void rolesLimitWhatUsersCanDo() {
		post("/users", Map.of("username", "it-sales-" + port, "password", "password123", "fullName", "Sales",
				"roles", new String[] { "SALES" }));
		var token = call("POST", "/auth/login", Map.of("username", "it-sales-" + port, "password", "password123"))
			.data()
			.get("accessToken")
			.asString();
		var sales = http.mutate().defaultHeader("Authorization", "Bearer " + token).build();
		var original = http;
		http = sales;
		try {
			assertThat(get("/sales-orders").status()).isEqualTo(200);
			assertThat(get("/accounting/trial-balance").status()).isEqualTo(403);
			assertThat(post("/purchase-orders/1/confirm", Map.of()).status()).isEqualTo(403);
		}
		finally {
			http = original;
		}
	}

	private Map<String, Object> receipt(long po, int qty) {
		long line = get("/purchase-orders/" + po).data().get("lines").get(0).get("id").asLong();
		return Map.of("lines", new Object[] { Map.of("lineId", line, "quantity", qty) });
	}

	private Map<String, Object> delivery(long so, int qty) {
		long line = get("/sales-orders/" + so).data().get("lines").get(0).get("id").asLong();
		return Map.of("lines", new Object[] { Map.of("lineId", line, "quantity", qty) });
	}

	private Response get(String path) {
		return call("GET", path, null);
	}

	private Response post(String path, Object body) {
		return call("POST", path, body);
	}

	private Response call(String method, String path, Object body) {
		var spec = http.method(org.springframework.http.HttpMethod.valueOf(method)).uri(path);
		if (body != null) {
			spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
		}
		return spec.exchange((req, res) -> {
			var bytes = res.getBody().readAllBytes();
			return new Response(res.getStatusCode().value(), bytes.length == 0 ? json.createObjectNode() : json.readTree(bytes));
		});
	}

}
