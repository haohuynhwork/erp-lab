package com.erplab.common.config;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

/** Serves the whole api under the path of {@code API_URL} (e.g. {@code /api}). */
@Component
class ApiPathCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

	private final AppProperties app;

	ApiPathCustomizer(AppProperties app) {
		this.app = app;
	}

	@Override
	public void customize(ConfigurableServletWebServerFactory factory) {
		factory.setContextPath(app.apiPrefix());
	}

}
