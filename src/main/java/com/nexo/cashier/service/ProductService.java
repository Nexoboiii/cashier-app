package com.nexo.cashier.service;

import com.nexo.cashier.persistence.Product;
import com.nexo.cashier.persistence.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Service
public class ProductService {

	private final ProductRepository repository;

	public ProductService(ProductRepository repository) {
		this.repository = repository;
	}

	public List<Product> findAll() {
		return repository.findAll();
	}

	public Product create(String name, int priceMinorUnits, int stockQuantity, int lowStockThreshold) {
		validate(name, priceMinorUnits, stockQuantity, lowStockThreshold);
		if (repository.findByName(name).isPresent())  throw new IllegalArgumentException("a product called '" + name + "' already exists");
		Product product = new Product(name, priceMinorUnits, stockQuantity, lowStockThreshold);
		return repository.save(product);
	}

	public Product update(Long id, String name, int priceMinorUnits, int lowStockThreshold) {
		Product existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("no product with id " + id));
		validate(name, priceMinorUnits, existing.getStockQuantity(), lowStockThreshold);
		Optional<Product> byName = repository.findByName(name);
		if (byName.isPresent() && !byName.get().getId().equals(id)) {
			throw new IllegalArgumentException("a product called '" + name + "' already exists");
		}
		existing.setName(name);
		existing.setPriceMinorUnits(priceMinorUnits);
		existing.setLowStockThreshold(lowStockThreshold);
		return repository.save(existing);
	}

	public ImportResult importCsv(InputStream in) {
		int created = 0;
		int updated = 0;
		List<ImportResult.RowError> errors = new ArrayList<>();

		try (CSVReader reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

			String[] header = reader.readNext();
			if (header == null) throw new IllegalArgumentException("the file is empty");
			if (header.length > 0) header[0] = header[0].replace("\uFEFF", "").trim();
			checkHeader(header);

			String[] row;
			int line = 1;
			while ((row = reader.readNext()) != null) {
				line++;
				if (row.length == 1 && row[0].isBlank()) continue;
				try {
					if (upsertRow(row)) created++; else updated++;
				} catch (Exception e) {
					errors.add(new ImportResult.RowError(line, e.getMessage()));
				}
			}
		} catch (IOException | CsvValidationException e) {
			throw new IllegalArgumentException("could not read the file: " + e.getMessage());
		}

		return new ImportResult(created, updated, errors.size(), errors);
	}
	public String exportCsv() {
		StringWriter out = new StringWriter();
		try (CSVWriter writer = new CSVWriter(out)) {
			writer.writeNext(new String[] { "name", "price", "stock", "lowStockThreshold" }, false);
			for (Product p : repository.findAll()) {
				writer.writeNext(new String[] {
						p.getName(),
						String.valueOf(p.getPriceMinorUnits()),
						String.valueOf(p.getStockQuantity()),
						String.valueOf(p.getLowStockThreshold())
				}, false);
			}
		} catch (IOException e) {
			throw new IllegalStateException("could not write the csv", e);
		}
		// BOM so excel opens it as utf-8
		return "\uFEFF" + out.toString();
	}

	private void checkHeader(String[] header) {
		String[] expected = { "name", "price", "stock", "lowStockThreshold" };
		if (header.length < expected.length) {
			throw new IllegalArgumentException("expected columns: name,price,stock,lowStockThreshold");
		}
		for (int i = 0; i < expected.length; i++) {
			if (!header[i].trim().equalsIgnoreCase(expected[i])) {
				throw new IllegalArgumentException(
						"column " + (i + 1) + " should be '" + expected[i] + "' but was '" + header[i].trim() + "'");
			}
		}
	}

	// true = created, false = updated
	private boolean upsertRow(String[] row) {
		if (row.length < 4) throw new IllegalArgumentException("expected 4 columns, found " + row.length);

		String name = row[0].trim();
		int price = parseWholeNumber(row[1], "price");
		int stock = parseWholeNumber(row[2], "stock");
		int threshold = parseWholeNumber(row[3], "lowStockThreshold");

		validate(name, price, stock, threshold);

		Optional<Product> existing = repository.findByName(name);
		if (existing.isPresent()) {
			Product p = existing.get();
			p.setPriceMinorUnits(price);
			p.setLowStockThreshold(threshold);
			// stock deliberately not touched - see note
			repository.save(p);
			return false;
		}
		repository.save(new Product(name, price, stock, threshold));
		return true;
	}

	private int parseWholeNumber(String raw, String field) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(field + " must be a whole number, was '" + raw.trim() + "'");
		}
	}
	private void validate(String name, int priceMinorUnits, int stockQuantity, int lowStockThreshold) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
		if (priceMinorUnits < 0) throw new IllegalArgumentException("price cannot be negative");
		if (stockQuantity < 0) throw new IllegalArgumentException("stock cannot be negative");
		if (lowStockThreshold < 0) throw new IllegalArgumentException("threshold cannot be negative");
	}
}