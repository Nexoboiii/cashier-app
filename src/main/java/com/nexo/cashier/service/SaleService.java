package com.nexo.cashier.service;

import com.nexo.cashier.model.PaymentMethod;
import com.nexo.cashier.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SaleService {

	private static final Logger log = LoggerFactory.getLogger(SaleService.class);

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;

	public SaleService(SaleRepository saleRepository, ProductRepository productRepository) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
	}

	@Transactional
	public Sale createSale(List<SaleLine> lines, PaymentMethod paymentMethod, Integer cashTenderedMinorUnits) {

		if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("a sale needs at least one line");
		if (paymentMethod == null) throw new IllegalArgumentException("payment method is required");

		Sale sale = new Sale(Instant.now(), paymentMethod);
		int total = 0;

		for (SaleLine line : lines) {
			if (line.quantity() <= 0) throw new IllegalArgumentException("quantity must be at least 1");

			Product product = productRepository.findById(line.productId())
					.orElseThrow(() -> new IllegalArgumentException("no product with id " + line.productId()));

			if (product.getStockQuantity() < line.quantity()) {
				throw new IllegalArgumentException(
						"only " + product.getStockQuantity() + " left of " + product.getName());
			}

			// price and name come from the db, never from the client
			SaleLineItem item = new SaleLineItem(
					product.getId(), product.getName(), product.getPriceMinorUnits(), line.quantity());

			sale.addLine(item);
			total += item.getLineTotal();

			product.setStockQuantity(product.getStockQuantity() - line.quantity());
			productRepository.save(product);
		}

		sale.setTotalMinorUnits(total);

		if (paymentMethod == PaymentMethod.CASH) {
			if (cashTenderedMinorUnits == null) {
				throw new IllegalArgumentException("cash tendered is required on a cash sale");
			}
			if (cashTenderedMinorUnits < total) {
				throw new IllegalArgumentException(
						"cash tendered (" + cashTenderedMinorUnits + ") is less than the total (" + total + ")");
			}
			sale.setCashTenderedMinorUnits(cashTenderedMinorUnits);
			sale.setChangeGivenMinorUnits(cashTenderedMinorUnits - total);
		}

		Sale saved = saleRepository.save(sale);
		log.info("sale {} completed: {} lines, total {}, {}",
				saved.getId(), saved.getLines().size(), saved.getTotalMinorUnits(), paymentMethod);
		return saved;
	}
}