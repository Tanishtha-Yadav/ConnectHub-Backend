package com.connecthub.auth.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

/**
 * Configuration for security headers needed for OAuth2 and cross-origin communication.
 *
 * WHY only COOP and NOT COEP?
 *   - COOP "same-origin-allow-popups" lets the parent page receive postMessage
 *     callbacks from the Google Sign-In popup window.
 *   - COEP "require-corp" must NOT be set: Google's accounts.google.com resources
 *     do not send Cross-Origin-Resource-Policy headers, so COEP would block them
 *     and cause the "postMessage blocked" error in the browser console.
 */
@Configuration
public class CookieSameSiteConfig {

    @Bean
    public Filter cookieSameSiteFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                // Required for Google OAuth popup to postMessage back to the opener.
                // "same-origin-allow-popups" allows popups (accounts.google.com) to
                // communicate with this page via postMessage without being blocked.
                httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");

                // DO NOT set Cross-Origin-Embedder-Policy here — it breaks Google Sign-In.

                chain.doFilter(request, response);
            }
        };
    }
}
