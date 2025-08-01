package it.peluso.balanceauth.controller;

import it.peluso.balanceauth.entity.User;
import it.peluso.balanceauth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class UserInfoController {
    
    private final UserRepository userRepository;
    private final OAuth2AuthorizationService authorizationService;

    public UserInfoController(UserRepository userRepository, OAuth2AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.unauthorized().build();
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", user.getUsername());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("roles", user.getRoles());
        userInfo.put("enabled", user.isEnabled());
        
        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/token/introspect")
    public ResponseEntity<?> introspectToken(@RequestParam("token") String token,
                                           @RequestParam("token_type_hint") String tokenTypeHint) {
        try {
            // Find the authorization by access token
            OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
            
            Map<String, Object> response = new HashMap<>();
            
            if (authorization == null || authorization.getAccessToken() == null) {
                response.put("active", false);
                return ResponseEntity.ok(response);
            }

            // Check if token is still valid
            boolean isActive = authorization.getAccessToken().getToken().getTokenValue().equals(token) &&
                             authorization.getAccessToken().isActive();
            
            response.put("active", isActive);
            
            if (isActive) {
                response.put("scope", String.join(" ", authorization.getAuthorizedScopes()));
                response.put("client_id", authorization.getRegisteredClientId());
                response.put("username", authorization.getPrincipalName());
                response.put("token_type", "Bearer");
                response.put("exp", authorization.getAccessToken().getToken().getExpiresAt().getEpochSecond());
                response.put("iat", authorization.getAccessToken().getToken().getIssuedAt().getEpochSecond());
                response.put("sub", authorization.getPrincipalName());
                
                // Add user-specific claims
                Optional<User> userOpt = userRepository.findByUsername(authorization.getPrincipalName());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    response.put("roles", user.getRoles());
                    response.put("email", user.getEmail());
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("active", false);
            return ResponseEntity.ok(response);
        }
    }
}