package it.peluso.balanceauth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final JWKSource<SecurityContext> jwkSource;
    private final AuthorizationServerSettings authorizationServerSettings;

    public JwtService(JWKSource<SecurityContext> jwkSource, AuthorizationServerSettings authorizationServerSettings) {
        this.jwkSource = jwkSource;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    public Optional<String> validateTokenAndGetUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // Get the public key from JWK source
            RSAKey rsaKey = (RSAKey) jwkSource.get(null, null).get(0);
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            
            // Create verifier
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            
            // Verify signature
            if (!signedJWT.verify(verifier)) {
                return Optional.empty();
            }
            
            // Get claims
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            
            // Check if token is expired
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                return Optional.empty();
            }
            
            // Return username from subject claim
            return Optional.ofNullable(claimsSet.getSubject());
            
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }

    public boolean isTokenValid(String token) {
        return validateTokenAndGetUsername(token).isPresent();
    }

    public Optional<JWTClaimsSet> getClaimsFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return Optional.of(signedJWT.getJWTClaimsSet());
        } catch (ParseException e) {
            return Optional.empty();
        }
    }
}