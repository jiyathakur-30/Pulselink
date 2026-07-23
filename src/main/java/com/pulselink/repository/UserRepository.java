package com.pulselink.repository;

import com.pulselink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRoleName(String roleName);
    long countByRoleName(String roleName);
}
