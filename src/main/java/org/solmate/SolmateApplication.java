package org.solmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SolmateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolmateApplication.class, args);
	}

}
