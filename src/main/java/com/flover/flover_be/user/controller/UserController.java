package com.flover.flover_be.user.controller;

import com.flover.flover_be.global.auth.LoginUserId;
import com.flover.flover_be.global.storage.StorageDto;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 정보 조회", description = "닉네임, 레벨, 프로필 이미지 URL 반환")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto.UserInfoResponse> getUserInfo(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok(userService.findUserInfo(userId));
    }

    @Operation(summary = "플로깅 누적 통계 조회",
            description = "마이페이지용 로그인 사용자의 총 플로깅 횟수, 걸음 수, 거리를 반환합니다.")
    @GetMapping("/me/plogging-stats")
    public ResponseEntity<UserDto.PloggingStatsResponse> getPloggingStats(@LoginUserId Long userId) {
        return ResponseEntity.ok(userService.findPloggingStats(userId));
    }

    @Operation(summary = "닉네임 수정", description = "사용자 닉네임 변경")
    @PutMapping("/me/nickname")
    public ResponseEntity<UserDto.NicknameResponse> updateNickname(
            @LoginUserId Long userId,
            @RequestBody @Valid UserDto.NicknameRequest request
    ) {
        return ResponseEntity.ok(userService.updateNickname(userId, request.nickname()));
    }

    @Operation(summary = "프로필 이미지 업로드 URL 발급",
            description = "S3 Presigned URL을 발급합니다. 프론트엔드는 해당 URL로 직접 PUT 업로드 후 imageUrl을 저장 API로 전달합니다.")
    @GetMapping("/me/profile-image/upload-url")
    public ResponseEntity<StorageDto.PresignedUploadUrlResponse> getProfileImageUploadUrl(
            @LoginUserId Long userId,
            @RequestParam String contentType
    ) {
        return ResponseEntity.ok(userService.generateProfileImagePresignedUrl(userId, contentType));
    }

    @Operation(summary = "프로필 이미지 URL 저장(수정)",
            description = "프론트엔드가 S3 업로드 완료 후 imageUrl을 전달하면 DB에 저장합니다. 기존 이미지는 S3에서 삭제시킵니다.")
    @PutMapping("/me/profile-image")
    public ResponseEntity<UserDto.ProfileImageResponse> updateProfileImage(
            @LoginUserId Long userId,
            @RequestBody @Valid UserDto.ProfileImageUrlRequest request
    ) {
        return ResponseEntity.ok(userService.saveProfileImageUrl(userId, request.imageUrl()));
    }
}
