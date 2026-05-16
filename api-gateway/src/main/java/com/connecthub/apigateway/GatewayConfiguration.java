package com.connecthub.apigateway;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {
    // CORS is handled globally via application.yml globalcors configuration
    // This prevents duplicate CORS headers when both Java bean and YAML config are present
}
