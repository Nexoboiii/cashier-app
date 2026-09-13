package com.nexo.cashier.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

	List<StockAdjustment> findByProductIdOrderByTimestampDesc(Long productId);

	List<StockAdjustment> findAllByOrderByTimestampDesc();
}