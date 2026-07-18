package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.dto.CrewPloggingDto;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewPloggingServiceTest {

    @Mock private CrewRepository crewRepository;
    @Mock private CrewMemberRepository crewMemberRepository;
    @Mock private CrewPloggingSessionRepository sessionRepository;
    @Mock private CrewPloggingParticipantRepository participantRepository;
    @Mock private CrewPloggingResponseMapper responseMapper;
    @Mock private CrewPloggingFinalizer finalizer;
    @InjectMocks private CrewPloggingService crewPloggingService;

    @DisplayName("크루장이 세션을 생성하면 자동으로 참가자에 등록된다")
    @Test
    void create_session_registers_leader_as_participant() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingDto.SessionResponse expected = sessionResponse(CrewPloggingStatus.RECRUITING);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(crew));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                10L, userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(sessionRepository.findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .willReturn(Optional.empty());
        given(sessionRepository.save(any(CrewPloggingSession.class))).willAnswer(i -> i.getArgument(0));
        given(responseMapper.toSessionResponse(any(), any())).willReturn(expected);

        // when
        CrewPloggingDto.SessionResponse response = crewPloggingService.createSession(userId, 10L);

        // then
        verify(participantRepository).save(any(CrewPloggingParticipant.class));
        assertThat(response).isEqualTo(expected);
        ArgumentCaptor<Collection<CrewPloggingStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(sessionRepository).findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(10L), statuses.capture());
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(
                CrewPloggingStatus.RECRUITING,
                CrewPloggingStatus.IN_PROGRESS,
                CrewPloggingStatus.COMPLETING
        ).doesNotContain(CrewPloggingStatus.CANCELED);
    }

    @DisplayName("COMPLETING 세션도 활성 세션이므로 새 세션을 만들지 않는다")
    @Test
    void create_session_returns_existing_completing_session() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession active = CrewPloggingSession.create(crew);
        active.start(java.time.LocalDateTime.now());
        active.end(java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusHours(24));
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(crew));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                10L, userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(sessionRepository.findFirstByCrewIdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .willReturn(Optional.of(active));
        given(responseMapper.toSessionResponse(active, userId))
                .willReturn(sessionResponse(CrewPloggingStatus.COMPLETING));

        // when
        crewPloggingService.createSession(userId, 10L);

        // then
        verify(sessionRepository, never()).save(any());
        verify(participantRepository, never()).save(any());
    }

    @DisplayName("일반 크루원은 같이 플로깅 세션을 생성할 수 없다")
    @Test
    void create_session_rejects_non_leader() {
        // given
        Long userId = 1L;
        User user = user("크루원");
        Crew crew = Crew.create("크루", "A1B2C3D4", user("크루장"));
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(crew));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                10L, userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> crewPloggingService.createSession(userId, 10L))
                .isInstanceOf(CrewException.class);
        verify(sessionRepository, never()).save(any());
    }

    @DisplayName("크루장이 시작하면 공통 서버 시작 시각과 참가 상태가 갱신된다")
    @Test
    void start_session_starts_joined_participants() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(session, leader, true);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(participant));
        given(responseMapper.toSessionResponse(session, userId))
                .willReturn(sessionResponse(CrewPloggingStatus.IN_PROGRESS));

        // when
        crewPloggingService.startSession(userId, 10L);

        // then
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(participant.getStatus().name()).isEqualTo("PARTICIPATING");
    }

    @DisplayName("크루장은 RECRUITING 세션과 참가자 전체를 취소한다")
    @Test
    void cancel_session_cancels_recruiting_session_and_participants() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        CrewPloggingParticipant leaderParticipant = CrewPloggingParticipant.create(session, leader, true);
        CrewPloggingParticipant memberParticipant = CrewPloggingParticipant.create(session, user("크루원"), false);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(10L))
                .willReturn(List.of(leaderParticipant, memberParticipant));
        given(responseMapper.toSessionResponse(session, userId))
                .willReturn(sessionResponse(CrewPloggingStatus.CANCELED));

        // when
        CrewPloggingDto.SessionResponse response = crewPloggingService.cancelSession(userId, 10L);

        // then
        assertThat(response.status()).isEqualTo(CrewPloggingStatus.CANCELED);
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.CANCELED);
        assertThat(session.getCanceledAt()).isNotNull();
        assertThat(leaderParticipant.getStatus().name()).isEqualTo("CANCELED");
        assertThat(memberParticipant.getStatus().name()).isEqualTo("CANCELED");
    }

    @DisplayName("이미 취소한 세션의 재취소는 변경 없이 성공한다")
    @Test
    void cancel_session_is_idempotent_when_already_canceled() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        session.cancel(java.time.LocalDateTime.now());
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(responseMapper.toSessionResponse(session, userId))
                .willReturn(sessionResponse(CrewPloggingStatus.CANCELED));

        // when
        crewPloggingService.cancelSession(userId, 10L);

        // then
        verify(participantRepository, never()).findAllByCrewPloggingSessionIdOrderByJoinedAtAsc(10L);
    }

    @DisplayName("일반 크루원은 세션 전체를 취소할 수 없다")
    @Test
    void cancel_session_rejects_non_leader() {
        // given
        Long userId = 2L;
        User leader = user("크루장");
        User user = user("크루원");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> crewPloggingService.cancelSession(userId, 10L))
                .isInstanceOf(CrewException.class);
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.RECRUITING);
    }

    @DisplayName("크루 비회원은 세션 전체를 취소할 수 없다")
    @Test
    void cancel_session_rejects_non_member() {
        // given
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), 3L, CrewMemberStatus.ACTIVE)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> crewPloggingService.cancelSession(3L, 10L))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("RECRUITING 이외의 미취소 세션은 전체 취소할 수 없다")
    @ParameterizedTest
    @EnumSource(value = CrewPloggingStatus.class, names = {"IN_PROGRESS", "COMPLETING", "COMPLETED"})
    void cancel_session_rejects_non_recruiting_status(CrewPloggingStatus status) {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession session = sessionWithStatus(crew, status);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> crewPloggingService.cancelSession(userId, 10L))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("취소가 먼저 확정된 세션은 동시에 도착한 시작 요청으로 되돌릴 수 없다")
    @Test
    void canceled_session_cannot_be_started() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        session.cancel(java.time.LocalDateTime.now());
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> crewPloggingService.startSession(userId, 10L))
                .isInstanceOf(CrewException.class);
        assertThat(session.getStatus()).isEqualTo(CrewPloggingStatus.CANCELED);
    }

    @DisplayName("전체 종료 시 전원이 이미 제출했으면 즉시 완료한다")
    @Test
    void end_session_finalizes_when_everyone_already_submitted() {
        // given
        Long userId = 1L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession session = sessionWithStatus(crew, CrewPloggingStatus.IN_PROGRESS);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusIn(any(), any())).willReturn(0L);

        // when
        crewPloggingService.endSession(userId, 10L);

        // then
        verify(finalizer).complete(org.mockito.ArgumentMatchers.eq(session),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class));
    }

    private CrewPloggingSession sessionWithStatus(Crew crew, CrewPloggingStatus status) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        if (status == CrewPloggingStatus.RECRUITING) {
            return session;
        }
        session.start(now.minusHours(1));
        if (status == CrewPloggingStatus.IN_PROGRESS) {
            return session;
        }
        session.end(now.minusMinutes(5), now.plusHours(24));
        if (status == CrewPloggingStatus.COMPLETING) {
            return session;
        }
        session.complete(now, 1);
        return session;
    }

    private User user(String nickname) {
        return User.create(OAuthProvider.KAKAO, nickname, "test@test.com", nickname, null);
    }

    private CrewPloggingDto.SessionResponse sessionResponse(CrewPloggingStatus status) {
        return new CrewPloggingDto.SessionResponse(
                null, status, null, null, null, true, null, false, 1, false
        );
    }
}
