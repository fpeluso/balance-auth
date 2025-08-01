package it.peluso.balanceauth.controller;

import it.peluso.balanceauth.dto.LoginRequest;
import it.peluso.balanceauth.dto.LoginResponse;
import it.peluso.balanceauth.dto.UserProfileResponse;
import it.peluso.balanceauth.entity.User;
import it.peluso.balanceauth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        UserProfileResponse profile = authService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(Authentication authentication) {
        return ResponseEntity.ok("Token is valid for user: " + authentication.getName());
    }
}