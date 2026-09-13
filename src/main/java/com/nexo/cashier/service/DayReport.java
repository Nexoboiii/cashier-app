package com.nexo.cashier.service;

import java.time.Instant;
import java.util.List;

public record DayReport(
		Long dayId,
		Instant openedAt,
		Instant closedAt,
		int openingFloatMinorUnits,

		int saleCount,
		int totalTakingsMinorUnits,
		int cashTakingsMinorUnits,
		int cardTakingsMinorUnits,
		int averageSaleMinorUnits,

		int cashTenderedMinorUnits,
		int changeGivenMinorUnits,
		int expectedCashMinorUnits,
		Integer countedCashMinorUnits,
		Integer varianceMinorUnits,
		String closeNote,

		List<ItemLine> items,
		List<AuditLine> exceptions,
		List<SupplierLine> suppliers) {

	public record ItemLine(String name, int units, int revenueMinorUnits) {}

	public record AuditLine(Instant at, String type, Long amountMinorUnits, String detail) {}

	public record SupplierLine(String supplier, int units, int revenueMinorUnits) {}
}