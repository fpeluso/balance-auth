package it.peluso.balanceauth.dto;

import java.util.Set;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private boolean enabled;
    
    public UserProfileResponse() {}
    
    public UserProfileResponse(Long id, String username, String email, Set<String> roles, boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}