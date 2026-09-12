package com.nexo.cashier.persistence;

import com.nexo.cashier.model.AdjustmentReason;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stock_adjustment")
public class StockAdjustment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant timestamp;

	@Column(nullable = false)
	private Long productId;

	// the delta, not the new total - negative for damage and downward corrections
	@Column(nullable = false)
	private int quantityChange;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private AdjustmentReason reason;

	@Column(length = 200)
	private String note;

	protected StockAdjustment() {
		// jpa
	}

	public StockAdjustment(Long productId, int quantityChange, AdjustmentReason reason, String note) {
		this.timestamp = Instant.now();
		this.productId = productId;
		this.quantityChange = quantityChange;
		this.reason = reason;
		this.note = note;
	}

	public Long getId() { return id; }
	public Instant getTimestamp() { return timestamp; }
	public Long getProductId() { return productId; }
	public int getQuantityChange() { return quantityChange; }
	public AdjustmentReason getReason() { return reason; }
	public String getNote() { return note; }
}