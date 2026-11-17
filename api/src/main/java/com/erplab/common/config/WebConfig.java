package com.erplab.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

	/** REST version prefix for our controllers only (springdoc, actuator stay unversioned). */
	public static final String REST_VERSION = "/v1";

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(REST_VERSION, type -> type.isAnnotationPresent(RestController.class)
				&& type.getPackageName().startsWith("com.erplab"));
	}

}
