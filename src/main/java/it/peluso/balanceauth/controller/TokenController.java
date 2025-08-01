package it.peluso.balanceauth.controller;

import it.peluso.balanceauth.dto.TokenIntrospectionResponse;
import it.peluso.balanceauth.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token")
public class TokenController {
    
    private final TokenService tokenService;
    
    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    @PostMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspectToken(@RequestParam String token) {
        TokenIntrospectionResponse response = tokenService.introspectToken(token);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/info")
    public ResponseEntity<TokenIntrospectionResponse> getTokenInfo(@RequestParam String token) {
        TokenIntrospectionResponse response = tokenService.introspectToken(token);
        return ResponseEntity.ok(response);
    }
}