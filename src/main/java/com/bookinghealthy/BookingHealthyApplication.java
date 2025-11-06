package com.bookinghealthy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication
public class BookingHealthyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingHealthyApplication.class, args);
	}

}
