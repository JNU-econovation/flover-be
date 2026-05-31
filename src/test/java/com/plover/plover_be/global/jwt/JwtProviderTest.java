package com.plover.plover_be.global.jwt;

import com.plover.plover_be.global.auth.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void getUserId_유효한_토큰에서_userId_반환() {
        String token = jwtProvider.generateToken(42L);

        assertThat(jwtProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void getUserId_만료된_토큰이면_AuthException_발생() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("1")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtProvider.getUserId(expiredToken))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void getUserId_변조된_토큰이면_AuthException_발생() {
        String token = jwtProvider.generateToken(1L);

        assertThatThrownBy(() -> jwtProvider.getUserId(token + "tampered"))
                .isInstanceOf(AuthException.class);
    }
}
