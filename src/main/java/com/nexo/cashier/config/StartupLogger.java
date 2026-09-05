package com.nexo.cashier.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger {

	private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

	@Value("${server.port:8080}") private String port;
	@Value("${LOG_DIR:logs}") private String logDir;
	@Value("${spring.datasource.url:(none set - boot auto-configured an in-memory db)}") private String dbUrl;

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		log.info("ready | port={} | logs={} | db={}", port, logDir, dbUrl);
	}

	@PreDestroy
	public void onStop() {
		log.info("stopping");
	}
}