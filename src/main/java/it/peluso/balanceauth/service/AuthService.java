package it.peluso.balanceauth.service;

import it.peluso.balanceauth.dto.LoginRequest;
import it.peluso.balanceauth.dto.LoginResponse;
import it.peluso.balanceauth.dto.UserProfileResponse;
import it.peluso.balanceauth.entity.User;
import it.peluso.balanceauth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    
    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }
    
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            return new LoginResponse(
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                "Login successful. Use OAuth2 endpoints to get access tokens."
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }
    
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles(),
            user.isEnabled()
        );
    }
}