package com.flover.flover_be.user.service;

import com.flover.flover_be.global.storage.StorageDto;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.exception.UserErrorCode;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileImageStorageService profileImageStorageService;

    @Transactional
    public UserDto.NicknameResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (!nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
            throw new UserException(UserErrorCode.NICKNAME_ALREADY_USED);
        }

        user.updateNickname(nickname);
        return new UserDto.NicknameResponse(user.getId(), user.getNickname());
    }

    public StorageDto.PresignedUploadUrlResponse generateProfileImagePresignedUrl(Long userId, String contentType) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return profileImageStorageService.generatePresignedUploadUrl(userId, contentType);
    }

    @Transactional
    public UserDto.ProfileImageResponse saveProfileImageUrl(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        profileImageStorageService.deleteIfOwnedByBucket(user.getProfileImageUrl());
        user.updateProfileImage(imageUrl);
        return new UserDto.ProfileImageResponse(user.getId(), user.getProfileImageUrl());
    }
}
