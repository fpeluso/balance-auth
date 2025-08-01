package it.peluso.balanceauth.dto;

public class TokenResponse {
    private String access_token;
    private String refresh_token;
    private String token_type;
    private long expires_in;
    private String scope;

    public TokenResponse() {}

    public TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, String scope) {
        this.access_token = accessToken;
        this.refresh_token = refreshToken;
        this.token_type = tokenType;
        this.expires_in = expiresIn;
        this.scope = scope;
    }

    // Getters and setters
    public String getAccess_token() { return access_token; }
    public void setAccess_token(String access_token) { this.access_token = access_token; }
    public String getRefresh_token() { return refresh_token; }
    public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }
    public String getToken_type() { return token_type; }
    public void setToken_type(String token_type) { this.token_type = token_type; }
    public long getExpires_in() { return expires_in; }
    public void setExpires_in(long expires_in) { this.expires_in = expires_in; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}