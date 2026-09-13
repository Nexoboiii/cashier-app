package com.nexo.cashier.api;

import com.nexo.cashier.persistence.TillDay;
import com.nexo.cashier.service.BackupService;
import com.nexo.cashier.service.DayReport;
import com.nexo.cashier.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/days")
public class DayController {

	private final ReportService service;
	private final BackupService backups;

	public DayController(ReportService service, BackupService backups) {
		this.service = service;
		this.backups = backups;
	}

	public record OpenRequest(int openingFloatMinorUnits) {}

	public record CloseRequest(int countedCashMinorUnits, String note) {}

	public record DayResponse(Long id, Instant openedAt, int openingFloatMinorUnits,
							  Instant closedAt, Integer expectedCashMinorUnits,
							  Integer countedCashMinorUnits, Integer varianceMinorUnits,
							  String closeNote, boolean open) {
		static DayResponse from(TillDay d) {
			return new DayResponse(d.getId(), d.getOpenedAt(), d.getOpeningFloatMinorUnits(),
					d.getClosedAt(), d.getExpectedCashMinorUnits(), d.getCountedCashMinorUnits(),
					d.getVarianceMinorUnits(), d.getCloseNote(), d.isOpen());
		}
	}

	@PostMapping("/open")
	@ResponseStatus(HttpStatus.CREATED)
	public DayResponse open(@RequestBody OpenRequest req) {
		return DayResponse.from(service.openDay(req.openingFloatMinorUnits()));
	}

	@PostMapping("/close")
	public DayResponse close(@RequestBody CloseRequest req) {
		DayResponse day = DayResponse.from(service.closeDay(req.countedCashMinorUnits(), req.note()));
		backups.backup("day-close");   // after the tx commits, or the backup misses the close
		return day;
	}

	// 204 when nothing is open - the till uses this to decide whether to show the open screen
	@GetMapping("/current")
	public ResponseEntity<DayResponse> current() {
		return service.currentDay()
				.map(d -> ResponseEntity.ok(DayResponse.from(d)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/current/report")
	public DayReport currentReport() {
		return service.currentReport();
	}

	@GetMapping("/{id}/report")
	public DayReport report(@PathVariable Long id) {
		return service.report(id);
	}

	@GetMapping
	public List<DayResponse> history() {
		return service.history().stream().map(DayResponse::from).toList();
	}
}