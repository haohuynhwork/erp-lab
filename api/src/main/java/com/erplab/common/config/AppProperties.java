package com.erplab.common.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Public URLs from {@code WEB_URL} / {@code API_URL}. Their paths are the web and api prefixes
 * (the api's is applied as the servlet context path by {@link ApiPathCustomizer}).
 */
@ConfigurationProperties("app")
public record AppProperties(URI webUrl, URI apiUrl) {

	/** Origin of the web app, e.g. {@code http://localhost:5173}, for CORS. */
	public String webOrigin() {
		return webUrl.getScheme() + "://" + webUrl.getAuthority();
	}

	/** Path of the api URL without trailing slash, e.g. {@code /api}; empty for the root. */
	public String apiPrefix() {
		var path = apiUrl.getPath();
		return path == null || path.equals("/") ? "" : path.replaceAll("/+$", "");
	}

}
