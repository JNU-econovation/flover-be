package com.flover.flover_be.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flover.flover_be.global.config.AppleProperties;
import com.flover.flover_be.global.jwt.JwtProvider;
import com.flover.flover_be.user.domain.OAuthProvider;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.AppleDto;
import com.flover.flover_be.user.dto.AuthDto;
import com.flover.flover_be.user.exception.AppleAuthException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppleAuthServiceTest {

    private static final String KID = "test-kid-001";
    private static final String APPLE_ID = "apple.user.id.00001";
    private static final String EMAIL = "user@privaterelay.appleid.com";
    private static final String CLIENT_ID = "com.example.plover";

    private static KeyPair keyPair;

    @Mock private UserService userService;
    @Mock private JwtProvider jwtProvider;
    @Mock private RestClient restClient;
    @Mock private AppleProperties appleProperties;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks private AppleAuthService appleAuthService;

    private RestClient.RequestHeadersUriSpec getUriSpec;
    private RestClient.RequestHeadersSpec getHeadersSpec;
    private RestClient.ResponseSpec getResponseSpec;

    @BeforeAll
    static void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        getResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);

        when(appleProperties.clientId()).thenReturn(CLIENT_ID);
    }

    // ──────────────── helpers ────────────────

    private String buildIdentityToken(String iss, String aud, String sub, String email, Date expiration) {
        var builder = Jwts.builder()
                .header().add("kid", KID).and()
                .issuer(iss)
                .subject(sub)
                .claim("email", email)
                .expiration(expiration)
                .signWith(keyPair.getPrivate());
        if (aud != null) {
            builder.audience().add(aud);
        }
        return builder.compact();
    }

    private AppleDto.JwksResponse buildJwksResponse() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        byte[] nBytes = stripLeadingZero(pub.getModulus().toByteArray());
        byte[] eBytes = stripLeadingZero(pub.getPublicExponent().toByteArray());
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes);
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(eBytes);
        return new AppleDto.JwksResponse(List.of(new AppleDto.JwksKey("RSA", KID, "sig", "RS256", n, e)));
    }

    private byte[] stripLeadingZero(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }

    private void stubJwks() {
        when(getResponseSpec.body(AppleDto.JwksResponse.class)).thenReturn(buildJwksResponse());
    }

    private String validToken() {
        return buildIdentityToken(
                "https://appleid.apple.com", CLIENT_ID, APPLE_ID, EMAIL,
                new Date(System.currentTimeMillis() + 3_600_000)
        );
    }

    // ──────────────── 성공 케이스 ────────────────

    @DisplayName("유효한 identityToken으로 로그인하면 JWT를 반환한다")
    @Test
    void 유효한_identityToken_로그인_성공() {
        // given
        User user = User.create(OAuthProvider.APPLE, APPLE_ID, EMAIL, "플러버123456", null);
        stubJwks();
        when(userService.upsertOAuthUser(OAuthProvider.APPLE, APPLE_ID, EMAIL)).thenReturn(user);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        // when
        AuthDto.LoginResponse result = appleAuthService.appleLogin(validToken(), null);

        // then
        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        verify(userService).upsertOAuthUser(OAuthProvider.APPLE, APPLE_ID, EMAIL);
    }

    @DisplayName("identityToken에서 sub와 email을 추출해 upsert를 호출한다")
    @Test
    void identityToken_claims_추출_검증() {
        // given
        String specificAppleId = "apple.specific.id.99999";
        String specificEmail = "specific@privaterelay.appleid.com";
        String token = buildIdentityToken(
                "https://appleid.apple.com", CLIENT_ID,
                specificAppleId, specificEmail,
                new Date(System.currentTimeMillis() + 3_600_000)
        );
        User user = User.create(OAuthProvider.APPLE, specificAppleId, specificEmail, "플러버111111", null);
        stubJwks();
        when(userService.upsertOAuthUser(any(), any(), any())).thenReturn(user);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        // when
        appleAuthService.appleLogin(token, "홍길동");

        // then
        verify(userService).upsertOAuthUser(OAuthProvider.APPLE, specificAppleId, specificEmail);
    }

    // ──────────────── 토큰 검증 실패 ────────────────

    @DisplayName("만료된 identityToken이면 AppleAuthException이 발생한다")
    @Test
    void 만료된_identityToken_예외() {
        // given
        String expiredToken = buildIdentityToken(
                "https://appleid.apple.com", CLIENT_ID, APPLE_ID, EMAIL,
                new Date(System.currentTimeMillis() - 1_000)
        );
        stubJwks();

        // when & then
        assertThatThrownBy(() -> appleAuthService.appleLogin(expiredToken, null))
                .isInstanceOf(AppleAuthException.class);
    }

    @DisplayName("서명이 유효하지 않은 토큰이면 AppleAuthException이 발생한다")
    @Test
    void 서명_무효_토큰_예외() throws NoSuchAlgorithmException {
        // given: validToken과 다른 키로 서명된 JWKS를 반환 → 서명 불일치
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();

        RSAPublicKey otherPub = (RSAPublicKey) otherKeyPair.getPublic();
        byte[] nBytes = stripLeadingZero(otherPub.getModulus().toByteArray());
        byte[] eBytes = stripLeadingZero(otherPub.getPublicExponent().toByteArray());
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes);
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(eBytes);
        AppleDto.JwksResponse mismatchedJwks = new AppleDto.JwksResponse(
                List.of(new AppleDto.JwksKey("RSA", KID, "sig", "RS256", n, e))
        );
        when(getResponseSpec.body(AppleDto.JwksResponse.class)).thenReturn(mismatchedJwks);

        // when & then
        assertThatThrownBy(() -> appleAuthService.appleLogin(validToken(), null))
                .isInstanceOf(AppleAuthException.class);
    }

    // ──────────────── claims 검증 실패 ────────────────

    @DisplayName("iss가 애플 도메인이 아니면 AppleAuthException이 발생한다")
    @Test
    void 잘못된_iss_예외() {
        // given
        String token = buildIdentityToken(
                "https://invalid.issuer.com", CLIENT_ID, APPLE_ID, EMAIL,
                new Date(System.currentTimeMillis() + 3_600_000)
        );
        stubJwks();

        // when & then
        assertThatThrownBy(() -> appleAuthService.appleLogin(token, null))
                .isInstanceOf(AppleAuthException.class);
    }

    @DisplayName("aud가 앱 Bundle ID와 다르면 AppleAuthException이 발생한다")
    @Test
    void 잘못된_aud_예외() {
        // given
        String token = buildIdentityToken(
                "https://appleid.apple.com", "com.other.app", APPLE_ID, EMAIL,
                new Date(System.currentTimeMillis() + 3_600_000)
        );
        stubJwks();

        // when & then
        assertThatThrownBy(() -> appleAuthService.appleLogin(token, null))
                .isInstanceOf(AppleAuthException.class);
    }

    // ──────────────── JWKS 조회 실패 ────────────────

    @DisplayName("JWKS 조회에 실패하면 AppleAuthException이 발생한다")
    @Test
    void JWKS_조회_실패_예외() {
        // given
        when(getResponseSpec.body(AppleDto.JwksResponse.class))
                .thenThrow(mock(RestClientException.class));

        // when & then
        assertThatThrownBy(() -> appleAuthService.appleLogin(validToken(), null))
                .isInstanceOf(AppleAuthException.class);
    }

    @DisplayName("JWKS에 매칭되는 kid가 없으면 AppleAuthException이 발생한다")
    @Test
    void kid_매칭_없음_예외() {
        // given
        AppleDto.JwksResponse noMatchJwks = new AppleDto.JwksResponse(
                List.of(new AppleDto.JwksKey("RSA", "different-kid", "sig", "RS256", "n", "e"))
        );
        when(getResponseSpec.body(AppleDto.JwksResponse.class)).thenReturn(noMatchJwks);

        // when & then
        assertThatThrownBy(() -> appleAuthService.appleLogin(validToken(), null))
                .isInstanceOf(AppleAuthException.class);
    }

    @DisplayName("잘못된 형식의 identityToken이면 AppleAuthException이 발생한다")
    @Test
    void 잘못된_토큰_형식_예외() {
        assertThatThrownBy(() -> appleAuthService.appleLogin("not.a.valid.jwt.token", null))
                .isInstanceOf(AppleAuthException.class);
    }
}