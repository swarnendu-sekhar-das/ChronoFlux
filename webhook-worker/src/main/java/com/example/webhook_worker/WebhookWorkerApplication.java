package com.example.webhook_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableRetry
public class WebhookWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookWorkerApplication.class, args);
	}

	@Bean
	public WebClient webClient() {
		return WebClient.create();
	}
}
