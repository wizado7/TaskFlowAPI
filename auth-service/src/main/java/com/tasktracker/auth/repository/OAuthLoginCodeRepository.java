package com.tasktracker.auth.repository;

import com.tasktracker.auth.entity.OAuthLoginCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from OAuthLoginCode code where code.codeHash = :codeHash")
    Optional<OAuthLoginCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);

    long deleteByExpiresAtBefore(Instant expiresAt);
}
