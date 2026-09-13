package com.nexo.cashier.service;

import com.nexo.cashier.model.AdjustmentReason;
import com.nexo.cashier.model.AuditEventType;
import com.nexo.cashier.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

	private final ProductRepository productRepository;
	private final StockAdjustmentRepository adjustmentRepository;
	private final AuditService audit;

	public InventoryService(ProductRepository productRepository,
							StockAdjustmentRepository adjustmentRepository,
							AuditService audit) {
		this.productRepository = productRepository;
		this.adjustmentRepository = adjustmentRepository;
		this.audit = audit;
	}

	@Transactional
	public Product restock(Long productId, int quantity, String note) {
		if (quantity <= 0) throw new IllegalArgumentException("restock quantity must be at least 1");
		return adjust(productId, quantity, AdjustmentReason.RESTOCK, note);
	}

	@Transactional
	public Product damage(Long productId, int quantity, String note) {
		if (quantity <= 0) throw new IllegalArgumentException("damaged quantity must be at least 1");
		return adjust(productId, -quantity, AdjustmentReason.DAMAGE, note);
	}

	@Transactional
	public Product correctTo(Long productId, int countedStock, String note) {
		if (countedStock < 0) throw new IllegalArgumentException("counted stock cannot be negative");
		Product product = find(productId);
		int delta = countedStock - product.getStockQuantity();
		if (delta == 0) return product;
		return adjust(productId, delta, AdjustmentReason.CORRECTION, note);
	}

	public List<Product> lowStock() {
		return productRepository.findAll().stream()
				.filter(p -> p.getStockQuantity() <= p.getLowStockThreshold())
				.toList();
	}

	public List<StockAdjustment> history() {
		return adjustmentRepository.findAllByOrderByTimestampDesc();
	}

	public List<StockAdjustment> history(Long productId) {
		return adjustmentRepository.findByProductIdOrderByTimestampDesc(productId);
	}

	private Product adjust(Long productId, int delta, AdjustmentReason reason, String note) {
		Product product = find(productId);
		int updated = product.getStockQuantity() + delta;

		if (updated < 0) {
			throw new IllegalArgumentException("that would leave " + product.getName()
					+ " at " + updated + "; there are only " + product.getStockQuantity() + " in stock");
		}

		product.setStockQuantity(updated);
		productRepository.save(product);
		adjustmentRepository.save(new StockAdjustment(productId, delta, reason, note));

		audit.record(AuditEventType.STOCK_ADJUSTED, "PRODUCT", productId, null,
				reason + " " + (delta > 0 ? "+" : "") + delta + " -> " + updated
						+ (note == null || note.isBlank() ? "" : " (" + note + ")"));

		return product;
	}

	private Product find(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("no product with id " + productId));
	}
}