package com.connecthub.auth.controller;

import com.connecthub.auth.dto.request.CompleteRegistrationRequest;
import com.connecthub.auth.dto.response.LoginResponse;
import com.connecthub.auth.dto.response.RegistrationSetupResponse;
import com.connecthub.auth.exception.UserNotFoundException;
import com.connecthub.auth.service.OAuth2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Authentication Controller
 * Handles Google and GitHub OAuth2 login flows
 */
@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2Controller {

    @Autowired
    private OAuth2Service oauth2Service;

    /**
     * Google OAuth2 callback and token exchange
     * Expects: idToken (Google ID token) in request body
     */
    @PostMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestBody Map<String, String> request) {
        System.out.println("[OAuth2Controller - Callback] Received request. Keys: " + request.keySet());
        try {
            String idToken = extractIdToken(request);
            if (idToken == null || idToken.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Google ID token is required. Keys present: " + request.keySet());
                error.put("error", "missing_id_token");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            LoginResponse response = oauth2Service.authenticateWithGoogle(idToken);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("error", "email_not_registered");
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Google authentication failed: " + e.getMessage());
            error.put("error", "authentication_failed");
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Support browser redirect (authorization code) flow where Google redirects
     * to our callback with a `code` query parameter. Exchange the code for
     * an ID token, then proceed with the same server-side authentication.
     */
    @GetMapping("/google/callback")
    public ResponseEntity<?> googleCallbackGet(@RequestParam(value = "code", required = false) String code) {
        try {
            if (code == null || code.isBlank()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authorization code is required");
                error.put("error", "missing_code");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            // Exchange authorization code for ID token and authenticate
            String idToken = oauth2Service.exchangeCodeForIdToken(code);
            LoginResponse response = oauth2Service.authenticateWithGoogle(idToken);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("error", "email_not_registered");
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Google authentication failed: " + e.getMessage());
            error.put("error", "authentication_failed");
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Google OAuth2 registration
     * Creates new account with user's Google email and name
     * User needs to set password and username to complete registration
     * Expects: idToken (Google ID token) in request body
     */
    @PostMapping("/google/register")
    public ResponseEntity<?> googleRegister(@RequestBody Map<String, String> request) {
        System.out.println("[OAuth2Controller] Received request. Keys: " + request.keySet());
        System.out.println("[OAuth2Controller] Request class: " + request.getClass().getName());
        try {
            String idToken = extractIdToken(request);
            if (idToken == null || idToken.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Google ID token is required. Keys present: " + request.keySet());
                error.put("error", "missing_id_token");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            // Register with Google - returns setup response (not logged in yet)
            RegistrationSetupResponse response = oauth2Service.registerWithGoogle(idToken);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Google registration failed: " + e.getMessage());
            error.put("error", "registration_failed");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Complete OAuth2 registration setup
     * User provides password and desired username
     * Returns JWT tokens for immediate login
     * Expects: userId (path), request body with password and username
     */
    @PostMapping("/complete-registration/{userId}")
    public ResponseEntity<?> completeRegistration(
            @PathVariable String userId,
            @RequestBody CompleteRegistrationRequest request) {
        try {
            LoginResponse response = oauth2Service.completeOAuth2Registration(userId, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("error", "user_not_found");
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to complete registration: " + e.getMessage());
            error.put("error", "completion_failed");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Link OAuth2 account to existing user
     * Requires: userId (path), code (query param)
     */
    @PostMapping("/link/{userId}")
    public ResponseEntity<Map<String, String>> linkOAuth2Account(
            @PathVariable String userId,
            @RequestParam String provider,
            @RequestParam String code) {
        try {
            oauth2Service.linkOAuth2Account(userId, provider, code);
            Map<String, String> response = new HashMap<>();
            response.put("message", "OAuth2 account linked successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get OAuth2 configuration for frontend
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getOAuth2Config() {
        // Delegate to service implementation so the redirect URI is consistent
        return new ResponseEntity<>(oauth2Service.getOAuth2Config(), HttpStatus.OK);
    }

    private String extractIdToken(Map<String, String> request) {
        if (request == null) {
            return null;
        }

        String idToken = request.get("credential");
        if (idToken == null || idToken.isBlank()) {
            idToken = request.get("idToken");
        }
        if (idToken == null || idToken.isBlank()) {
            idToken = request.get("code");
        }

        return idToken;
    }
}
