package com.elrics.patientportal.repository;

import com.elrics.patientportal.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
}
