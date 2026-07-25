package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.model.User;
import com.chronoflux.adminapi.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${admin.allowed.emails:admin@chronoflux.com}")
    private java.util.List<String> allowedAdmins;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User upsertUser(GoogleIdToken.Payload payload) {
        String googleSub = payload.getSubject();
        
        if (!allowedAdmins.contains(payload.getEmail())) {
            throw new SecurityException("Not an authorized admin.");
        }
        
        return userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setGoogleSub(googleSub);
                    newUser.setEmail(payload.getEmail());
                    newUser.setName((String) payload.get("name"));
                    newUser.setAvatarUrl((String) payload.get("picture"));
                    newUser.setRole("ADMIN"); 
                    return userRepository.save(newUser);
                });
    }
}
