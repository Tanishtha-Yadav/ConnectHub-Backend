package com.connecthub.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * OAuth2 Configuration for Google OAuth2 Authentication.
 * Configures the OAuth2 client manager for handling Google login flows.
 */
@Configuration
public class OAuth2Config {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Config.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:}")
    private String googleRedirectUri;

    /**
     * Configure OAuth2 Authorized Client Manager.
     * Used for server-side OAuth2 token requests and management.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .clientCredentials()
                        .build();

        DefaultOAuth2AuthorizedClientManager clientManager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        clientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return clientManager;
    }

    /**
     * Validates Google OAuth2 config on startup.
     * Logs a warning instead of crashing if credentials are missing,
     * so the app can still start for email/password auth.
     */
    @Bean
    public OAuth2ValidationBean oauth2ValidationBean() {
        return new OAuth2ValidationBean(googleClientId, googleClientSecret, googleRedirectUri);
    }

    public static class OAuth2ValidationBean {

        public OAuth2ValidationBean(String clientId, String clientSecret, String redirectUri) {
            Logger logger = LoggerFactory.getLogger(OAuth2ValidationBean.class);
            if (clientId == null || clientId.isBlank()) {
                logger.warn("⚠️  Google OAuth2 Client ID not configured — Google Sign-In will be unavailable.");
            } else if (clientSecret == null || clientSecret.isBlank()) {
                logger.warn("⚠️  Google OAuth2 Client Secret not configured — Google Sign-In will be unavailable.");
            } else {
                logger.info("✅  Google OAuth2 configured. Client ID: {}...{}", 
                        clientId.substring(0, Math.min(12, clientId.length())),
                        clientId.substring(Math.max(0, clientId.length() - 6)));
            }
        }
    }
}
