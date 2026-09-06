package com.nexo.cashier.api;

import com.nexo.cashier.persistence.Product;
import com.nexo.cashier.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	public record CreateRequest(String name, int priceMinorUnits, int stockQuantity, int lowStockThreshold) {}

	public record UpdateRequest(String name, int priceMinorUnits, int lowStockThreshold) {}

	public record ProductResponse(Long id, String name, int priceMinorUnits,
								  int stockQuantity, int lowStockThreshold, boolean active) {

		static ProductResponse from(Product p) {
			return new ProductResponse(
					p.getId(),
					p.getName(),
					p.getPriceMinorUnits(),
					p.getStockQuantity(),
					p.getLowStockThreshold(),
					p.isActive());
		}
	}

	@GetMapping
	public List<ProductResponse> list() {
		return service.findAll().stream().map(ProductResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@RequestBody CreateRequest req) {
		Product saved = service.create(
				req.name(), req.priceMinorUnits(), req.stockQuantity(), req.lowStockThreshold());
		return ProductResponse.from(saved);
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable Long id, @RequestBody UpdateRequest req) {
		Product saved = service.update(id, req.name(), req.priceMinorUnits(), req.lowStockThreshold());
		return ProductResponse.from(saved);
	}
}