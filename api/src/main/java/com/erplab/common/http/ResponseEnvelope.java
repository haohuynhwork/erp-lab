package com.erplab.common.http;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of every REST response.
 * <ul>
 * <li>{@code success}: {@code data} holds the result</li>
 * <li>{@code fail} (4xx): {@code code}/{@code message}; validation issues in {@code data.issues}</li>
 * <li>{@code error} (5xx): {@code message}</li>
 * </ul>
 * Controllers return plain data; {@link ResponseEnvelopeAdvice} and {@link ApiExceptionHandler} wrap it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseEnvelope<T>(Status status, T data, String code, String message) {

	public enum Status {
		success, fail, error
	}

	public static <T> ResponseEnvelope<T> success(T data) {
		return new ResponseEnvelope<>(Status.success, data, null, null);
	}

	public static <T> ResponseEnvelope<T> fail(String code, String message, T data) {
		return new ResponseEnvelope<>(Status.fail, data, code, message);
	}

	public static ResponseEnvelope<Void> error(String message) {
		return new ResponseEnvelope<>(Status.error, null, "internal_error", message);
	}

}
