package com.flover.flover_be.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";
    private static final long EXPIRATION = 3_600_000L;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, EXPIRATION);

    @Test
    void generateToken_토큰_생성_성공() {
        String token = jwtProvider.generateToken(1L);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_subject가_userId와_일치() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String token = jwtProvider.generateToken(42L);

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
    }
}
