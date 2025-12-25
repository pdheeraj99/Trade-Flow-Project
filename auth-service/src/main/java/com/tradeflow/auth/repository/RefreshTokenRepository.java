package com.tradeflow.auth.repository;

import com.tradeflow.auth.entity.RefreshToken;
import com.tradeflow.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find valid (non-revoked, non-expired) token
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllUserTokens(@Param("user") User user);

    /**
     * Delete expired tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Count active tokens for a user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
