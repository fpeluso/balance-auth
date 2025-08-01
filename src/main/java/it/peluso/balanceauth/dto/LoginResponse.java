package it.peluso.balanceauth.dto;

import java.util.Set;

public class LoginResponse {
    private String username;
    private String email;
    private Set<String> roles;
    private String message;
    
    public LoginResponse() {}
    
    public LoginResponse(String username, String email, Set<String> roles, String message) {
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.message = message;
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}