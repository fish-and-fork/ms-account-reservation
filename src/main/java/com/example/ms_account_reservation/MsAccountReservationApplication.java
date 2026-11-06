package com.example.ms_account_reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAccountReservationApplication {

	static void main(String[] args) {
		SpringApplication.run(MsAccountReservationApplication.class, args);
	}
}
