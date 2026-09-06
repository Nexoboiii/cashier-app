package com.nexo.cashier.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// unique: the CSV importer upserts on name
	@Column(nullable = false, unique = true, length = 200)
	private String name;

	// whole rupees
	@Column(nullable = false)
	private int priceMinorUnits;

	@Column(nullable = false)
	private int stockQuantity;

	@Column(nullable = false)
	private int lowStockThreshold;

	@Column(nullable = false)
	private boolean active = true;

	protected Product() {
		// jpa
	}

	public Product(String name, int priceMinorUnits, int stockQuantity, int lowStockThreshold) {
		this.name = name;
		this.priceMinorUnits = priceMinorUnits;
		this.stockQuantity = stockQuantity;
		this.lowStockThreshold = lowStockThreshold;
	}

	public Long getId() { return id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public int getPriceMinorUnits() { return priceMinorUnits; }
	public void setPriceMinorUnits(int priceMinorUnits) { this.priceMinorUnits = priceMinorUnits; }

	public int getStockQuantity() { return stockQuantity; }
	public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

	public int getLowStockThreshold() { return lowStockThreshold; }
	public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
}