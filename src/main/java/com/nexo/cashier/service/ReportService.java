package com.nexo.cashier.service;

import com.nexo.cashier.model.AuditEventType;
import com.nexo.cashier.model.PaymentMethod;
import com.nexo.cashier.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ReportService {

	private static final String OWN_STOCK = "Own stock";

	private final TillDayRepository days;
	private final SaleRepository sales;
	private final AuditRepository audits;
	private final AuditService audit;

	public ReportService(TillDayRepository days, SaleRepository sales,
						 AuditRepository audits, AuditService audit) {
		this.days = days;
		this.sales = sales;
		this.audits = audits;
		this.audit = audit;
	}

	public Optional<TillDay> currentDay() {
		return days.findFirstByClosedAtIsNullOrderByOpenedAtDesc();
	}

	public List<TillDay> history() {
		return days.findAllByOrderByOpenedAtDesc();
	}

	@Transactional
	public TillDay openDay(int openingFloatMinorUnits) {
		if (openingFloatMinorUnits < 0) {
			throw new IllegalArgumentException("opening float cannot be negative");
		}
		currentDay().ifPresent(d -> {
			throw new IllegalArgumentException("day " + d.getId() + " is still open - close it first");
		});

		TillDay day = days.save(new TillDay(Instant.now(), openingFloatMinorUnits));
		audit.record(AuditEventType.DAY_OPENED, "DAY", day.getId(),
				(long) openingFloatMinorUnits, "opening float set");
		return day;
	}

	@Transactional
	public TillDay closeDay(int countedCashMinorUnits, String note) {
		if (countedCashMinorUnits < 0) {
			throw new IllegalArgumentException("counted cash cannot be negative");
		}
		TillDay day = currentDay().orElseThrow(
				() -> new IllegalArgumentException("no day is open"));

		Instant closingAt = Instant.now();
		DayReport report = build(day, closingAt);

		day.close(closingAt, report.expectedCashMinorUnits(), countedCashMinorUnits, note);
		days.save(day);

		audit.record(AuditEventType.DAY_CLOSED, "DAY", day.getId(),
				(long) day.getVarianceMinorUnits(),
				"expected " + report.expectedCashMinorUnits()
						+ ", counted " + countedCashMinorUnits);
		return day;
	}

	@Transactional(readOnly = true)
	public DayReport report(Long dayId) {
		TillDay day = days.findById(dayId).orElseThrow(
				() -> new IllegalArgumentException("no day with id " + dayId));
		return build(day, day.isOpen() ? Instant.now() : day.getClosedAt());
	}

	@Transactional(readOnly = true)
	public DayReport currentReport() {
		TillDay day = currentDay().orElseThrow(
				() -> new IllegalArgumentException("no day is open"));
		return build(day, Instant.now());
	}

	// units and revenue always move together - one object so they cannot drift
	private static final class Tally {
		int units;
		int revenue;

		void add(int quantity, int amount) {
			units += quantity;
			revenue += amount;
		}
	}

	private DayReport build(TillDay day, Instant upTo) {
		List<Sale> daySales = sales.findByTimestampBetweenOrderByTimestampAsc(day.getOpenedAt(), upTo);

		int total = 0, cash = 0, card = 0, tendered = 0, change = 0;

		Map<String, Tally> byItem = new HashMap<>();
		Map<String, Tally> bySupplier = new HashMap<>();
		Map<String, Map<String, Tally>> bySupplierItem = new HashMap<>();

		for (Sale s : daySales) {
			total += s.getTotalMinorUnits();
			if (s.getPaymentMethod() == PaymentMethod.CASH) {
				cash += s.getTotalMinorUnits();
				tendered += s.getCashTenderedMinorUnits() == null ? 0 : s.getCashTenderedMinorUnits();
				change += s.getChangeGivenMinorUnits() == null ? 0 : s.getChangeGivenMinorUnits();
			} else {
				card += s.getTotalMinorUnits();
			}

			for (SaleLineItem l : s.getLines()) {
				String name = l.getProductNameAtSale();
				String who = l.getSupplierAtSale() == null ? OWN_STOCK : l.getSupplierAtSale();

				byItem.computeIfAbsent(name, k -> new Tally()).add(l.getQuantity(), l.getLineTotal());
				bySupplier.computeIfAbsent(who, k -> new Tally()).add(l.getQuantity(), l.getLineTotal());
				bySupplierItem
						.computeIfAbsent(who, k -> new HashMap<>())
						.computeIfAbsent(name, k -> new Tally())
						.add(l.getQuantity(), l.getLineTotal());
			}
		}

		List<DayReport.ItemLine> items = itemLines(byItem);

		List<DayReport.SupplierLine> suppliers = bySupplier.entrySet().stream()
				.map(e -> new DayReport.SupplierLine(
						e.getKey(), e.getValue().units, e.getValue().revenue,
						itemLines(bySupplierItem.getOrDefault(e.getKey(), Map.of()))))
				.sorted(Comparator.comparingInt(DayReport.SupplierLine::revenueMinorUnits).reversed()
						.thenComparing(DayReport.SupplierLine::supplier))
				.toList();

		List<DayReport.AuditLine> exceptions = audits
				.findByTimestampBetweenAndTypeNotOrderByTimestampAsc(
						day.getOpenedAt(), upTo, AuditEventType.SALE_COMPLETED)
				.stream()
				.map(a -> new DayReport.AuditLine(
						a.getTimestamp(), a.getType().name(), a.getAmountMinorUnits(), a.getDetail()))
				.toList();

		// notes in minus notes out - the physical movement, not the sale totals
		// a closed day keeps the figure its variance was signed off against
		int expected = day.isOpen()
				? day.getOpeningFloatMinorUnits() + tendered - change
				: day.getExpectedCashMinorUnits();

		// truncates - display only, never reconciled
		int average = daySales.isEmpty() ? 0 : total / daySales.size();

		return new DayReport(
				day.getId(), day.getOpenedAt(), day.getClosedAt(), day.getOpeningFloatMinorUnits(),
				daySales.size(), total, cash, card, average,
				tendered, change, expected,
				day.getCountedCashMinorUnits(), day.getVarianceMinorUnits(), day.getCloseNote(),
				items, exceptions, suppliers);
	}

	// biggest earner first, name as the tiebreak so the order is stable
	private static List<DayReport.ItemLine> itemLines(Map<String, Tally> tallies) {
		return tallies.entrySet().stream()
				.map(e -> new DayReport.ItemLine(e.getKey(), e.getValue().units, e.getValue().revenue))
				.sorted(Comparator.comparingInt(DayReport.ItemLine::revenueMinorUnits).reversed()
						.thenComparing(DayReport.ItemLine::name))
				.toList();
	}
}