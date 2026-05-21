package com.flover.flover_be.user.controller;

import com.flover.flover_be.global.auth.LoginUserId;
import com.flover.flover_be.user.dto.AppleDto;
import com.flover.flover_be.user.dto.AuthDto;
import com.flover.flover_be.user.service.AppleAuthService;
import com.flover.flover_be.user.service.KakaoAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "소셜 OAuth 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final AppleAuthService appleAuthService;

    @Operation(summary = "카카오 로그인 (Code 방식, Deprecated)", description = "WebView 방식. 인가 코드로 JWT 발급. Native SDK 전환 완료 후 제거 예정.")
    @PostMapping("/kakao/login")
    public ResponseEntity<AuthDto.LoginResponse> kakaoLogin(
            @RequestBody @Valid AuthDto.CallbackRequest request
    ) {
        AuthDto.LoginResponse response = kakaoAuthService.kakaoLogin(request.code());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "카카오 로그인 (Native SDK 방식)", description = "카카오 Native SDK에서 발급받은 accessToken으로 JWT 발급")
    @PostMapping("/v2/kakao/login")
    public ResponseEntity<AuthDto.LoginResponse> kakaoLoginV2(
            @RequestBody @Valid AuthDto.KakaoTokenRequest request
    ) {
        AuthDto.LoginResponse response = kakaoAuthService.kakaoLoginWithToken(request.accessToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "애플 로그인", description = "iOS 앱에서 받은 identityToken으로 JWT 토큰 발급")
    @PostMapping("/apple/login")
    public ResponseEntity<AuthDto.LoginResponse> appleLogin(
            @RequestBody @Valid AppleDto.LoginRequest request
    ) {
        AuthDto.LoginResponse response = appleAuthService.appleLogin(request.identityToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "서버 처리 없이 204 반환. 클라이언트가 토큰을 삭제해야 합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@LoginUserId Long userId) {
        return ResponseEntity.noContent().build();
    }
}