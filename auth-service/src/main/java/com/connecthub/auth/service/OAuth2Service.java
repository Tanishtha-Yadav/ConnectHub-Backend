package com.connecthub.auth.service;

import com.connecthub.auth.dto.request.CompleteRegistrationRequest;
import com.connecthub.auth.dto.response.LoginResponse;
import com.connecthub.auth.dto.response.RegistrationSetupResponse;

/**
 * OAuth2 Authentication Service Interface
 * Handles authentication with OAuth2 providers (Google, GitHub)
 */
public interface OAuth2Service {

    /**
     * Authenticate user with Google OAuth2 (login only)
     * @param idToken Google ID token from Google Sign-In
     * @return LoginResponse with JWT token
     * @throws UserNotFoundException if user email is not registered
     */
    LoginResponse authenticateWithGoogle(String idToken);

    /**
     * Register or login user with Google OAuth2
     * Creates new account if email doesn't exist
     * @param idToken Google ID token from Google Sign-In
     * @return RegistrationSetupResponse indicating user needs to set password/username
     */
    RegistrationSetupResponse registerWithGoogle(String idToken);

    /**
     * Complete OAuth2 registration by setting password and username
     * @param userId User ID from registration
     * @param request Contains password and username
     * @return LoginResponse with JWT token
     */
    LoginResponse completeOAuth2Registration(String userId, CompleteRegistrationRequest request);

    /**
     * Link OAuth2 account to existing user
     * @param userId User ID
     * @param provider OAuth2 provider name (google/github)
     * @param idToken Google ID token
     */
    void linkOAuth2Account(String userId, String provider, String idToken);

    /**
     * Get OAuth2 provider configuration
     * @return Map with provider URLs and keys
     */
    java.util.Map<String, String> getOAuth2Config();

    /**
     * Exchange an OAuth2 authorization code for an ID token (Google) and return the id_token JWT.
     * This is used to support the authorization-code redirect flow where Google performs a browser redirect.
     * @param code authorization code
     * @return id_token JWT string
     */
    String exchangeCodeForIdToken(String code);
}
