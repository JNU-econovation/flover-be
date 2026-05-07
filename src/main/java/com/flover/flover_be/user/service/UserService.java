package com.flover.flover_be.user.service;

import com.flover.flover_be.global.storage.StorageDto;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository;
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
    private final PloggingSessionRepository ploggingSessionRepository;

    @Transactional(readOnly = true)
    public UserDto.UserInfoResponse findUserInfo(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserDto.UserInfoResponse(user.getNickname(), user.getLevel(), user.getTitle().getDisplayName(), user.getProfileImageUrl(), user.getExperience());
    }

    @Transactional
    public UserDto.NicknameResponse updateNickname(Long userId, String nickname) {
        User user = getUserOrThrow(userId);

        if (!nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
            throw new UserException(UserErrorCode.NICKNAME_ALREADY_USED);
        }

        user.updateNickname(nickname);
        return new UserDto.NicknameResponse(user.getId(), user.getNickname());
    }

    @Transactional(readOnly = true)
    public StorageDto.PresignedUploadUrlResponse generateProfileImagePresignedUrl(Long userId, String contentType) {
        getUserOrThrow(userId);
        return profileImageStorageService.generatePresignedUploadUrl(userId, contentType);
    }

    @Transactional(readOnly = true)
    public UserDto.PloggingStatsResponse findPloggingStats(Long userId) {
        getUserOrThrow(userId);
        PloggingSessionRepository.PloggingStatsView stats = ploggingSessionRepository.findStatsByUserId(userId);
        return new UserDto.PloggingStatsResponse(stats.getCount(), stats.getTotalStepCount(), stats.getTotalDistanceMeters());
    }

    @Transactional
    public UserDto.ProfileImageResponse saveProfileImageUrl(Long userId, String imageUrl) {
        User user = getUserOrThrow(userId);
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals(imageUrl)) {
            profileImageStorageService.deleteIfOwnedByBucket(user.getProfileImageUrl());
        }
        user.updateProfileImage(imageUrl);
        return new UserDto.ProfileImageResponse(user.getId(), user.getProfileImageUrl());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
