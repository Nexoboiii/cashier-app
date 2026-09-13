package com.nexo.cashier.config;

import com.nexo.cashier.model.AuditEventType;
import com.nexo.cashier.service.AuditService;
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

	private final AuditService audit;

	public StartupLogger(AuditService audit) {
		this.audit = audit;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		log.info("ready | port={} | logs={} | db={}", port, logDir, dbUrl);
		audit.record(AuditEventType.APP_STARTED, "APP", null, null, "port " + port);
	}

	@PreDestroy
	public void onStop() {
		log.info("stopping");
		audit.record(AuditEventType.APP_STOPPED, "APP", null, null, null);
	}
}