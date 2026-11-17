package com.erplab.common.http;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;
import com.erplab.common.domain.UnauthorizedException;

/** Turns exceptions into {@link ResponseEnvelope} {@code fail} (4xx) or {@code error} (5xx). */
@RestControllerAdvice
class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	record Issue(String path, String message) {
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ResponseEnvelope<?>> validation(MethodArgumentNotValidException e) {
		List<Issue> issues = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(f -> new Issue(f.getField(), f.getDefaultMessage()))
			.toList();
		return fail(HttpStatus.BAD_REQUEST, "validation_failed", "Invalid request", Map.of("issues", issues));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ResponseEnvelope<?>> unreadable(HttpMessageNotReadableException e) {
		return fail(HttpStatus.BAD_REQUEST, "invalid_body", "Request body is not valid JSON for this endpoint", null);
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ResponseEnvelope<?>> notFound(NotFoundException e) {
		return fail(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage(), null);
	}

	@ExceptionHandler(UnauthorizedException.class)
	ResponseEntity<ResponseEnvelope<?>> unauthorized(UnauthorizedException e) {
		return fail(HttpStatus.UNAUTHORIZED, e.getCode(), e.getMessage(), null);
	}

	@ExceptionHandler(DomainException.class)
	ResponseEntity<ResponseEnvelope<?>> domain(DomainException e) {
		return fail(HttpStatus.UNPROCESSABLE_CONTENT, e.getCode(), e.getMessage(), null);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ResponseEnvelope<?>> conflict(ObjectOptimisticLockingFailureException e) {
		return fail(HttpStatus.CONFLICT, "conflict", "Changed by someone else, reload and try again", null);
	}

	/** Unique index violations (duplicate code, SKU, username, ...). */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ResponseEnvelope<?>> integrity(DataIntegrityViolationException e) {
		var msg = String.valueOf(e.getMostSpecificCause().getMessage());
		if (msg.contains("duplicate key")) {
			return fail(HttpStatus.CONFLICT, "duplicate", "A record with the same code already exists", null);
		}
		if (msg.contains("foreign key")) {
			return fail(HttpStatus.CONFLICT, "in_use", "Referenced record does not exist or is still in use", null);
		}
		log.warn("Data integrity violation", e);
		return fail(HttpStatus.CONFLICT, "integrity", "The change conflicts with existing data", null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ResponseEnvelope<?>> forbidden(AccessDeniedException e) {
		return fail(HttpStatus.FORBIDDEN, "forbidden", "Not allowed", null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ResponseEnvelope<?>> other(Exception e) {
		// Spring MVC's own 4xx (unknown route, 405, unreadable JSON, ...)
		if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
			HttpStatusCode status = er.getStatusCode();
			return fail(status, "http_" + status.value(), er.getBody().getDetail(), null);
		}
		log.error("Unhandled exception", e);
		return ResponseEntity.internalServerError().body(ResponseEnvelope.error("Something went wrong"));
	}

	private static ResponseEntity<ResponseEnvelope<?>> fail(HttpStatusCode status, String code, String message,
			Object data) {
		return ResponseEntity.status(status).body(ResponseEnvelope.fail(code, message, data));
	}

}
