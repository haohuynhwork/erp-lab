package com.erplab.common.http;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Wraps what our controllers return in {@link ResponseEnvelope#success}. */
@RestControllerAdvice(basePackages = "com.erplab")
class ResponseEnvelopeAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return !StringHttpMessageConverter.class.isAssignableFrom(converterType)
				&& !ResponseEnvelope.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
			Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body instanceof ResponseEnvelope<?> || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
			return body;
		}
		return ResponseEnvelope.success(body);
	}

}
