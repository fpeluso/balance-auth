package it.peluso.balanceauth.service;

import it.peluso.balanceauth.dto.LoginRequest;
import it.peluso.balanceauth.dto.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository clientRepository;
    private final AuthorizationServerSettings authorizationServerSettings;

    public AuthenticationService(AuthenticationManager authenticationManager,
                               OAuth2AuthorizationService authorizationService,
                               RegisteredClientRepository clientRepository,
                               AuthorizationServerSettings authorizationServerSettings) {
        this.authenticationManager = authenticationManager;
        this.authorizationService = authorizationService;
        this.clientRepository = clientRepository;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    public TokenResponse authenticate(LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            // Get the registered client (using the default client)
            RegisteredClient registeredClient = clientRepository.findByClientId("oidc-client");

            // Create authorization
            OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(authentication.getName())
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(registeredClient.getScopes())
                .build();

            // Generate access token
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(Duration.ofHours(1)); // 1 hour expiry

            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                UUID.randomUUID().toString(),
                issuedAt,
                expiresAt,
                registeredClient.getScopes()
            );

            // Generate refresh token
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                UUID.randomUUID().toString(),
                issuedAt,
                expiresAt.plus(Duration.ofDays(30)) // 30 days expiry
            );

            // Set tokens in authorization
            authorization = OAuth2Authorization.from(authorization)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

            // Save authorization
            authorizationService.save(authorization);

            return new TokenResponse(
                accessToken.getTokenValue(),
                refreshToken.getTokenValue(),
                accessToken.getTokenType().getValue(),
                Duration.between(issuedAt, expiresAt).getSeconds(),
                String.join(" ", registeredClient.getScopes())
            );

        } catch (AuthenticationException e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public TokenResponse refreshToken(String refreshTokenValue) {
        // Find authorization by refresh token
        OAuth2Authorization authorization = authorizationService.findByToken(
            refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);

        if (authorization == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        RegisteredClient registeredClient = clientRepository.findById(authorization.getRegisteredClientId());

        // Generate new access token
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofHours(1));

        OAuth2AccessToken newAccessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            UUID.randomUUID().toString(),
            issuedAt,
            expiresAt,
            registeredClient.getScopes()
        );

        // Update authorization with new access token
        authorization = OAuth2Authorization.from(authorization)
            .accessToken(newAccessToken)
            .build();

        authorizationService.save(authorization);

        return new TokenResponse(
            newAccessToken.getTokenValue(),
            refreshTokenValue, // Keep the same refresh token
            newAccessToken.getTokenType().getValue(),
            Duration.between(issuedAt, expiresAt).getSeconds(),
            String.join(" ", registeredClient.getScopes())
        );
    }
}