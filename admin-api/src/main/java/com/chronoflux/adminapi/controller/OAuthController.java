package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.service.TokenService;
import com.chronoflux.adminapi.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> callback(@RequestBody Map<String, String> payload,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String idTokenString = payload.get("idToken");
        
        GoogleIdToken.Payload tokenPayload = tokenService.verifyToken(idTokenString);
        
        if (tokenPayload != null) {
            userService.upsertUser(tokenPayload);

            // HttpOnly prevents JS from reading the token, SameSite=Strict blocks CSRF
            Cookie cookie = new Cookie("AUTH_TOKEN", idTokenString);
            cookie.setHttpOnly(true);
            // Set Secure flag dynamically: true if request is HTTPS or terminated via reverse proxy
            cookie.setSecure(isSecureRequest(request));
            cookie.setAttribute("SameSite", "Strict");
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour session
            
            response.addCookie(cookie);
            
            return ResponseEntity.ok(Map.of("message", "Login successful", "email", tokenPayload.getEmail()));
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid ID token"));
        }
    }
    
    @PostMapping("/signout")
    public ResponseEntity<?> signout(HttpServletRequest request, HttpServletResponse response) {
        // Clear cookie by setting maxAge=0 (matching same path and secure flag)
        Cookie cookie = new Cookie("AUTH_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureRequest(request));
        cookie.setAttribute("SameSite", "Strict");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
