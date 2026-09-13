package com.nexo.cashier.api;

import com.nexo.cashier.model.AdjustmentReason;
import com.nexo.cashier.persistence.StockAdjustment;
import com.nexo.cashier.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

import static com.nexo.cashier.api.ProductController.ProductResponse;

@RestController
public class InventoryController {

	private final InventoryService service;

	public InventoryController(InventoryService service) {
		this.service = service;
	}

	public record AdjustRequest(int quantity, String note) {}

	public record CorrectRequest(int countedStock, String note) {}

	public record AdjustmentResponse(Long id, Instant timestamp, Long productId,
									 int quantityChange, AdjustmentReason reason, String note) {

		static AdjustmentResponse from(StockAdjustment a) {
			return new AdjustmentResponse(a.getId(), a.getTimestamp(), a.getProductId(),
					a.getQuantityChange(), a.getReason(), a.getNote());
		}
	}

	@GetMapping("/api/products/low-stock")
	public List<ProductResponse> lowStock() {
		return service.lowStock().stream().map(ProductResponse::from).toList();
	}

	@PostMapping("/api/products/{id}/restock")
	public ProductResponse restock(@PathVariable Long id, @RequestBody AdjustRequest req) {
		return ProductResponse.from(service.restock(id, req.quantity(), req.note()));
	}

	@PostMapping("/api/products/{id}/damage")
	public ProductResponse damage(@PathVariable Long id, @RequestBody AdjustRequest req) {
		return ProductResponse.from(service.damage(id, req.quantity(), req.note()));
	}

	@PostMapping("/api/products/{id}/correct")
	public ProductResponse correct(@PathVariable Long id, @RequestBody CorrectRequest req) {
		return ProductResponse.from(service.correctTo(id, req.countedStock(), req.note()));
	}

	@GetMapping("/api/products/{id}/adjustments")
	public List<AdjustmentResponse> history(@PathVariable Long id) {
		return service.history(id).stream().map(AdjustmentResponse::from).toList();
	}

	@GetMapping("/api/inventory/adjustments")
	public List<AdjustmentResponse> allHistory() {
		return service.history().stream().map(AdjustmentResponse::from).toList();
	}
}