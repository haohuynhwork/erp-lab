package com.erplab.system.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erplab.common.config.AppProperties;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "system")
@RestController
@RequestMapping("/system")
class SystemController {

	record Info(String name, String version, String webUrl, String apiUrl) {
	}

	private final AppProperties app;

	SystemController(AppProperties app) {
		this.app = app;
	}

	@GetMapping("/info")
	Info info() {
		return new Info("ERP Lab", "v1", app.webUrl().toString(), app.apiUrl().toString());
	}

}
