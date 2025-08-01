package it.peluso.balanceauth.controller;

import it.peluso.balanceauth.entity.User;
import it.peluso.balanceauth.service.JwtService;
import it.peluso.balanceauth.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final RegistrationService registrationService;
    private final JwtService jwtService;

    public UserController(RegistrationService registrationService, JwtService jwtService) {
        this.registrationService = registrationService;
        this.jwtService = jwtService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            
            return jwtService.validateTokenAndGetUsername(token)
                .map(username -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("username", username);
                    response.put("message", "Profile retrieved successfully");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.badRequest().body("Invalid token"));
        }
        return ResponseEntity.badRequest().body("Missing Authorization header");
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        // This would typically call a service to get all users
        // For now, return a placeholder response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin endpoint - all users would be returned here");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        // This would typically call a service to delete a user
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User with ID " + userId + " would be deleted");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        // This would typically call a service to update user roles
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Roles for user " + userId + " would be updated");
        response.put("newRoles", request.get("roles"));
        return ResponseEntity.ok(response);
    }
}