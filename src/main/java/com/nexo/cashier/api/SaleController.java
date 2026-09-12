package com.nexo.cashier.api;

import com.nexo.cashier.model.PaymentMethod;
import com.nexo.cashier.persistence.Sale;
import com.nexo.cashier.persistence.SaleLineItem;
import com.nexo.cashier.service.SaleLine;
import com.nexo.cashier.service.SaleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

	private final SaleService service;

	public SaleController(SaleService service) {
		this.service = service;
	}

	public record LineRequest(Long productId, int quantity) {}

	public record CreateSaleRequest(List<LineRequest> lines,
									PaymentMethod paymentMethod,
									Integer cashTenderedMinorUnits) {}

	public record LineResponse(String name, int unitPrice, int quantity, int lineTotal) {
		static LineResponse from(SaleLineItem l) {
			return new LineResponse(
					l.getProductNameAtSale(), l.getUnitPriceAtSale(), l.getQuantity(), l.getLineTotal());
		}
	}

	public record SaleResponse(Long id, Instant timestamp, int totalMinorUnits,
							   PaymentMethod paymentMethod,
							   Integer cashTenderedMinorUnits, Integer changeGivenMinorUnits,
							   List<LineResponse> lines) {
		static SaleResponse from(Sale s) {
			return new SaleResponse(
					s.getId(), s.getTimestamp(), s.getTotalMinorUnits(), s.getPaymentMethod(),
					s.getCashTenderedMinorUnits(), s.getChangeGivenMinorUnits(),
					s.getLines().stream().map(LineResponse::from).toList());
		}
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SaleResponse create(@RequestBody CreateSaleRequest req) {
		if (req.lines() == null) throw new IllegalArgumentException("a sale needs at least one line");

		List<SaleLine> lines = req.lines().stream()
				.map(l -> new SaleLine(l.productId(), l.quantity()))
				.toList();

		Sale sale = service.createSale(lines, req.paymentMethod(), req.cashTenderedMinorUnits());
		return SaleResponse.from(sale);
	}
}