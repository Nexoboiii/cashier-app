package com.nexo.cashier.persistence;

import com.nexo.cashier.model.AuditEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

	@Query("""
			select a from AuditEvent a
			where (:from is null or a.timestamp >= :from)
			  and (:to is null or a.timestamp <= :to)
			  and (:type is null or a.type = :type)
			order by a.timestamp desc
			""")
	List<AuditEvent> search(@Param("from") Instant from,
							@Param("to") Instant to,
							@Param("type") AuditEventType type,
							Pageable page);
}