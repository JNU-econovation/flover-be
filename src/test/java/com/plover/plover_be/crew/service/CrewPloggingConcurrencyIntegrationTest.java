package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.domain.Crew;
import com.plover.plover_be.crew.domain.CrewMember;
import com.plover.plover_be.crew.domain.CrewPloggingParticipant;
import com.plover.plover_be.crew.domain.CrewPloggingSession;
import com.plover.plover_be.crew.domain.CrewPloggingStatus;
import com.plover.plover_be.crew.domain.CrewRole;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.plogging.domain.PloggingMode;
import com.plover.plover_be.plogging.dto.PloggingDto;
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.repository.UserRepository;
import com.plover.plover_be.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CrewPloggingConcurrencyIntegrationTest {

    @Autowired private CrewPloggingService crewPloggingService;
    @Autowired private CrewPloggingCompletionProcessor completionProcessor;
    @Autowired private CrewService crewService;
    @Autowired private CrewDeletionService crewDeletionService;
    @Autowired private CrewRepository crewRepository;
    @Autowired private CrewMemberRepository crewMemberRepository;
    @Autowired private CrewPloggingSessionRepository sessionRepository;
    @Autowired private CrewPloggingParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private TransactionTemplate transactionTemplate;

    @DisplayName("start와 cancel 동시 요청은 세션 행 잠금으로 하나의 상태 전이만 성공한다")
    @Test
    void concurrent_start_and_cancel_allow_only_one_transition() throws Exception {
        // given
        TestIds ids = transactionTemplate.execute(status -> createRecruitingSession());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Future<Boolean> startResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewPloggingService.startSession(ids.userId(), ids.sessionId())));
            Future<Boolean> cancelResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewPloggingService.cancelSession(ids.userId(), ids.sessionId())));

            // when
            startSignal.countDown();
            boolean startSucceeded = startResult.get(10, TimeUnit.SECONDS);
            boolean cancelSucceeded = cancelResult.get(10, TimeUnit.SECONDS);

            // then
            assertThat(startSucceeded ^ cancelSucceeded).isTrue();
            CrewPloggingStatus finalStatus = sessionRepository.findById(ids.sessionId()).orElseThrow().getStatus();
            assertThat(finalStatus).isIn(CrewPloggingStatus.IN_PROGRESS, CrewPloggingStatus.CANCELED);
        } finally {
            executor.shutdownNow();
            transactionTemplate.executeWithoutResult(status -> {
                crewDeletionService.deleteOwnedCrewsAndUserReferences(ids.userId());
                userRepository.deleteById(ids.userId());
            });
        }
    }

    @DisplayName("자발적 탈퇴와 강퇴 동시 요청은 멤버십을 한 번만 WITHDRAWN으로 전환한다")
    @Test
    void concurrent_withdraw_and_removal_allow_only_one_membership_transition() throws Exception {
        MembershipTestIds ids = transactionTemplate.execute(status -> createCrewWithMember(false));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Future<Boolean> withdrawResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewService.withdraw(ids.memberId(), ids.crewId())));
            Future<Boolean> removeResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewService.removeMember(ids.leaderId(), ids.crewId(), ids.memberId())));

            startSignal.countDown();
            assertThat(withdrawResult.get(10, TimeUnit.SECONDS) ^ removeResult.get(10, TimeUnit.SECONDS)).isTrue();
            String memberStatus = transactionTemplate.execute(status -> crewMemberRepository
                    .findByCrewIdAndUserIdForUpdate(ids.crewId(), ids.memberId())
                    .orElseThrow().getStatus().name());
            assertThat(memberStatus).isEqualTo("WITHDRAWN");
        } finally {
            executor.shutdownNow();
            cleanup(ids.leaderId(), ids.memberId());
        }
    }

    @DisplayName("세션 시작과 탈퇴가 경합해도 참가 상태와 멤버십 상태가 모순되지 않는다")
    @Test
    void concurrent_start_and_withdraw_keep_participant_and_membership_consistent() throws Exception {
        MembershipTestIds ids = transactionTemplate.execute(status -> createCrewWithMember(true));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Future<Boolean> sessionStartResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewPloggingService.startSession(ids.leaderId(), ids.sessionId())));
            Future<Boolean> withdrawResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewService.withdraw(ids.memberId(), ids.crewId())));

            startSignal.countDown();
            assertThat(sessionStartResult.get(10, TimeUnit.SECONDS)).isTrue();
            boolean withdrew = withdrawResult.get(10, TimeUnit.SECONDS);
            ParticipantMembershipState state = transactionTemplate.execute(status -> new ParticipantMembershipState(
                    crewMemberRepository.findByCrewIdAndUserIdForUpdate(ids.crewId(), ids.memberId())
                            .orElseThrow().getStatus().name(),
                    participantRepository.findBySessionIdAndUserIdForUpdate(ids.sessionId(), ids.memberId())
                            .orElseThrow().getStatus().name()
            ));
            if (withdrew) {
                assertThat(state.memberStatus()).isEqualTo("WITHDRAWN");
                assertThat(state.participantStatus()).isEqualTo("CANCELED");
            } else {
                assertThat(state.memberStatus()).isEqualTo("ACTIVE");
                assertThat(state.participantStatus()).isEqualTo("PARTICIPATING");
            }
        } finally {
            executor.shutdownNow();
            cleanup(ids.leaderId(), ids.memberId());
        }
    }

    @DisplayName("전체 종료와 강퇴가 경합하면 미제출 참가자의 멤버십을 변경하지 않는다")
    @Test
    void concurrent_end_and_removal_keep_unsubmitted_member_active() throws Exception {
        MembershipTestIds ids = transactionTemplate.execute(status -> createCrewWithMember(true));
        crewPloggingService.startSession(ids.leaderId(), ids.sessionId());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Future<Boolean> endResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewPloggingService.endSession(ids.leaderId(), ids.sessionId())));
            Future<Boolean> removeResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewService.removeMember(ids.leaderId(), ids.crewId(), ids.memberId())));

            startSignal.countDown();
            assertThat(endResult.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(removeResult.get(10, TimeUnit.SECONDS)).isFalse();
            ParticipantMembershipState state = findParticipantMembershipState(ids);
            assertThat(state.memberStatus()).isEqualTo("ACTIVE");
            assertThat(state.participantStatus()).isEqualTo("PARTICIPATING");
            assertThat(sessionRepository.findById(ids.sessionId()).orElseThrow().getStatus())
                    .isEqualTo(CrewPloggingStatus.COMPLETING);
        } finally {
            executor.shutdownNow();
            cleanup(ids.leaderId(), ids.memberId());
        }
    }

    @DisplayName("개인 완료와 강퇴가 경합해도 제출 기록은 보존되고 멤버십 상태가 일관된다")
    @Test
    void concurrent_completion_and_removal_preserve_submitted_record() throws Exception {
        MembershipTestIds ids = transactionTemplate.execute(status -> createCrewWithMember(true));
        crewPloggingService.startSession(ids.leaderId(), ids.sessionId());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Future<Boolean> completeResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> completionProcessor.complete(
                            ids.memberId(), completeRequest(ids.sessionId()))));
            Future<Boolean> removeResult = executor.submit(() -> executeAfterSignal(
                    startSignal, () -> crewService.removeMember(ids.leaderId(), ids.crewId(), ids.memberId())));

            startSignal.countDown();
            assertThat(completeResult.get(10, TimeUnit.SECONDS)).isTrue();
            boolean removed = removeResult.get(10, TimeUnit.SECONDS);
            ParticipantMembershipState state = findParticipantMembershipState(ids);
            assertThat(state.participantStatus()).isEqualTo("SUBMITTED");
            assertThat(state.memberStatus()).isEqualTo(removed ? "WITHDRAWN" : "ACTIVE");
            Long recordId = transactionTemplate.execute(status -> participantRepository
                    .findBySessionIdAndUserIdForUpdate(ids.sessionId(), ids.memberId())
                    .orElseThrow().getPloggingSession().getId());
            assertThat(recordId).isNotNull();
        } finally {
            executor.shutdownNow();
            cleanup(ids.leaderId(), ids.memberId());
        }
    }

    private boolean executeAfterSignal(CountDownLatch signal, Runnable request) throws InterruptedException {
        signal.await();
        try {
            request.run();
            return true;
        } catch (CrewException expectedConflict) {
            return false;
        }
    }

    private TestIds createRecruitingSession() {
        String suffix = UUID.randomUUID().toString();
        User leader = userRepository.save(User.create(
                OAuthProvider.KAKAO, "leader" + suffix, "leader@test.com", "leader" + suffix, null));
        String joinCode = String.format("%06d", Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000));
        Crew crew = crewRepository.save(Crew.create("크루", joinCode, leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        CrewPloggingSession session = sessionRepository.save(CrewPloggingSession.create(crew));
        participantRepository.save(CrewPloggingParticipant.create(session, leader, true));
        return new TestIds(leader.getId(), session.getId());
    }

    private MembershipTestIds createCrewWithMember(boolean createSession) {
        String suffix = UUID.randomUUID().toString();
        User leader = userRepository.save(User.create(
                OAuthProvider.KAKAO, "leader" + suffix, "leader@test.com", "leader" + suffix, null));
        User member = userRepository.save(User.create(
                OAuthProvider.KAKAO, "member" + suffix, "member@test.com", "member" + suffix, null));
        Crew crew = crewRepository.save(Crew.create(
                "크루", String.format("%06d", Math.floorMod(suffix.hashCode(), 1_000_000)), leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        crewMemberRepository.save(CrewMember.create(crew, member, CrewRole.MEMBER));
        if (!createSession) {
            return new MembershipTestIds(leader.getId(), member.getId(), crew.getId(), null);
        }
        CrewPloggingSession session = sessionRepository.save(CrewPloggingSession.create(crew));
        participantRepository.save(CrewPloggingParticipant.create(session, leader, true));
        participantRepository.save(CrewPloggingParticipant.create(session, member, false));
        return new MembershipTestIds(leader.getId(), member.getId(), crew.getId(), session.getId());
    }

    private ParticipantMembershipState findParticipantMembershipState(MembershipTestIds ids) {
        return transactionTemplate.execute(status -> new ParticipantMembershipState(
                crewMemberRepository.findByCrewIdAndUserIdForUpdate(ids.crewId(), ids.memberId())
                        .orElseThrow().getStatus().name(),
                participantRepository.findBySessionIdAndUserIdForUpdate(ids.sessionId(), ids.memberId())
                        .orElseThrow().getStatus().name()
        ));
    }

    private PloggingDto.CompleteRequest completeRequest(Long sessionId) {
        LocalDateTime now = LocalDateTime.now();
        return new PloggingDto.CompleteRequest(
                PloggingMode.FREE,
                now.minusHours(1),
                now,
                1000,
                2000,
                100,
                3600,
                0,
                "공원",
                37.5,
                127.0,
                37.6,
                127.1,
                List.of(),
                null,
                List.of(),
                sessionId
        );
    }

    private void cleanup(Long leaderId, Long memberId) {
        userService.deleteUser(memberId);
        userService.deleteUser(leaderId);
    }

    private record TestIds(Long userId, Long sessionId) {}

    private record MembershipTestIds(Long leaderId, Long memberId, Long crewId, Long sessionId) {}

    private record ParticipantMembershipState(String memberStatus, String participantStatus) {}
}
