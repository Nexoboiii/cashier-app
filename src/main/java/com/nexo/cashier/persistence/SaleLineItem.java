package com.nexo.cashier.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "sale_line_item")
public class SaleLineItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "sale_id", nullable = false)
	private Sale sale;

	// plain id, not a relation - see note
	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false, length = 200)
	private String productNameAtSale;

	@Column(nullable = false)
	private int unitPriceAtSale;

	// snapshot - reassigning the product must not re-attribute past sales
	@Column(length = 120)
	private String supplierAtSale;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private int lineTotal;

	protected SaleLineItem() {
		// jpa
	}

	public SaleLineItem(Long productId, String productNameAtSale, int unitPriceAtSale,
						int quantity, String supplierAtSale) {
		this.productId = productId;
		this.productNameAtSale = productNameAtSale;
		this.unitPriceAtSale = unitPriceAtSale;
		this.quantity = quantity;
		this.lineTotal = unitPriceAtSale * quantity;
		this.supplierAtSale = supplierAtSale;
	}

	public Long getId() { return id; }
	public Long getProductId() { return productId; }
	public String getProductNameAtSale() { return productNameAtSale; }
	public int getUnitPriceAtSale() { return unitPriceAtSale; }
	public int getQuantity() { return quantity; }
	public int getLineTotal() { return lineTotal; }
	public String getSupplierAtSale() { return supplierAtSale; }

	Sale getSale() { return sale; }
	void setSale(Sale sale) { this.sale = sale; }
}