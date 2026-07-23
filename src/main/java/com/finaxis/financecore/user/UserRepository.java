package com.finaxis.financecore.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByIdAndActiveTrue(Long id);

    boolean existsByEmailIgnoreCase(String email);
}
