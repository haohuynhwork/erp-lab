package com.erplab.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
class OpenApiConfig {

	/** Swagger UI tag order; each context's controllers use {@code @Tag(name = "<context>")}. */
	private static final List<Tag> TAGS = List.of(
			new Tag().name("auth").description("Sign in, tokens, users and roles"),
			new Tag().name("masterdata").description("Products, BOMs, units, categories, warehouses, partners"),
			new Tag().name("inventory").description("Stock, reservations, movements, receipts, transfers, counts"),
			new Tag().name("purchasing").description("Purchase orders, goods receipts, supplier bills and payments"),
			new Tag().name("sales").description("Quotations, sales orders, deliveries, invoices, credit notes, payments"),
			new Tag().name("accounting").description("Chart of accounts, journal, statements, balances"),
			new Tag().name("report").description("Dashboard, Excel exports, PDFs"),
			new Tag().name("audit").description("Who did what"),
			new Tag().name("system").description("Health and app info"));

	@Bean
	OpenAPI openApi(AppProperties app) {
		return new OpenAPI()
			.info(new Info().title("ERP Lab API").version("v1"))
			.servers(List.of(new Server().url(app.apiUrl().toString())))
			.tags(TAGS);
	}

}
