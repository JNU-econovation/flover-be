package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewMemberStatus;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingParticipantStatus;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.domain.PloggingSession;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.plogging.service.PloggingRecordWriter;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CrewPloggingCompletionProcessorTest {

    @Mock private CrewPloggingSessionRepository sessionRepository;
    @Mock private CrewPloggingParticipantRepository participantRepository;
    @Mock private CrewMemberRepository crewMemberRepository;
    @Mock private PloggingRecordWriter ploggingRecordWriter;
    @Mock private CrewPloggingFinalizer finalizer;
    @InjectMocks private CrewPloggingCompletionProcessor completionProcessor;

    @DisplayName("COMPLETING 참가자의 개인 기록을 저장하고 대표 후보로 연결한다")
    @Test
    void complete_links_personal_record_and_representative() {
        // given
        Long userId = 1L;
        Long crewSessionId = 10L;
        User user = user("참가자");
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        CrewPloggingSession crewSession = completingSession(crew);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, user, false);
        participant.start();
        PloggingDto.CompleteRequest request = request(crewSessionId);
        PloggingSession personalRecord = personalRecord(user, request);
        PloggingDto.CompleteResponse expected = new PloggingDto.CompleteResponse(20L, 0, 720, 1, 2);

        given(sessionRepository.findByIdForUpdate(crewSessionId)).willReturn(Optional.of(crewSession));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.findBySessionIdAndUserIdForUpdate(crewSessionId, userId))
                .willReturn(Optional.of(participant));
        given(ploggingRecordWriter.save(user, request, crewSession))
                .willReturn(new PloggingRecordWriter.SavedPloggingRecord(personalRecord, expected));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusIn(crewSessionId, List.of(
                CrewPloggingParticipantStatus.JOINED,
                CrewPloggingParticipantStatus.PARTICIPATING
        ))).willReturn(1L);

        // when
        PloggingDto.CompleteResponse response = completionProcessor.complete(userId, request);

        // then
        assertThat(response).isEqualTo(expected);
        assertThat(participant.getPloggingSession()).isEqualTo(personalRecord);
        assertThat(crewSession.getRepresentativePloggingSession()).isEqualTo(personalRecord);
        assertThat(crewSession.getRepresentativeStepCountSnapshot()).isEqualTo(request.stepCount());
    }

    @DisplayName("IN_PROGRESS 참가자는 개인 기록을 제출해도 다른 참가자가 있으면 세션을 계속 진행한다")
    @Test
    void complete_early_submission_keeps_session_in_progress() {
        // given
        Long userId = 1L;
        Long crewSessionId = 10L;
        User user = user("참가자");
        Crew crew = Crew.create("크루", "A1B2C3D4", user("크루장"));
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        CrewPloggingSession crewSession = inProgressSession(crew);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, user, false);
        participant.start();
        PloggingDto.CompleteRequest request = request(crewSessionId);
        PloggingSession personalRecord = personalRecord(user, request);
        PloggingDto.CompleteResponse expected = new PloggingDto.CompleteResponse(20L, 0, 720, 1, 2);
        stubCompletion(userId, crewSessionId, user, crew, member, crewSession, participant,
                request, personalRecord, expected, 1L);

        // when
        PloggingDto.CompleteResponse response = completionProcessor.complete(userId, request);

        // then
        assertThat(response).isEqualTo(expected);
        assertThat(participant.getStatus()).isEqualTo(CrewPloggingParticipantStatus.SUBMITTED);
        assertThat(participant.getPloggingSession()).isSameAs(personalRecord);
        assertThat(participant.getSubmittedAt()).isNotNull();
        assertThat(crewSession.getStatus()).isEqualTo(CrewPloggingStatus.IN_PROGRESS);
        verify(finalizer, never()).complete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("크루장의 IN_PROGRESS 개인 완료 기록은 기존 참가자 기록보다 대표로 우선한다")
    @Test
    void leader_early_submission_becomes_representative_without_ending_session() {
        // given
        Long userId = 1L;
        Long crewSessionId = 10L;
        User leader = user("크루장");
        User memberUser = user("먼저 제출한 크루원");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession crewSession = inProgressSession(crew);
        PloggingDto.CompleteRequest request = request(crewSessionId);
        crewSession.selectRepresentative(personalRecord(memberUser, request), 2L, memberUser.getNickname());
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, leader, true);
        participant.start();
        PloggingSession leaderRecord = personalRecord(leader, request);
        PloggingDto.CompleteResponse expected = new PloggingDto.CompleteResponse(20L, 0, 720, 1, 2);
        stubCompletion(userId, crewSessionId, leader, crew, member, crewSession, participant,
                request, leaderRecord, expected, 1L);

        // when
        completionProcessor.complete(userId, request);

        // then
        assertThat(crewSession.getRepresentativePloggingSession()).isSameAs(leaderRecord);
        assertThat(crewSession.getStatus()).isEqualTo(CrewPloggingStatus.IN_PROGRESS);
    }

    @DisplayName("IN_PROGRESS에서 마지막 참가자가 제출하면 세션 최종화를 한 번 요청한다")
    @Test
    void last_early_submission_finalizes_session() {
        // given
        Long userId = 1L;
        Long crewSessionId = 10L;
        User leader = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", leader);
        CrewMember member = CrewMember.create(crew, leader, CrewRole.LEADER);
        CrewPloggingSession crewSession = inProgressSession(crew);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, leader, true);
        participant.start();
        PloggingDto.CompleteRequest request = request(crewSessionId);
        PloggingSession record = personalRecord(leader, request);
        PloggingDto.CompleteResponse expected = new PloggingDto.CompleteResponse(20L, 0, 720, 1, 2);
        stubCompletion(userId, crewSessionId, leader, crew, member, crewSession, participant,
                request, record, expected, 0L);

        // when
        completionProcessor.complete(userId, request);

        // then
        verify(finalizer).complete(org.mockito.ArgumentMatchers.eq(crewSession),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @DisplayName("전원 제출되면 크루 세션을 즉시 완료한다")
    @Test
    void complete_finalizes_when_everyone_submitted() {
        // given
        Long userId = 1L;
        Long crewSessionId = 10L;
        User user = user("크루장");
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.LEADER);
        CrewPloggingSession crewSession = completingSession(crew);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, user, true);
        participant.start();
        PloggingDto.CompleteRequest request = request(crewSessionId);
        PloggingSession personalRecord = personalRecord(user, request);
        PloggingDto.CompleteResponse response = new PloggingDto.CompleteResponse(20L, 0, 720, 1, 2);
        given(sessionRepository.findByIdForUpdate(crewSessionId)).willReturn(Optional.of(crewSession));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.findBySessionIdAndUserIdForUpdate(crewSessionId, userId))
                .willReturn(Optional.of(participant));
        given(ploggingRecordWriter.save(user, request, crewSession))
                .willReturn(new PloggingRecordWriter.SavedPloggingRecord(personalRecord, response));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusIn(crewSessionId, List.of(
                CrewPloggingParticipantStatus.JOINED,
                CrewPloggingParticipantStatus.PARTICIPATING
        ))).willReturn(0L);

        // when
        completionProcessor.complete(userId, request);

        // then
        verify(finalizer).complete(org.mockito.ArgumentMatchers.eq(crewSession), org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @DisplayName("이미 제출한 참가자의 중복 완료 요청은 개인 기록을 다시 저장하지 않는다")
    @Test
    void complete_rejects_duplicate_submission() {
        // given
        Long userId = 1L;
        Long crewSessionId = 10L;
        User user = user("참가자");
        Crew crew = Crew.create("크루", "A1B2C3D4", user);
        CrewMember member = CrewMember.create(crew, user, CrewRole.MEMBER);
        CrewPloggingSession crewSession = completingSession(crew);
        CrewPloggingParticipant participant = CrewPloggingParticipant.create(crewSession, user, false);
        participant.start();
        PloggingDto.CompleteRequest request = request(crewSessionId);
        participant.submit(personalRecord(user, request), LocalDateTime.now());
        given(sessionRepository.findByIdForUpdate(crewSessionId)).willReturn(Optional.of(crewSession));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.findBySessionIdAndUserIdForUpdate(crewSessionId, userId))
                .willReturn(Optional.of(participant));

        // when & then
        assertThatThrownBy(() -> completionProcessor.complete(userId, request))
                .isInstanceOf(CrewException.class);
        org.mockito.Mockito.verify(ploggingRecordWriter, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("RECRUITING, CANCELED, COMPLETED 상태에서는 크루 연결 개인 완료를 거부한다")
    @ParameterizedTest
    @EnumSource(value = CrewPloggingStatus.class, names = {"RECRUITING", "CANCELED", "COMPLETED"})
    void complete_rejects_disallowed_session_status(CrewPloggingStatus status) {
        // given
        Crew crew = Crew.create("크루", "A1B2C3D4", user("크루장"));
        CrewPloggingSession session = sessionWithStatus(crew, status);
        given(sessionRepository.findByIdForUpdate(10L)).willReturn(Optional.of(session));

        // when & then
        assertThatThrownBy(() -> completionProcessor.complete(1L, request(10L)))
                .isInstanceOf(CrewException.class);
        verify(ploggingRecordWriter, never()).save(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private void stubCompletion(
            Long userId,
            Long crewSessionId,
            User user,
            Crew crew,
            CrewMember member,
            CrewPloggingSession crewSession,
            CrewPloggingParticipant participant,
            PloggingDto.CompleteRequest request,
            PloggingSession personalRecord,
            PloggingDto.CompleteResponse response,
            long unsubmittedCount
    ) {
        given(sessionRepository.findByIdForUpdate(crewSessionId)).willReturn(Optional.of(crewSession));
        given(crewMemberRepository.findByCrewIdAndUserIdAndStatus(
                crew.getId(), userId, CrewMemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(participantRepository.findBySessionIdAndUserIdForUpdate(crewSessionId, userId))
                .willReturn(Optional.of(participant));
        given(ploggingRecordWriter.save(user, request, crewSession))
                .willReturn(new PloggingRecordWriter.SavedPloggingRecord(personalRecord, response));
        given(participantRepository.countByCrewPloggingSessionIdAndStatusIn(crewSessionId, List.of(
                CrewPloggingParticipantStatus.JOINED,
                CrewPloggingParticipantStatus.PARTICIPATING
        ))).willReturn(unsubmittedCount);
    }

    private CrewPloggingSession inProgressSession(Crew crew) {
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        session.start(LocalDateTime.now().minusHours(1));
        return session;
    }

    private CrewPloggingSession sessionWithStatus(Crew crew, CrewPloggingStatus status) {
        LocalDateTime now = LocalDateTime.now();
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        if (status == CrewPloggingStatus.RECRUITING) {
            return session;
        }
        if (status == CrewPloggingStatus.CANCELED) {
            session.cancel(now);
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

    private CrewPloggingSession completingSession(Crew crew) {
        CrewPloggingSession session = CrewPloggingSession.create(crew);
        LocalDateTime now = LocalDateTime.now();
        session.start(now.minusHours(1));
        session.end(now.minusMinutes(1), now.plusHours(24));
        return session;
    }

    private PloggingDto.CompleteRequest request(Long crewSessionId) {
        return new PloggingDto.CompleteRequest(
                PloggingMode.FREE,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                1000, 2000, 100, 3600, 0, "공원",
                37.5, 127.0, 37.6, 127.1,
                List.of(), null, List.of(), crewSessionId
        );
    }

    private PloggingSession personalRecord(User user, PloggingDto.CompleteRequest request) {
        return PloggingSession.create(
                user, request.mode(), request.startedAt(), request.finishedAt(),
                request.distanceMeters(), request.stepCount(), request.caloriesBurned(),
                request.ploggingSeconds(), request.restSeconds(), request.placeName(),
                request.startLatitude(), request.startLongitude(), request.endLatitude(), request.endLongitude(),
                request.mapImageUrl()
        );
    }

    private User user(String nickname) {
        return User.create(OAuthProvider.KAKAO, nickname, "test@test.com", nickname, null);
    }
}
