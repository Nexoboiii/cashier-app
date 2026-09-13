package com.nexo.cashier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CashierApplication {

	public static void main(String[] args) {
		SpringApplication.run(CashierApplication.class, args);
	}

}
