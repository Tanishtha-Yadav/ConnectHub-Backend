package com.connecthub.apigateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Authentication Filter for API Gateway
 * Validates JWT tokens for protected endpoints
 * Forwards valid tokens to downstream services
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient webClient;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_VALIDATION_ENDPOINT = "http://auth-service/api/auth/validate";

    public AuthenticationFilter(@Autowired WebClient webClient) {
        super(Config.class);
        this.webClient = webClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            System.out.println("Auth Filter: Processing request " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getURI());
            // Check if Authorization header exists
            if (!hasAuthorizationHeader(exchange)) {
                System.out.println("Auth Filter: Missing Authorization Header for " + exchange.getRequest().getURI());
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            // Extract token from header
            String token = extractToken(exchange);
            if (token == null) {
                System.out.println("Auth Filter: Missing or invalid token format for " + exchange.getRequest().getURI());
                return onError(exchange, "Invalid Authorization Header Format", HttpStatus.UNAUTHORIZED);
            }

            // Validate token with auth-service
            return validateTokenWithAuthService(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        // Token is valid, add it to the downstream request
                        ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                                .build())
                            .build();
                        return chain.filter(modifiedExchange);
                    } else {
                        System.out.println("Auth Filter: Token validation failed for " + exchange.getRequest().getURI());
                        return onError(exchange, "Invalid or Expired Token", HttpStatus.UNAUTHORIZED);
                    }
                })
                .onErrorResume(throwable -> {
                    System.err.println("Auth Filter: Error during validation for " + exchange.getRequest().getURI() + ": " + throwable.getMessage());
                    return onError(exchange, "Token Validation Failed: " + throwable.getMessage(), 
                        HttpStatus.UNAUTHORIZED);
                });
        };
    }

    /**
     * Check if Authorization header exists
     */
    private boolean hasAuthorizationHeader(ServerWebExchange exchange) {
        return exchange.getRequest()
            .getHeaders()
            .containsKey(AUTHORIZATION_HEADER);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Validate token by calling auth-service
     */
    private Mono<Boolean> validateTokenWithAuthService(String token) {
        return webClient.get()
            .uri(TOKEN_VALIDATION_ENDPOINT)
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(response -> {
                Object valid = response.get("valid");
                return valid instanceof Boolean && (Boolean) valid;
            })
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> {
                System.err.println("Auth Filter: Validation request failed for token: " + e.getMessage());
                return Mono.just(false);
            });
    }

    /**
     * Handle authentication error
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse()
            .getHeaders()
            .add(HttpHeaders.CONTENT_TYPE, "application/json");

        String errorBody = String.format(
            "{\"error\": \"%s\", \"status\": %d, \"timestamp\": \"%s\"}",
            message, httpStatus.value(), java.time.Instant.now()
        );

        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(errorBody.getBytes())));
    }

    /**
     * Configuration class
     */
    public static class Config {
        // Add any configuration needed for the filter
    }

    /**
     * Token Validation Response DTO
     */
    private static class TokenValidationResponse {
        private boolean valid;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
