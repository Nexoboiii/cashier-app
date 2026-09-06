package com.nexo.cashier.service;

import com.nexo.cashier.persistence.Product;
import com.nexo.cashier.persistence.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

	private final ProductRepository repository;

	public ProductService(ProductRepository repository) {
		this.repository = repository;
	}

	public List<Product> findAll() {
		return repository.findAll();
	}

	public Product create(String name, int priceMinorUnits, int stockQuantity, int lowStockThreshold) {
		validate(name, priceMinorUnits, stockQuantity, lowStockThreshold);
		if (repository.findByName(name).isPresent())  throw new IllegalArgumentException("a product called '" + name + "' already exists");
		Product product = new Product(name, priceMinorUnits, stockQuantity, lowStockThreshold);
		return repository.save(product);
	}

	public Product update(Long id, String name, int priceMinorUnits, int lowStockThreshold) {
		Product existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("no product with id " + id));
		validate(name, priceMinorUnits, existing.getStockQuantity(), lowStockThreshold);
		Optional<Product> byName = repository.findByName(name);
		if (byName.isPresent() && !byName.get().getId().equals(id)) {
			throw new IllegalArgumentException("a product called '" + name + "' already exists");
		}
		existing.setName(name);
		existing.setPriceMinorUnits(priceMinorUnits);
		existing.setLowStockThreshold(lowStockThreshold);
		return repository.save(existing);
	}
	private void validate(String name, int priceMinorUnits, int stockQuantity, int lowStockThreshold) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
		if (priceMinorUnits < 0) throw new IllegalArgumentException("price cannot be negative");
		if (stockQuantity < 0) throw new IllegalArgumentException("stock cannot be negative");
		if (lowStockThreshold < 0) throw new IllegalArgumentException("threshold cannot be negative");
	}
}