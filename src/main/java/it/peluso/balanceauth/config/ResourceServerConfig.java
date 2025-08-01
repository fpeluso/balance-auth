package it.peluso.balanceauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration for JWT resource server validation
 * This configuration can be copied to your microservices for token validation
 */
@Configuration
public class ResourceServerConfig {

    /**
     * JWT Authentication Converter that extracts authorities from both 'scope' and 'roles' claims
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Get scope-based authorities
            Collection<GrantedAuthority> scopeAuthorities = authoritiesConverter.convert(jwt);
            
            // Get role-based authorities
            Collection<GrantedAuthority> roleAuthorities = extractRoleAuthorities(jwt);
            
            // Combine both
            return Stream.concat(scopeAuthorities.stream(), roleAuthorities.stream())
                    .collect(Collectors.toSet());
        });
        
        return converter;
    }

    /**
     * Extracts role authorities from JWT 'roles' claim
     */
    private Collection<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        
        return roles.stream()
                .map(role -> {
                    // Ensure role has ROLE_ prefix
                    if (!role.startsWith("ROLE_")) {
                        return new SimpleGrantedAuthority("ROLE_" + role);
                    }
                    return new SimpleGrantedAuthority(role);
                })
                .collect(Collectors.toList());
    }

    /**
     * JWT Decoder for validating tokens
     * In your microservices, you can configure this to point to this authorization server
     */
    @Bean(name = "resourceServerJwtDecoder")
    public JwtDecoder resourceServerJwtDecoder() {
        // For microservices, use NimbusJwtDecoder with the JWK Set URI
        return NimbusJwtDecoder.withJwkSetUri("http://localhost:9000/.well-known/jwks.json")
                .build();
    }
}