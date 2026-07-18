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
import com.plover.plover_be.user.domain.OAuthProvider;
import com.plover.plover_be.user.domain.User;
import com.plover.plover_be.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

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
    @Autowired private CrewDeletionService crewDeletionService;
    @Autowired private CrewRepository crewRepository;
    @Autowired private CrewMemberRepository crewMemberRepository;
    @Autowired private CrewPloggingSessionRepository sessionRepository;
    @Autowired private CrewPloggingParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;
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
        String joinCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Crew crew = crewRepository.save(Crew.create("크루", joinCode, leader));
        crewMemberRepository.save(CrewMember.create(crew, leader, CrewRole.LEADER));
        CrewPloggingSession session = sessionRepository.save(CrewPloggingSession.create(crew));
        participantRepository.save(CrewPloggingParticipant.create(session, leader, true));
        return new TestIds(leader.getId(), session.getId());
    }

    private record TestIds(Long userId, Long sessionId) {}
}
