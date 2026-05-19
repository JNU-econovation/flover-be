package com.flover.flover_be.user.repository;

import com.flover.flover_be.user.domain.OAuthProvider;
import com.flover.flover_be.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    boolean existsByNickname(String nickname);
}