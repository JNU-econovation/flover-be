package com.plover.plover_be.user.service;

import com.plover.plover_be.crew.service.CrewDeletionService;
import com.plover.plover_be.global.storage.StorageDto;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.dto.UserDto;
import com.plover.plover_be.user.exception.UserErrorCode;
import com.plover.plover_be.user.exception.UserException;
import com.plover.plover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProfileImageStorageService profileImageStorageService;
    @Mock private UserPloggingPort userPloggingPort;
    @Mock private CrewDeletionService crewDeletionService;
    @InjectMocks private UserService userService;

    private static User kakaoUser(String nickname, String imageUrl) {
        return User.create(OAuthProvider.KAKAO, "12345", "test@test.com", nickname, imageUrl);
    }

    // ──────────────── upsertOAuthUser ────────────────

    @DisplayName("신규 OAuth 유저는 DB에 저장하고 반환한다")
    @Test
    void upsert_oauth_user_신규_유저_저장() {
        // given
        given(userRepository.findByProviderAndProviderId(OAuthProvider.APPLE, "apple.user.001"))
                .willReturn(Optional.empty());
        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        User result = userService.upsertOAuthUser(OAuthProvider.APPLE, "apple.user.001", "user@apple.com");

        // then
        assertThat(result.getProvider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(result.getProviderId()).isEqualTo("apple.user.001");
        assertThat(result.getNickname()).matches("플러버\\d{6}");
        verify(userRepository).save(any(User.class));
    }

    @DisplayName("기존 OAuth 유저는 저장 없이 반환한다")
    @Test
    void upsert_oauth_user_기존_유저_반환() {
        // given
        User existing = User.create(OAuthProvider.APPLE, "apple.user.001", "user@apple.com", "플러버111111", null);
        given(userRepository.findByProviderAndProviderId(OAuthProvider.APPLE, "apple.user.001"))
                .willReturn(Optional.of(existing));

        // when
        User result = userService.upsertOAuthUser(OAuthProvider.APPLE, "apple.user.001", "other@apple.com");

        // then
        assertThat(result.getNickname()).isEqualTo("플러버111111");
        verify(userRepository, never()).save(any());
    }

    @DisplayName("기본 닉네임이 중복이면 재생성한다")
    @Test
    void upsert_oauth_user_닉네임_중복_재생성() {
        // given
        given(userRepository.findByProviderAndProviderId(OAuthProvider.APPLE, "apple.user.001"))
                .willReturn(Optional.empty());
        given(userRepository.existsByNickname(anyString())).willReturn(true).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        userService.upsertOAuthUser(OAuthProvider.APPLE, "apple.user.001", null);

        // then
        verify(userRepository, times(2)).existsByNickname(anyString());
    }

    // ──────────────── findUserInfo ────────────────

    @DisplayName("유저 정보를 정상적으로 조회한다")
    @Test
    void find_user_info_성공() {
        // given
        Long userId = 1L;
        User user = kakaoUser("닉네임", "https://img.url/profile.png");
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

    // ──────────────── updateNickname ────────────────

    @DisplayName("닉네임을 정상적으로 변경한다")
    @Test
    void update_nickname_성공() {
        // given
        Long userId = 1L;
        User user = kakaoUser("기존닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);

        // when
        UserDto.NicknameResponse result = userService.updateNickname(userId, "새닉네임");

        // then
        assertThat(result.nickname()).isEqualTo("새닉네임");
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
        User user = kakaoUser("기존닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(userId, "중복닉네임"))
                .isInstanceOf(UserException.class);
    }

    @DisplayName("현재 닉네임과 동일한 닉네임으로 변경 시 중복 검사를 건너뛴다")
    @Test
    void update_nickname_같은_닉네임이면_중복_검사_건너뜀() {
        // given
        Long userId = 1L;
        User user = kakaoUser("기존닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateNickname(userId, "기존닉네임");

        // then
        verify(userRepository, never()).existsByNickname(anyString());
    }

    // ──────────────── generateProfileImagePresignedUrl ────────────────

    @DisplayName("Presigned URL 발급 시 유저 존재 확인 후 URL을 반환한다")
    @Test
    void generate_profile_image_presigned_url_성공() {
        // given
        Long userId = 1L;
        String contentType = "image/png";
        String uploadUrl = "https://bucket.s3.amazonaws.com/presigned";
        String imageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";
        User user = kakaoUser("nickname", null);
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

    // ──────────────── saveProfileImageUrl ────────────────

    @DisplayName("기존 프로필 이미지가 없을 때 새 URL을 저장한다")
    @Test
    void save_profile_image_url_기존없음_성공() {
        // given
        Long userId = 1L;
        String newImageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";
        User user = kakaoUser("nickname", null);
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
        User user = kakaoUser("nickname", oldImageUrl);
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
        User user = kakaoUser("nickname", sameUrl);
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

    // ──────────────── findPloggingStats ────────────────

    @DisplayName("플로깅 기록이 있는 사용자의 누적 통계를 반환한다")
    @Test
    void find_plogging_stats_성공() {
        // given
        Long userId = 1L;
        User user = kakaoUser("닉네임", null);
        UserPloggingPort.PloggingStats stats = new UserPloggingPort.PloggingStats(3L, 12000L, 8500L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userPloggingPort.findStats(userId)).willReturn(stats);

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
        User user = kakaoUser("닉네임", null);
        UserPloggingPort.PloggingStats stats = new UserPloggingPort.PloggingStats(0L, 0L, 0L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userPloggingPort.findStats(userId)).willReturn(stats);

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

    // ──────────────── deleteUser ────────────────

    @DisplayName("프로필 이미지가 있는 유저 탈퇴 시 DB 삭제 완료 후 트랜잭션 커밋 시점에 S3 이미지를 삭제한다")
    @Test
    void delete_user_프로필이미지있음_DB삭제_후_커밋시점에_S3삭제() {
        // given
        Long userId = 1L;
        String profileImageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/img.png";
        User user = kakaoUser("닉네임", profileImageUrl);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        try (MockedStatic<TransactionSynchronizationManager> txSync = mockStatic(TransactionSynchronizationManager.class)) {
            ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);

            // when
            userService.deleteUser(userId);

            // then - 크루 및 플로깅 데이터 정리 후 사용자를 삭제하는지 확인
            InOrder inOrder = inOrder(crewDeletionService, userPloggingPort, userRepository);
            inOrder.verify(crewDeletionService).deleteOwnedCrewsAndUserReferences(userId);
            inOrder.verify(userPloggingPort).deleteByUserId(userId);
            inOrder.verify(userRepository).delete(user);

            // S3 삭제는 커밋 이후에 실행되도록 등록됐는지 확인
            txSync.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            verify(profileImageStorageService, never()).deleteIfOwnedByBucket(any());

            // 커밋 시점 시뮬레이션 → afterCommit 호출 시 S3 삭제 실행
            syncCaptor.getValue().afterCommit();
            verify(profileImageStorageService).deleteIfOwnedByBucket(profileImageUrl);
        }
    }

    @DisplayName("프로필 이미지가 없는 유저 탈퇴 시 S3 삭제 없이 데이터를 제거한다")
    @Test
    void delete_user_프로필이미지없음_S3삭제없이_데이터삭제() {
        // given
        Long userId = 1L;
        User user = kakaoUser("닉네임", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.deleteUser(userId);

        // then
        verify(profileImageStorageService, never()).deleteIfOwnedByBucket(any());
        verify(crewDeletionService).deleteOwnedCrewsAndUserReferences(userId);
        verify(userPloggingPort).deleteByUserId(userId);
        verify(userRepository).delete(user);
    }

    @DisplayName("존재하지 않는 유저 탈퇴 시 예외가 발생하고 삭제 로직이 실행되지 않는다")
    @Test
    void delete_user_유저없음_예외() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        verify(userRepository, never()).delete(any(User.class));
    }
    @DisplayName("크루장도 소유 크루 정리 후 계정을 삭제할 수 있다")
    @Test
    void delete_user_allows_crew_leader_after_crew_cleanup() {
        // given
        Long userId = 1L;
        User user = kakaoUser("크루장", null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        // when
        userService.deleteUser(userId);

        // then
        verify(crewDeletionService).deleteOwnedCrewsAndUserReferences(userId);
        verify(userRepository).delete(user);
    }
}
