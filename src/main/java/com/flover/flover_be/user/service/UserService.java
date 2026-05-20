package com.flover.flover_be.user.service;

import com.flover.flover_be.global.storage.StorageDto;
import com.flover.flover_be.plogging.repository.PloggingPhotoRepository;
import com.flover.flover_be.plogging.repository.PloggingRoutePointRepository;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository;
import com.flover.flover_be.user.domain.OAuthProvider;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.exception.UserErrorCode;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileImageStorageService profileImageStorageService;
    private final PloggingSessionRepository ploggingSessionRepository;
    private final PloggingPhotoRepository ploggingPhotoRepository;
    private final PloggingRoutePointRepository ploggingRoutePointRepository;

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

    @Transactional
    public User upsertOAuthUser(OAuthProvider provider, String providerId, String email) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    String nickname = generateDefaultNickname();
                    return userRepository.save(User.create(provider, providerId, email, nickname, null));
                });
    }

    private String generateDefaultNickname() {
        String nickname;
        do {
            int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
            nickname = "플러버" + suffix;
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserOrThrow(userId);
        String profileImageUrl = user.getProfileImageUrl();

        ploggingPhotoRepository.deleteByPloggingSessionUserId(userId);
        ploggingRoutePointRepository.deleteByPloggingSessionUserId(userId);
        ploggingSessionRepository.deleteByUserId(userId);
        userRepository.delete(user);

        if (profileImageUrl != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    profileImageStorageService.deleteIfOwnedByBucket(profileImageUrl);
                }
            });
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
