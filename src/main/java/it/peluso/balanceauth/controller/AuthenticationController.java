package it.peluso.balanceauth.controller;

import it.peluso.balanceauth.dto.LoginRequest;
import it.peluso.balanceauth.dto.TokenResponse;
import it.peluso.balanceauth.service.AuthenticationService;
import it.peluso.balanceauth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            TokenResponse tokenResponse = authenticationService.authenticate(loginRequest);
            return ResponseEntity.ok(tokenResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("refresh_token") String refreshToken) {
        try {
            TokenResponse tokenResponse = authenticationService.refreshToken(refreshToken);
            return ResponseEntity.ok(tokenResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Token refresh failed: " + e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            
            if (jwtService.isTokenValid(token)) {
                String username = jwtService.validateTokenAndGetUsername(token).orElse("Unknown");
                return ResponseEntity.ok().body("Token is valid for user: " + username);
            } else {
                return ResponseEntity.badRequest().body("Invalid or expired token");
            }
        }
        return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
    }
}