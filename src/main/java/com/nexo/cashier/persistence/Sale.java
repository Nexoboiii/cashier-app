package com.nexo.cashier.persistence;

import com.nexo.cashier.model.PaymentMethod;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale")
public class Sale {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant timestamp;

	@Column(nullable = false)
	private int totalMinorUnits;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private PaymentMethod paymentMethod;

	// null on card sales
	private Integer cashTenderedMinorUnits;
	private Integer changeGivenMinorUnits;

	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SaleLineItem> lines = new ArrayList<>();

	protected Sale() {
		// jpa
	}

	public Sale(Instant timestamp, PaymentMethod paymentMethod) {
		this.timestamp = timestamp;
		this.paymentMethod = paymentMethod;
	}

	// sets both sides - jpa will not do it for you
	public void addLine(SaleLineItem line) {
		lines.add(line);
		line.setSale(this);
	}

	public Long getId() { return id; }
	public Instant getTimestamp() { return timestamp; }
	public List<SaleLineItem> getLines() { return lines; }

	public int getTotalMinorUnits() { return totalMinorUnits; }
	public void setTotalMinorUnits(int totalMinorUnits) { this.totalMinorUnits = totalMinorUnits; }

	public PaymentMethod getPaymentMethod() { return paymentMethod; }

	public Integer getCashTenderedMinorUnits() { return cashTenderedMinorUnits; }
	public void setCashTenderedMinorUnits(Integer v) { this.cashTenderedMinorUnits = v; }

	public Integer getChangeGivenMinorUnits() { return changeGivenMinorUnits; }
	public void setChangeGivenMinorUnits(Integer v) { this.changeGivenMinorUnits = v; }
}