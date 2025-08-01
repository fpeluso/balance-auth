package it.peluso.balanceauth.service;

import it.peluso.balanceauth.dto.TokenIntrospectionResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TokenService {
    
    private final JwtDecoder jwtDecoder;
    
    public TokenService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }
    
    public TokenIntrospectionResponse introspectToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Jwt jwt = jwtDecoder.decode(token);
            
            TokenIntrospectionResponse response = new TokenIntrospectionResponse(true);
            response.setSub(jwt.getSubject());
            response.setUsername(jwt.getSubject());
            response.setExp(jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : null);
            response.setIat(jwt.getIssuedAt() != null ? jwt.getIssuedAt().getEpochSecond() : null);
            response.setScope(jwt.getClaimAsString("scope"));
            response.setClientId(jwt.getClaimAsString("azp"));
            
            // Extract authorities from JWT claims
            if (jwt.hasClaim("authorities")) {
                List<String> authorities = jwt.getClaimAsStringList("authorities");
                response.setAuthorities(authorities);
            }
            
            return response;
        } catch (JwtException e) {
            return new TokenIntrospectionResponse(false);
        }
    }
}