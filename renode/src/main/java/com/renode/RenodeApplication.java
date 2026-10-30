package com.renode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RenodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RenodeApplication.class, args);
	}

}
