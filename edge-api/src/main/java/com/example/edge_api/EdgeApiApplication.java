package com.example.edge_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EdgeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EdgeApiApplication.class, args);
	}

}
