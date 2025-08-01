package it.peluso.balanceauth.dto;

import java.util.List;

public class TokenIntrospectionResponse {
    private boolean active;
    private String sub;
    private String username;
    private List<String> authorities;
    private Long exp;
    private Long iat;
    private String scope;
    private String clientId;
    
    public TokenIntrospectionResponse() {}
    
    public TokenIntrospectionResponse(boolean active) {
        this.active = active;
    }
    
    // Getters and setters
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<String> getAuthorities() { return authorities; }
    public void setAuthorities(List<String> authorities) { this.authorities = authorities; }
    public Long getExp() { return exp; }
    public void setExp(Long exp) { this.exp = exp; }
    public Long getIat() { return iat; }
    public void setIat(Long iat) { this.iat = iat; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}