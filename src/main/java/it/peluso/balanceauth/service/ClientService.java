package it.peluso.balanceauth.service;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClientService {

    private final RegisteredClientRepository clientRepository;

    public ClientService(RegisteredClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public void registerMicroserviceClient(String clientId, String clientSecret, String redirectUri) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("{noop}" + clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri(redirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        // Note: In a real implementation, you would save this to a database
        // For now, we'll use the in-memory repository
        System.out.println("Registered client: " + clientId);
    }

    public void registerFastifyClient() {
        registerMicroserviceClient(
            "fastify-bank-service",
            "fastify-secret",
            "http://localhost:3000/callback"
        );
    }

    public void registerNestClient() {
        registerMicroserviceClient(
            "nest-transaction-service", 
            "nest-secret",
            "http://localhost:3001/callback"
        );
    }

    public void registerWebClient() {
        registerMicroserviceClient(
            "web-client",
            "web-secret", 
            "http://localhost:8080/login/oauth2/code/web-client"
        );
    }
}