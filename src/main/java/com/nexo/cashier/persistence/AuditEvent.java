package com.nexo.cashier.persistence;

import com.nexo.cashier.model.AuditEventType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant timestamp;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private AuditEventType type;

	@Column(length = 16)
	private String entityType;

	private Long entityId;

	// null unless money moved
	private Long amountMinorUnits;

	@Column(length = 1000)
	private String detail;

	protected AuditEvent() {
		// jpa
	}

	public AuditEvent(AuditEventType type, String entityType, Long entityId,
					  Long amountMinorUnits, String detail) {
		this.timestamp = Instant.now();
		this.type = type;
		this.entityType = entityType;
		this.entityId = entityId;
		this.amountMinorUnits = amountMinorUnits;
		this.detail = detail;
	}

	public Long getId() { return id; }
	public Instant getTimestamp() { return timestamp; }
	public AuditEventType getType() { return type; }
	public String getEntityType() { return entityType; }
	public Long getEntityId() { return entityId; }
	public Long getAmountMinorUnits() { return amountMinorUnits; }
	public String getDetail() { return detail; }
}