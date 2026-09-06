package com.nexo.cashier.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public record ApiError(String error, String reqId) {}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<GlobalExceptionHandler.ApiError> badRequest(IllegalArgumentException e) {
		String reqId = MDC.get("reqId");
		log.warn("bad request: {}", e.getMessage());
		return ResponseEntity.badRequest().body(new GlobalExceptionHandler.ApiError(e.getMessage(), reqId));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handle(Exception e) {
		String reqId = MDC.get("reqId");
		log.error("unhandled exception", e);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiError("Something went wrong. Quote code " + reqId, reqId));
	}
}
