package com.nexo.cashier.service;

// unitPriceMinorUnits is null for everything except an open-price product
public record SaleLine(Long productId, int quantity, Integer unitPriceMinorUnits) {

	public SaleLine(Long productId, int quantity) {
		this(productId, quantity, null);
	}
}
