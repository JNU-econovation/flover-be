package com.flover.flover_be.user.controller;

import com.flover.flover_be.user.dto.AuthDto;
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

@Tag(name = "Auth", description = "카카오 OAuth 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;

    @Operation(summary = "카카오 로그인", description = "프론트에서 받은 인가 코드로 JWT 토큰 발급")
    @PostMapping("/kakao/login")
    public ResponseEntity<AuthDto.LoginResponse> kakaoLogin(
            @RequestBody @Valid AuthDto.CallbackRequest request
    ) {
        AuthDto.LoginResponse response = kakaoAuthService.kakaoLogin(request.code());
        return ResponseEntity.ok(response);
    }
}
