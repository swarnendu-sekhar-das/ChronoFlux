package com.chronoflux.adminapi.repository;

import com.chronoflux.adminapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleSub(String googleSub);
    Optional<User> findByEmail(String email);
}
