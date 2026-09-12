package com.nexo.cashier.service;

import com.nexo.cashier.model.PaymentMethod;
import com.nexo.cashier.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SaleServiceTest {

	@Autowired SaleService saleService;
	@Autowired SaleRepository saleRepository;
	@Autowired ProductRepository productRepository;
	@Autowired AuditRepository auditRepository;

	@Test
	void aFailedSaleLeavesNothingBehind() {
		Product plenty = productRepository.save(new Product("RB Sticker", 200, 10, 2));
		Product scarce = productRepository.save(new Product("RB Poster", 1500, 1, 1));

		long salesBefore = saleRepository.count();

		// line 1 succeeds and decrements, line 2 has only 1 in stock
		assertThrows(IllegalArgumentException.class, () -> saleService.createSale(
				List.of(new SaleLine(plenty.getId(), 3),
						new SaleLine(scarce.getId(), 5)),
				PaymentMethod.CASH, 10000));

		assertEquals(salesBefore, saleRepository.count(), "a failed sale wrote a sale row");
		assertEquals(10, productRepository.findById(plenty.getId()).orElseThrow().getStockQuantity(),
				"a failed sale left stock decremented");
	}

	@Test
	void aCashSaleTotalsAndGivesChange() {
		Product badge = productRepository.save(new Product("RB Badge", 350, 10, 2));

		Sale sale = saleService.createSale(
				List.of(new SaleLine(badge.getId(), 3)),
				PaymentMethod.CASH, 2000);

		assertEquals(1050, sale.getTotalMinorUnits());
		assertEquals(950, sale.getChangeGivenMinorUnits());
		assertEquals(7, productRepository.findById(badge.getId()).orElseThrow().getStockQuantity());

		SaleLineItem line = sale.getLines().get(0);
		assertEquals("RB Badge", line.getProductNameAtSale());
		assertEquals(350, line.getUnitPriceAtSale());
		assertEquals(1050, line.getLineTotal());
	}

	@Test
	void aFailedSaleStillWritesAnAuditRow() {
		Product scarce = productRepository.save(new Product("RB Pin", 500, 1, 1));

		long salesBefore = saleRepository.count();
		long auditBefore = auditRepository.count();

		assertThrows(IllegalArgumentException.class, () -> saleService.createSale(
				List.of(new SaleLine(scarce.getId(), 5)),
				PaymentMethod.CASH, 10000));

		assertEquals(salesBefore, saleRepository.count(), "a failed sale wrote a sale row");
		assertEquals(auditBefore + 1, auditRepository.count(),
				"SALE_FAILED did not survive the rollback - check REQUIRES_NEW and that AuditService is a separate bean");
	}
}