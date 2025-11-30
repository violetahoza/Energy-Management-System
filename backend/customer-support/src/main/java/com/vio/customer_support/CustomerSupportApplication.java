package com.vio.customer_support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CustomerSupportApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerSupportApplication.class, args);
	}

}
