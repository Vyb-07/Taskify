package com.taskify.taskify.service;

import com.taskify.taskify.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);

    RefreshToken verifyExpiration(RefreshToken token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);

    void revokeAllUserTokens(Long userId);
}
