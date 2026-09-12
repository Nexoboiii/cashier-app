package com.nexo.cashier.api;

import com.nexo.cashier.model.AuditEventType;
import com.nexo.cashier.persistence.AuditEvent;
import com.nexo.cashier.persistence.AuditRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

	private static final int MAX_LIMIT = 1000;

	private final AuditRepository repository;

	public AuditController(AuditRepository repository) {
		this.repository = repository;
	}

	public record AuditResponse(Long id, Instant timestamp, AuditEventType type,
								String entityType, Long entityId,
								Long amountMinorUnits, String detail) {

		static AuditResponse from(AuditEvent a) {
			return new AuditResponse(a.getId(), a.getTimestamp(), a.getType(),
					a.getEntityType(), a.getEntityId(), a.getAmountMinorUnits(), a.getDetail());
		}
	}

	@GetMapping
	public List<AuditResponse> list(@RequestParam(required = false) Instant from,
									@RequestParam(required = false) Instant to,
									@RequestParam(required = false) AuditEventType type,
									@RequestParam(defaultValue = "200") int limit) {

		int capped = Math.clamp(limit, 1, MAX_LIMIT);
		return repository.search(from, to, type, PageRequest.of(0, capped))
				.stream().map(AuditResponse::from).toList();
	}
}