package com.taskify.taskify.service;

import com.taskify.taskify.model.IdempotencyKey;
import java.util.Optional;

public interface IdempotencyService {

    Optional<IdempotencyKey> getStoredResponse(String key, Long userId, String endpoint, Object requestBody);

    void saveResponse(String key, Long userId, String endpoint, Object requestBody, Object responseBody, int status);

    void cleanupExpiredKeys();
}
