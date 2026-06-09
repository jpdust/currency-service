package com.unstampedpages.currencyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CurrencyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CurrencyServiceApplication.class, args);
	}
}
