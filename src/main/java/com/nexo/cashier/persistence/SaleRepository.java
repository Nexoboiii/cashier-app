package com.nexo.cashier.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
	List<Sale> findByTimestampBetweenOrderByTimestampAsc(Instant from, Instant to);
}