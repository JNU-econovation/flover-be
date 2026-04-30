package com.flover.flover_be.user.controller;

import com.flover.flover_be.global.auth.LoginUserId;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "닉네임 수정", description = "사용자 닉네임 변경")
    @PutMapping("/me/nickname")
    public ResponseEntity<UserDto.NicknameResponse> updateNickname(
            @LoginUserId Long userId,
            @RequestBody @Valid UserDto.NicknameRequest request
    ) {
        return ResponseEntity.ok(userService.updateNickname(userId, request.nickname()));
    }
}
