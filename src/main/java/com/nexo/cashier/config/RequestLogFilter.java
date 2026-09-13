package com.nexo.cashier.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLogFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {

		MDC.put("reqId", UUID.randomUUID().toString().substring(0, 8));
		long start = System.nanoTime();

		try {
			chain.doFilter(req, res);
		} finally {
			long ms = (System.nanoTime() - start) / 1_000_000;
			log.info("{} {} -> {} ({}ms)", req.getMethod(), req.getRequestURI(), res.getStatus(), ms);
			MDC.clear();
		}
	}
}
