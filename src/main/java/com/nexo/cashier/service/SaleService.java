package com.nexo.cashier.service;

import com.nexo.cashier.model.PaymentMethod;
import com.nexo.cashier.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexo.cashier.model.AuditEventType;
import java.time.Instant;
import java.util.List;

@Service
public class SaleService {

	private static final Logger log = LoggerFactory.getLogger(SaleService.class);

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final AuditService audit;
	private final TillDayRepository tillDayRepository;

	public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
					   AuditService audit, TillDayRepository tillDayRepository) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
		this.audit = audit;
		this.tillDayRepository = tillDayRepository;
	}

	@Transactional
	public Sale createSale(List<SaleLine> lines, PaymentMethod paymentMethod, Integer cashTenderedMinorUnits) {
		try {
			return doCreateSale(lines, paymentMethod, cashTenderedMinorUnits);
		} catch (RuntimeException e) {
			audit.recordFailure(AuditEventType.SALE_FAILED, "SALE", null, null, e.getMessage());
			throw e;
		}
	}

	private Sale doCreateSale(List<SaleLine> lines, PaymentMethod paymentMethod, Integer cashTenderedMinorUnits) {
		if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("a sale needs at least one line");
		if (paymentMethod == null) throw new IllegalArgumentException("payment method is required");

		// no open day means the sale would appear in no report
		if (tillDayRepository.findFirstByClosedAtIsNullOrderByOpenedAtDesc().isEmpty()) {
			throw new IllegalArgumentException("no day is open - open the till first");
		}

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

			int unitPrice = unitPriceFor(product, line.unitPriceMinorUnits());

			// name always comes from the db; so does price unless the product is open-price
			SaleLineItem item = new SaleLineItem(
					product.getId(), product.getName(), unitPrice,
					line.quantity(), product.getSupplier());

			sale.addLine(item);
			total += item.getLineTotal();

			if (product.isOpenPrice()) {
				audit.record(AuditEventType.PRICE_OVERRIDE, "PRODUCT", product.getId(),
						(long) unitPrice, product.getName() + " rung at " + unitPrice);
			}

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
		audit.record(AuditEventType.SALE_COMPLETED, "SALE", saved.getId(),
				(long) saved.getTotalMinorUnits(),
				saved.getLines().size() + " lines, " + paymentMethod);
		return saved;
	}

	// the only place a client-supplied price is ever accepted, and only when the
	// product is flagged open-price. anything else is rejected, not ignored.
	private static int unitPriceFor(Product product, Integer requested) {
		if (product.isOpenPrice()) {
			if (requested == null) {
				throw new IllegalArgumentException(product.getName() + " needs an amount");
			}
			if (requested <= 0) {
				throw new IllegalArgumentException("amount for " + product.getName() + " must be more than zero");
			}
			return requested;
		}
		if (requested != null) {
			throw new IllegalArgumentException("price cannot be set for " + product.getName());
		}
		return product.getPriceMinorUnits();
	}
}
