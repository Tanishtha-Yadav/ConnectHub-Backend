package com.connecthub.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * API Gateway Application.
 *
 * WHY @LoadBalanced on WebClient.Builder?
 *   The AuthenticationFilter calls auth-service by service name (not IP).
 *   @LoadBalanced tells Spring Cloud to resolve "auth-service" via Eureka
 *   and load-balance across instances automatically.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	public WebClient webClient(@LoadBalanced WebClient.Builder builder) {
		return builder.build();
	}
}

