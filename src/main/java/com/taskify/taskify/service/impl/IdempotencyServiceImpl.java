package com.taskify.taskify.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskify.taskify.exception.IdempotencyException;
import com.taskify.taskify.model.IdempotencyKey;
import com.taskify.taskify.repository.IdempotencyKeyRepository;
import com.taskify.taskify.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final int ttlMinutes;

    public IdempotencyServiceImpl(IdempotencyKeyRepository repository, ObjectMapper objectMapper,
            @Value("${app.idempotency.ttl-minutes:1440}") int ttlMinutes) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.ttlMinutes = ttlMinutes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyKey> getStoredResponse(String key, Long userId, String endpoint, Object requestBody) {
        Optional<IdempotencyKey> storedKey = repository.findByIdempotencyKeyAndUserIdAndEndpoint(key, userId, endpoint);

        if (storedKey.isPresent()) {
            IdempotencyKey idempotencyKey = storedKey.get();

            if (idempotencyKey.isExpired()) {
                return Optional.empty();
            }

            String currentHash = generateHash(requestBody);
            if (!idempotencyKey.getRequestHash().equals(currentHash)) {
                throw new IdempotencyException("Idempotency key reused with a different request payload.");
            }

            return Optional.of(idempotencyKey);
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void saveResponse(String key, Long userId, String endpoint, Object requestBody, Object responseBody,
            int status) {
        String requestHash = generateHash(requestBody);
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing response for idempotency storage", e);
        }

        IdempotencyKey idempotencyKey = new IdempotencyKey(
                key, userId, endpoint, requestHash, responseJson, status, ttlMinutes);

        repository.save(idempotencyKey);
    }

    @Override
    @Transactional
    public void cleanupExpiredKeys() {
        repository.deleteExpiredKeys(LocalDateTime.now());
    }

    private String generateHash(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating request hash for idempotency", e);
        }
    }
}
