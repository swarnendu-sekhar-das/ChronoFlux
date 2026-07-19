package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.service.TokenService;
import com.chronoflux.adminapi.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class OAuthController {

    private final TokenService tokenService;
    private final UserService userService;

    public OAuthController(TokenService tokenService, UserService userService) {
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String idTokenString = payload.get("idToken");
        
        GoogleIdToken.Payload tokenPayload = tokenService.verifyToken(idTokenString);
        
        if (tokenPayload != null) {
            // Valid token, upsert user in DB
            userService.upsertUser(tokenPayload);

            // Create a HTTP-only cookie (Secure=false for local HTTP development)
            Cookie cookie = new Cookie("AUTH_TOKEN", idTokenString);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true in production with HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour
            
            response.addCookie(cookie);
            
            return ResponseEntity.ok(Map.of("message", "Login successful", "email", tokenPayload.getEmail()));
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid ID token"));
        }
    }
    
    @PostMapping("/signout")
    public ResponseEntity<?> signout(HttpServletResponse response) {
        Cookie cookie = new Cookie("AUTH_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Must match the original cookie's Secure flag
        cookie.setPath("/");
        cookie.setMaxAge(0);
        
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
