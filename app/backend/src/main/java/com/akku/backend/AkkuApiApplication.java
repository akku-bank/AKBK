package com.akku.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class AkkuApiApplication {

	public static void main(String[] args) {

		SpringApplication.run(AkkuApiApplication.class, args);
	}

}
