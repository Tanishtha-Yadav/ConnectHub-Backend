package com.connecthub.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // RestTemplate bean is defined in RestTemplateConfig (with @LoadBalanced for Eureka)
}
