package com.nexo.cashier.service;

import java.util.List;

public record ImportResult(int created, int updated, int skipped, List<RowError> errors) {

	public record RowError(int line, String reason) {}
}