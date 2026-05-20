package com.flover.flover_be.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flover.flover_be.global.config.AppleProperties;
import com.flover.flover_be.global.jwt.JwtProvider;
import com.flover.flover_be.user.domain.OAuthProvider;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.AppleDto;
import com.flover.flover_be.user.dto.AuthDto;
import com.flover.flover_be.user.exception.AppleAuthException;
import com.flover.flover_be.user.exception.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISS = "https://appleid.apple.com";

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RestClient restClient;
    private final AppleProperties appleProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthDto.LoginResponse appleLogin(String identityToken) {
        Claims claims = verifyIdentityToken(identityToken);
        String appleId = claims.getSubject();
        String email = claims.get("email", String.class);

        User user = userService.upsertOAuthUser(OAuthProvider.APPLE, appleId, email);
        String jwtToken = jwtProvider.generateToken(user.getId());

        return new AuthDto.LoginResponse(jwtToken, "Bearer", user.getId(), user.getNickname(),
                user.getEmail());
    }

    private Claims verifyIdentityToken(String identityToken) {
        logDecodedTokenInfo(identityToken);
        String kid = extractKid(identityToken);
        PublicKey publicKey = fetchApplePublicKey(kid);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            validateClaims(claims);
            return claims;
        } catch (AppleAuthException e) {
            throw e;
        } catch (JwtException e) {
            log.error("[Apple] 서명 검증 실패 — {}: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
        } catch (Exception e) {
            log.error("[Apple] identityToken 처리 오류 — {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }
    }

    // 서명 검증 전에 만료 여부와 aud를 미리 확인해 실패 원인을 로그로 남긴다
    private void logDecodedTokenInfo(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));

            long exp = payload.path("exp").asLong();
            long now = System.currentTimeMillis() / 1000;
            if (exp < now) {
                log.error("[Apple] 토큰 만료 — {}초 전 만료 (exp={}, now={})", now - exp, exp, now);
            }

            String aud = payload.path("aud").asText();
            if (!appleProperties.clientId().equals(aud)) {
                log.error("[Apple] aud 불일치 — 토큰: {}, 설정: {}", aud, appleProperties.clientId());
            }
        } catch (Exception e) {
            log.warn("[Apple] 토큰 사전 디코딩 실패: {}", e.getMessage());
        }
    }

    private String extractKid(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            JsonNode header = objectMapper.readTree(headerBytes);
            String kid = header.path("kid").asText(null);
            if (kid == null) {
                log.error("[Apple] 토큰 헤더에 kid 없음");
                throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
            }
            return kid;
        } catch (AppleAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Apple] kid 추출 실패: {}", e.getMessage());
            throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }
    }

    private PublicKey fetchApplePublicKey(String kid) {
        AppleDto.JwksResponse jwks;
        try {
            jwks = restClient.get()
                    .uri(APPLE_JWKS_URL)
                    .retrieve()
                    .body(AppleDto.JwksResponse.class);
        } catch (RestClientException e) {
            log.error("[Apple DEBUG] JWKS 조회 실패: {}", e.getMessage());
            throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }

        if (jwks == null) {
            log.error("[Apple] JWKS 응답이 null");
            throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }

        List<String> availableKids = jwks.keys().stream().map(AppleDto.JwksKey::kid).toList();

        AppleDto.JwksKey matchedKey = jwks.keys().stream()
                .filter(key -> kid.equals(key.kid()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("[Apple] kid 불일치 — 토큰 kid: {}, JWKS kids: {}", kid, availableKids);
                    return new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
                });

        return buildRsaPublicKey(matchedKey);
    }

    private PublicKey buildRsaPublicKey(AppleDto.JwksKey key) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(key.n());
            byte[] eBytes = Base64.getUrlDecoder().decode(key.e());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, nBytes),
                    new BigInteger(1, eBytes)
            );
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Apple RSA 공개키 생성 실패: {}", e.getMessage());
            throw new AppleAuthException(AuthErrorCode.APPLE_INVALID_TOKEN);
        }
    }

    private void validateClaims(Claims claims) {
        String iss = claims.getIssuer();
        Set<String> audience = claims.getAudience();
        String sub = claims.getSubject();
        log.debug("[Apple] iss={}, aud={}, sub={}", iss, audience, sub);

        if (!APPLE_ISS.equals(iss)) {
            log.error("[Apple] iss 불일치 — 실제: {}, 기대: {}", iss, APPLE_ISS);
            throw new AppleAuthException(AuthErrorCode.APPLE_CLAIMS_INVALID);
        }

        String expectedClientId = appleProperties.clientId();
        if (audience == null || !audience.contains(expectedClientId)) {
            log.error("[Apple] aud 불일치 — 실제: {}, 기대: {}", audience, expectedClientId);
            throw new AppleAuthException(AuthErrorCode.APPLE_CLAIMS_INVALID);
        }
    }
}
