package com.nexo.cashier.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

	private static final Logger log = LoggerFactory.getLogger(SystemController.class);

	private final ApplicationContext context;

	public SystemController(ApplicationContext context) {
		this.context = context;
	}

	@PostMapping("/shutdown")
	public Map<String, String> shutdown() {
		log.info("shutdown requested");
		// answer first, then exit - otherwise the caller just sees a dropped connection
		Thread stopper = new Thread(() -> {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			SpringApplication.exit(context, () -> 0);
			System.exit(0);
		});
		stopper.setDaemon(false);
		stopper.start();
		return Map.of("status", "stopping");
	}
}