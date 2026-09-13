package com.nexo.cashier.service;

import com.nexo.cashier.model.AuditEventType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class BackupService {

	private static final Logger log = LoggerFactory.getLogger(BackupService.class);

	// sorts chronologically as text - the purge depends on this
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final String PREFIX = "cashier-backup-";

	private final JdbcTemplate jdbc;
	private final AuditService audit;
	private final String dir;
	private final int keep;

	public BackupService(JdbcTemplate jdbc, AuditService audit,
						 @Value("${cashier.backup.dir:backups}") String dir,
						 @Value("${cashier.backup.keep:20}") int keep) {
		this.jdbc = jdbc;
		this.audit = audit;
		this.dir = dir.replace('\\', '/');
		this.keep = keep;
	}

	// never throws - a failed backup must not break whatever triggered it
	public Path backup(String reason) {
		String name = PREFIX + LocalDateTime.now().format(STAMP) + "-" + safe(reason) + ".zip";
		Path target = Path.of(dir, name);
		try {
			Files.createDirectories(Path.of(dir));

			// h2 command, not a query - the argument is a literal, so quotes are escaped
			jdbc.execute("BACKUP TO '" + (dir + "/" + name).replace("'", "''") + "'");

			long size = Files.size(target);
			log.info("backup written: {} ({} bytes)", target, size);
			audit.record(AuditEventType.BACKUP_CREATED, "BACKUP", null, null, name + ", " + size + " bytes");
			purge();
			return target;
		} catch (Exception e) {
			log.error("backup failed ({})", reason, e);
			return null;
		}
	}

	@Scheduled(cron = "59 59 23 * * *")
	public void nightly() {
		backup("nightly");
	}

	// runs before the pool closes - spring destroys dependents first
	@PreDestroy
	public void onShutdown() {
		backup("shutdown");
	}

	private void purge() {
		if (keep <= 0) return;
		try (Stream<Path> files = Files.list(Path.of(dir))) {
			List<Path> stale = files
					.filter(p -> p.getFileName().toString().startsWith(PREFIX))
					.filter(p -> p.getFileName().toString().endsWith(".zip"))
					.sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
					.skip(keep)
					.toList();
			for (Path p : stale) {
				Files.delete(p);
				log.info("purged old backup {}", p.getFileName());
			}
		} catch (Exception e) {
			log.warn("could not purge old backups", e);
		}
	}

	private static String safe(String reason) {
		if (reason == null || reason.isBlank()) return "manual";
		return reason.toLowerCase().replaceAll("[^a-z0-9-]", "-");
	}
}