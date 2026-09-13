package com.nexo.cashier.service;

import com.nexo.cashier.model.AuditEventType;
import com.nexo.cashier.persistence.AuditEvent;
import com.nexo.cashier.persistence.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	private static final int MAX_DETAIL = 1000;

	private final AuditRepository repository;

	public AuditService(AuditRepository repository) {
		this.repository = repository;
	}

	// joins the caller's transaction - row lives or dies with the event it describes
	public void record(AuditEventType type, String entityType, Long entityId,
					   Long amountMinorUnits, String detail) {
		write(type, entityType, entityId, amountMinorUnits, detail);
	}

	// own transaction - survives the rollback that produced it
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailure(AuditEventType type, String entityType, Long entityId,
							  Long amountMinorUnits, String detail) {
		write(type, entityType, entityId, amountMinorUnits, detail);
	}

	private void write(AuditEventType type, String entityType, Long entityId,
					   Long amountMinorUnits, String detail) {
		try {
			repository.save(new AuditEvent(type, entityType, entityId, amountMinorUnits, trim(detail)));
		} catch (Exception e) {
			log.error("could not write audit event {}", type, e);
		}
	}

	private static String trim(String detail) {
		if (detail == null || detail.length() <= MAX_DETAIL) return detail;
		return detail.substring(0, MAX_DETAIL - 3) + "...";
	}
}