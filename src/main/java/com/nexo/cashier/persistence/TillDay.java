package com.nexo.cashier.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "till_day")
public class TillDay {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant openedAt;

	@Column(nullable = false)
	private int openingFloatMinorUnits;

	// all null while the day is open
	private Instant closedAt;
	private Integer expectedCashMinorUnits;
	private Integer countedCashMinorUnits;
	private Integer varianceMinorUnits;

	@Column(length = 500)
	private String closeNote;

	protected TillDay() {
		// jpa
	}

	public TillDay(Instant openedAt, int openingFloatMinorUnits) {
		this.openedAt = openedAt;
		this.openingFloatMinorUnits = openingFloatMinorUnits;
	}

	// one way - a closed day never reopens
	public void close(Instant at, int expected, int counted, String note) {
		if (closedAt != null) throw new IllegalStateException("day " + id + " is already closed");
		this.closedAt = at;
		this.expectedCashMinorUnits = expected;
		this.countedCashMinorUnits = counted;
		this.varianceMinorUnits = counted - expected;
		this.closeNote = note;
	}

	public boolean isOpen() { return closedAt == null; }

	public Long getId() { return id; }
	public Instant getOpenedAt() { return openedAt; }
	public int getOpeningFloatMinorUnits() { return openingFloatMinorUnits; }
	public Instant getClosedAt() { return closedAt; }
	public Integer getExpectedCashMinorUnits() { return expectedCashMinorUnits; }
	public Integer getCountedCashMinorUnits() { return countedCashMinorUnits; }
	public Integer getVarianceMinorUnits() { return varianceMinorUnits; }
	public String getCloseNote() { return closeNote; }
}