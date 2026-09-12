package com.nexo.cashier.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TillDayRepository extends JpaRepository<TillDay, Long> {

	Optional<TillDay> findFirstByClosedAtIsNullOrderByOpenedAtDesc();

	List<TillDay> findAllByOrderByOpenedAtDesc();
}