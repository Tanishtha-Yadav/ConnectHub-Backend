package com.connecthub.media.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret:ConnectHub_JWT_Secret_Key_2026!@#$%^&*_SuperSecure_abcdefghijklmnopqrstuvwxyz_ABCDEF}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Always permit CORS preflight requests — no JWT needed for OPTIONS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/media/health").permitAll()
                .requestMatchers("/api/media/files/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Admin endpoints
                .requestMatchers("/api/media/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // Spring Security 6: unauthenticated requests return 403 by default for
            // anonymous users hitting .anyRequest().authenticated(). This forces 401.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            .addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private class JwtAuthFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                    io.jsonwebtoken.Claims claims = Jwts.parser().verifyWith(key).build()
                            .parseSignedClaims(token).getPayload();
                    String userId = claims.getSubject();
                    String role = (String) claims.get("role");

                    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    if (role != null) {
                        if (role.startsWith("ROLE_")) {
                            authorities.add(new SimpleGrantedAuthority(role));
                        } else {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, token, authorities));
                } catch (Exception e) {
                    System.err.println("Media Service JWT validation failed: " + e.getMessage());
                    e.printStackTrace();
                    // Token is present but invalid — return 401 immediately.
                    // Do NOT just clear context: Spring Security 6 returns 403 for
                    // anonymous users hitting .authenticated() endpoints.
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
