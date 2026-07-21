package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.dto.CrewDto;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.plogging.repository.PloggingSessionRepository;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewMemberManagementServiceTest {

    @Mock private CrewRepository crewRepository;
    @Mock private CrewMemberRepository crewMemberRepository;
    @Mock private CrewPloggingSessionRepository crewPloggingSessionRepository;
    @Mock private CrewPloggingParticipantRepository crewPloggingParticipantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PloggingSessionRepository ploggingSessionRepository;
    @Mock private CrewPloggingResponseMapper responseMapper;
    @Mock private CrewPloggingPhotoSummaryReader photoSummaryReader;
    @Mock private CrewJoinCodeGenerator joinCodeGenerator;
    @InjectMocks private CrewService crewService;

    @DisplayName("일반 크루원은 활성 세션이 없으면 자발적으로 탈퇴한다")
    @Test
    void member_withdraws_when_active_session_does_not_exist() {
        CrewMember member = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.empty());
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 2L))
                .willReturn(Optional.of(member));

        crewService.withdraw(2L, 10L);

        verify(member).withdraw(any());
    }

    @DisplayName("크루원 목록은 ACTIVE 회원만 joinedAt 오름차순 조회 결과로 반환한다")
    @Test
    void find_members_returns_active_members_in_repository_order() {
        CrewMember requester = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember activeMember = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        User leaderUser = org.mockito.Mockito.mock(User.class);
        User memberUser = org.mockito.Mockito.mock(User.class);
        given(leader.getUser()).willReturn(leaderUser);
        given(activeMember.getUser()).willReturn(memberUser);
        given(leaderUser.getNickname()).willReturn("크루장");
        given(memberUser.getNickname()).willReturn("크루원");
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(10L, 1L, CrewMemberStatus.ACTIVE))
                .willReturn(Optional.of(requester));
        given(crewMemberRepository.findAllByCrewIdAndStatusOrderByJoinedAtAsc(
                10L, CrewMemberStatus.ACTIVE)).willReturn(List.of(leader, activeMember));

        CrewDto.CrewMemberListResponse response = crewService.findMembers(1L, 10L);

        assertThat(response.members()).extracting(CrewDto.CrewMemberListItemResponse::nickname)
                .containsExactly("크루장", "크루원");
    }

    @DisplayName("같은 크루원의 공개 프로필과 DB 집계 통계를 반환한다")
    @Test
    void find_member_profile_returns_public_fields_and_aggregated_stats() {
        CrewMember requester = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        User targetUser = org.mockito.Mockito.mock(User.class);
        PloggingSessionRepository.PloggingStatsView stats =
                org.mockito.Mockito.mock(PloggingSessionRepository.PloggingStatsView.class);
        given(target.getUser()).willReturn(targetUser);
        given(targetUser.getId()).willReturn(2L);
        given(targetUser.getNickname()).willReturn("대상");
        given(targetUser.getProfileImageUrl()).willReturn("https://example.com/profile.jpg");
        given(targetUser.getLevel()).willReturn(3);
        given(targetUser.getExperience()).willReturn(1440L);
        given(stats.getCount()).willReturn(4L);
        given(stats.getTotalStepCount()).willReturn(12000L);
        given(stats.getTotalDistanceMeters()).willReturn(8000L);
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(10L, 1L, CrewMemberStatus.ACTIVE))
                .willReturn(Optional.of(requester));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(10L, 2L, CrewMemberStatus.ACTIVE))
                .willReturn(Optional.of(target));
        given(ploggingSessionRepository.findStatsByUserId(2L)).willReturn(stats);

        CrewDto.CrewMemberProfileResponse response = crewService.findMemberProfile(1L, 10L, 2L);

        assertThat(response).isEqualTo(new CrewDto.CrewMemberProfileResponse(
                2L, "대상", "https://example.com/profile.jpg", 3, 1440L, 4L, 12000L, 8000L
        ));
    }

    @DisplayName("크루 비회원은 크루원 목록을 조회할 수 없다")
    @Test
    void non_member_cannot_find_members() {
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(10L, 3L, CrewMemberStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> crewService.findMembers(3L, 10L))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("WITHDRAWN 대상의 공개 프로필은 조회할 수 없다")
    @Test
    void withdrawn_member_profile_cannot_be_found() {
        CrewMember requester = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(10L, 1L, CrewMemberStatus.ACTIVE))
                .willReturn(Optional.of(requester));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(10L, 2L, CrewMemberStatus.ACTIVE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> crewService.findMemberProfile(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("크루장은 자발적으로 탈퇴할 수 없다")
    @Test
    void leader_cannot_withdraw() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        givenContextWithoutSession(1L, leader);

        assertThatThrownBy(() -> crewService.withdraw(1L, 10L))
                .isInstanceOf(CrewException.class);
        verify(leader, never()).withdraw(any());
    }

    @DisplayName("모집 중 JOINED 일반 크루원은 자발적 탈퇴 시 참가도 취소한다")
    @Test
    void joined_member_withdraws_from_recruiting_session() {
        CrewMember member = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewPloggingSession session = session(CrewPloggingStatus.RECRUITING);
        CrewPloggingParticipant participant = participant(CrewPloggingParticipantStatus.JOINED);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 2L)).willReturn(Optional.of(member));
        given(crewPloggingParticipantRepository.findBySessionIdAndUserIdForUpdate(20L, 2L))
                .willReturn(Optional.of(participant));

        crewService.withdraw(2L, 10L);

        verify(participant).cancel();
        verify(member).withdraw(any());
    }

    @DisplayName("크루장은 모집 중 JOINED 참가자를 강퇴하면 참가를 취소하고 멤버십을 탈퇴 처리한다")
    @Test
    void leader_removes_joined_member_from_recruiting_session() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewPloggingSession session = session(CrewPloggingStatus.RECRUITING);
        CrewPloggingParticipant participant = participant(CrewPloggingParticipantStatus.JOINED);
        givenRemovalContext(leader, target, session, participant);

        crewService.removeMember(1L, 10L, 2L);

        verify(participant).cancel();
        verify(target).withdraw(any());
    }

    @DisplayName("진행 중인 PARTICIPATING 참가자는 강퇴할 수 없다")
    @Test
    void participating_member_cannot_be_removed() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewPloggingSession session = session(CrewPloggingStatus.IN_PROGRESS);
        CrewPloggingParticipant participant = participant(CrewPloggingParticipantStatus.PARTICIPATING);
        givenRemovalContext(leader, target, session, participant);

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
        verify(target, never()).withdraw(any());
    }

    @DisplayName("제출 유예 중 PARTICIPATING 참가자는 강퇴할 수 없다")
    @Test
    void participating_member_cannot_be_removed_from_completing_session() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewPloggingSession session = session(CrewPloggingStatus.COMPLETING);
        CrewPloggingParticipant participant = participant(CrewPloggingParticipantStatus.PARTICIPATING);
        givenRemovalContext(leader, target, session, participant);

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
        verify(target, never()).withdraw(any());
    }

    @DisplayName("완료 제출한 참가자는 진행 중 세션에서도 강퇴할 수 있다")
    @Test
    void submitted_member_can_be_removed_from_in_progress_session() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewPloggingSession session = session(CrewPloggingStatus.IN_PROGRESS);
        CrewPloggingParticipant participant = participant(CrewPloggingParticipantStatus.SUBMITTED);
        givenRemovalContext(leader, target, session, participant);

        crewService.removeMember(1L, 10L, 2L);

        verify(target).withdraw(any());
        verify(participant, never()).cancel();
    }

    @DisplayName("진행 중 세션에 비정상 JOINED 참가자가 있으면 강퇴하지 않는다")
    @Test
    void joined_member_in_in_progress_session_cannot_be_removed() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        CrewPloggingSession session = session(CrewPloggingStatus.IN_PROGRESS);
        CrewPloggingParticipant participant = participant(CrewPloggingParticipantStatus.JOINED);
        givenRemovalContext(leader, target, session, participant);

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
        verify(target, never()).withdraw(any());
    }

    @DisplayName("일반 크루원은 다른 크루원을 강퇴할 수 없다")
    @Test
    void member_cannot_remove_another_member() {
        CrewMember requester = member(CrewRole.MEMBER, CrewMemberStatus.ACTIVE);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.empty());
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 1L))
                .willReturn(Optional.of(requester));

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
    }

    @DisplayName("이미 탈퇴한 대상에 대한 중복 강퇴는 충돌로 거절한다")
    @Test
    void withdrawn_member_cannot_be_removed_again() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember target = member(CrewRole.MEMBER, CrewMemberStatus.WITHDRAWN);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.empty());
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 1L)).willReturn(Optional.of(leader));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 2L)).willReturn(Optional.of(target));

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
        verify(target, never()).withdraw(any());
    }

    @DisplayName("크루장은 강퇴 대상이 될 수 없다")
    @Test
    void leader_cannot_be_removed() {
        CrewMember requester = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        CrewMember targetLeader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.empty());
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 1L)).willReturn(Optional.of(requester));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 2L)).willReturn(Optional.of(targetLeader));

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 2L))
                .isInstanceOf(CrewException.class);
        verify(targetLeader, never()).withdraw(any());
    }

    @DisplayName("크루장은 자신을 강퇴할 수 없다")
    @Test
    void leader_cannot_remove_self() {
        CrewMember leader = member(CrewRole.LEADER, CrewMemberStatus.ACTIVE);
        givenContextWithoutSession(1L, leader);

        assertThatThrownBy(() -> crewService.removeMember(1L, 10L, 1L))
                .isInstanceOf(CrewException.class);
        verify(leader, never()).withdraw(any());
    }

    private void givenContextWithoutSession(Long userId, CrewMember member) {
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.empty());
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, userId))
                .willReturn(Optional.of(member));
    }

    private void givenRemovalContext(
            CrewMember leader,
            CrewMember target,
            CrewPloggingSession session,
            CrewPloggingParticipant participant
    ) {
        given(crewRepository.findByIdForUpdate(10L)).willReturn(Optional.of(org.mockito.Mockito.mock(Crew.class)));
        given(crewPloggingSessionRepository.findActiveByCrewIdForUpdate(any(), any()))
                .willReturn(Optional.of(session));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 1L)).willReturn(Optional.of(leader));
        given(crewMemberRepository.findByCrewIdAndUserIdForUpdate(10L, 2L)).willReturn(Optional.of(target));
        given(crewPloggingParticipantRepository.findBySessionIdAndUserIdForUpdate(20L, 2L))
                .willReturn(Optional.of(participant));
    }

    private CrewMember member(CrewRole role, CrewMemberStatus status) {
        CrewMember member = org.mockito.Mockito.mock(CrewMember.class);
        org.mockito.Mockito.lenient().when(member.getRole()).thenReturn(role);
        org.mockito.Mockito.lenient().when(member.getStatus()).thenReturn(status);
        return member;
    }

    private CrewPloggingSession session(CrewPloggingStatus status) {
        CrewPloggingSession session = org.mockito.Mockito.mock(CrewPloggingSession.class);
        given(session.getId()).willReturn(20L);
        given(session.getStatus()).willReturn(status);
        return session;
    }

    private CrewPloggingParticipant participant(CrewPloggingParticipantStatus status) {
        CrewPloggingParticipant participant = org.mockito.Mockito.mock(CrewPloggingParticipant.class);
        given(participant.getStatus()).willReturn(status);
        return participant;
    }
}
