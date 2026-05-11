package com.flover.flover_be.user.service;

import com.flover.flover_be.global.storage.StorageDto;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository;
import com.flover.flover_be.plogging.repository.PloggingSessionRepository.PloggingStatsView;
import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.exception.UserErrorCode;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProfileImageStorageService profileImageStorageService;
    @Mock private PloggingSessionRepository ploggingSessionRepository;
    @InjectMocks private UserService userService;

    @DisplayName("유저 정보를 정상적으로 조회한다")
    @Test
    void find_user_info_성공() {
        // given
        Long userId = 1L;
        User user = User.create(1L, "test@test.com", "닉네임", "https://img.url/profile.png");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserDto.UserInfoResponse result = userService.findUserInfo(userId);

        // then
        assertThat(result.nickname()).isEqualTo("닉네임");
        assertThat(result.level()).isEqualTo(1);
        assertThat(result.title()).isEqualTo("쓰봉이");
        assertThat(result.profileImageUrl()).isEqualTo("https://img.url/profile.png");
        assertThat(result.experience()).isEqualTo(0L);
    }

    @DisplayName("존재하지 않는 유저 정보 조회 시 예외가 발생한다")
    @Test
    void find_user_info_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUserInfo(1L))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("닉네임을 정상적으로 변경한다")
    @Test
    void update_nickname_성공() {
        // given
        Long userId = 1L;
        String newNickname = "새닉네임";
        User user = User.create(1L, "test@test.com", "기존닉네임", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(newNickname)).willReturn(false);

        // when
        UserDto.NicknameResponse result = userService.updateNickname(userId, newNickname);

        // then
        assertThat(result.nickname()).isEqualTo(newNickname);
    }

    @DisplayName("존재하지 않는 유저의 닉네임 변경 시 예외가 발생한다")
    @Test
    void update_nickname_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUserInfo(1L))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @DisplayName("이미 사용 중인 닉네임으로 변경 시 예외가 발생한다")
    @Test
    void update_nickname_중복_닉네임_예외() {
        // given
        Long userId = 1L;
        String duplicateNickname = "중복닉네임";
        User user = User.create(1L, "test@test.com", "기존닉네임", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(duplicateNickname)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(userId, duplicateNickname))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("현재 닉네임과 동일한 닉네임으로 변경 시 중복 검사를 건너뛴다")
    @Test
    void update_nickname_같은_닉네임이면_중복_검사_건너뜀() {
        // given
        Long userId = 1L;
        String sameNickname = "기존닉네임";
        User user = User.create(1L, "test@test.com", sameNickname, null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateNickname(userId, sameNickname);

        // then
        verify(userRepository, never()).existsByNickname(anyString());
    }

    @DisplayName("Presigned URL 발급 시 유저 존재 확인 후 URL을 반환한다")
    @Test
    void generate_profile_image_presigned_url_성공() {
        // given
        Long userId = 1L;
        String contentType = "image/png";
        String uploadUrl = "https://bucket.s3.amazonaws.com/presigned";
        String imageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";
        User user = User.create(1L, "test@test.com", "nickname", null);
        StorageDto.PresignedUploadUrlResponse expected = new StorageDto.PresignedUploadUrlResponse(uploadUrl, imageUrl);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(profileImageStorageService.generatePresignedUploadUrl(userId, contentType)).willReturn(expected);

        // when
        StorageDto.PresignedUploadUrlResponse result = userService.generateProfileImagePresignedUrl(userId, contentType);

        // then
        assertThat(result.uploadUrl()).isEqualTo(uploadUrl);
        assertThat(result.objectUrl()).isEqualTo(imageUrl);
    }

    @DisplayName("Presigned URL 발급 시 유저가 없으면 예외가 발생한다")
    @Test
    void generate_profile_image_presigned_url_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.generateProfileImagePresignedUrl(1L, "image/png"))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("기존 프로필 이미지가 없을 때 새 URL을 저장한다")
    @Test
    void save_profile_image_url_기존없음_성공() {
        // given
        Long userId = 1L;
        String newImageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";
        User user = User.create(1L, "test@test.com", "nickname", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserDto.ProfileImageResponse result = userService.saveProfileImageUrl(userId, newImageUrl);

        // then
        assertThat(result.profileImageUrl()).isEqualTo(newImageUrl);
        assertThat(user.getProfileImageUrl()).isEqualTo(newImageUrl);
        verify(profileImageStorageService, never()).deleteIfOwnedByBucket(any());
    }

    @DisplayName("기존 프로필 이미지가 있으면 교체 시 삭제를 요청한다")
    @Test
    void save_profile_image_url_기존있음_삭제후_저장() {
        // given
        Long userId = 1L;
        String oldImageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/old.png";
        String newImageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/new.png";
        User user = User.create(1L, "test@test.com", "nickname", oldImageUrl);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.saveProfileImageUrl(userId, newImageUrl);

        // then
        verify(profileImageStorageService).deleteIfOwnedByBucket(oldImageUrl);
        assertThat(user.getProfileImageUrl()).isEqualTo(newImageUrl);
    }

    @DisplayName("동일한 URL로 교체 시 S3 삭제를 요청하지 않는다")
    @Test
    void save_profile_image_url_동일_URL_삭제_안함() {
        // given
        Long userId = 1L;
        String sameUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";
        User user = User.create(1L, "test@test.com", "nickname", sameUrl);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.saveProfileImageUrl(userId, sameUrl);

        // then
        verify(profileImageStorageService, never()).deleteIfOwnedByBucket(any());
    }

    @DisplayName("프로필 이미지 URL 저장 시 유저가 없으면 예외가 발생한다")
    @Test
    void save_profile_image_url_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.saveProfileImageUrl(1L, "https://example.com/img.png"))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("플로깅 기록이 있는 사용자의 누적 통계를 반환한다")
    @Test
    void find_plogging_stats_성공() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        PloggingStatsView stats = mockStats(3L, 12000L, 8500L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findStatsByUserId(userId)).willReturn(stats);

        // when
        UserDto.PloggingStatsResponse result = userService.findPloggingStats(userId);

        // then
        assertThat(result.totalPloggingCount()).isEqualTo(3L);
        assertThat(result.totalStepCount()).isEqualTo(12000L);
        assertThat(result.totalDistanceMeters()).isEqualTo(8500L);
    }

    @DisplayName("플로깅 기록이 없는 사용자는 모든 통계가 0이다")
    @Test
    void find_plogging_stats_기록없음_모두_0() {
        // given
        Long userId = 1L;
        User user = User.create(12345L, "test@test.com", "닉네임", null);
        PloggingStatsView stats = mockStats(0L, 0L, 0L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ploggingSessionRepository.findStatsByUserId(userId)).willReturn(stats);

        // when
        UserDto.PloggingStatsResponse result = userService.findPloggingStats(userId);

        // then
        assertThat(result.totalPloggingCount()).isZero();
        assertThat(result.totalStepCount()).isZero();
        assertThat(result.totalDistanceMeters()).isZero();
    }

    @DisplayName("존재하지 않는 유저의 플로깅 통계 조회 시 예외가 발생한다")
    @Test
    void find_plogging_stats_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findPloggingStats(1L))
                .isInstanceOf(UserException.class);
    }

    private PloggingStatsView mockStats(long count, long totalStepCount, long totalDistanceMeters) {
        return new PloggingStatsView() {
            public long getCount() { return count; }
            public long getTotalStepCount() { return totalStepCount; }
            public long getTotalDistanceMeters() { return totalDistanceMeters; }
        };
    }
}
