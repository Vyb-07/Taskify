package com.taskify.taskify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Injected from application.properties
    @Value("${jwt.secret}")
    private String secret;

    // Expiration in milliseconds (e.g. 3600000 = 1 hour)
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Generate token for given userDetails. We allow passing extra claims map if
     * needed.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Convenience overload when no extra claims are required.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = Map.of(
                "roles", userDetails.getAuthorities().stream()
                        .map(grantedAuthority -> grantedAuthority.getAuthority())
                        .collect(java.util.stream.Collectors.toList()));
        return generateToken(claims, userDetails);
    }

    /**
     * Extract username (subject) from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generic extractor for any claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validate token contents and expiration.
     * Note: checking of authorities/roles inside claims can be added if needed.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    /**
     * Parse all claims (will throw on invalid signature / malformed token).
     */
    private Claims parseAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Build a SecretKey from configured secret. We expect the secret to be base64
     * (or raw).
     * Uses io.jsonwebtoken.security.Keys to produce a safe HMAC key.
     */
    private SecretKey getSigningKey() {
        // Try to decode base64; if that fails, use raw bytes
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ex) {
            keyBytes = secret.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}