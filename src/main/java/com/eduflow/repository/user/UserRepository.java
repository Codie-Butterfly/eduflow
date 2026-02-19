package com.eduflow.repository.user;

import com.eduflow.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByPasswordResetToken(String token);

    Optional<User> findByRefreshToken(String refreshToken);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(com.eduflow.entity.user.Role.RoleName roleName);

    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<User> findAllActive();
}
