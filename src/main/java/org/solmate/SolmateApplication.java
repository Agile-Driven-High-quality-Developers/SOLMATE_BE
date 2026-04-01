package org.solmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SolmateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolmateApplication.class, args);
	}

}
