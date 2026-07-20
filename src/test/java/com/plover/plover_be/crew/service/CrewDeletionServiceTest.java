package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.repository.CrewMemberRepository;
import com.plover.plover_be.crew.repository.CrewPloggingParticipantRepository;
import com.plover.plover_be.crew.repository.CrewPloggingSessionRepository;
import com.plover.plover_be.crew.repository.CrewRepository;
import com.plover.plover_be.plogging.repository.PloggingPhotoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrewDeletionServiceTest {

    @Mock private CrewRepository crewRepository;
    @Mock private CrewMemberRepository crewMemberRepository;
    @Mock private CrewPloggingSessionRepository sessionRepository;
    @Mock private CrewPloggingParticipantRepository participantRepository;
    @Mock private PloggingPhotoRepository photoRepository;
    @InjectMocks private CrewDeletionService crewDeletionService;

    @DisplayName("여러 소유 크루는 공유 사진 연결부터 FK 순서대로 모두 삭제한다")
    @Test
    void deletes_all_owned_crews_in_fk_order_without_deleting_personal_photos() {
        // given
        Long userId = 1L;
        given(crewRepository.findIdsByLeaderId(userId)).willReturn(List.of(10L, 20L));

        // when
        crewDeletionService.deleteOwnedCrewsAndUserReferences(userId);

        // then
        InOrder inOrder = inOrder(photoRepository, participantRepository, sessionRepository,
                crewMemberRepository, crewRepository);
        verifyCrewDeletion(inOrder, 10L);
        verifyCrewDeletion(inOrder, 20L);
        inOrder.verify(sessionRepository).clearRepresentativeRecordByUserId(userId);
        inOrder.verify(participantRepository).deleteByUserId(userId);
        inOrder.verify(crewMemberRepository).deleteByUserId(userId);
        verify(photoRepository, never()).deleteByPloggingSessionUserId(userId);
    }

    @DisplayName("일반 크루원 탈퇴는 크루를 삭제하지 않고 사용자 직접 참조만 정리한다")
    @Test
    void member_deletion_keeps_crew_and_representative_snapshots() {
        // given
        Long userId = 2L;
        given(crewRepository.findIdsByLeaderId(userId)).willReturn(List.of());

        // when
        crewDeletionService.deleteOwnedCrewsAndUserReferences(userId);

        // then
        verify(sessionRepository).clearRepresentativeRecordByUserId(userId);
        verify(participantRepository).deleteByUserId(userId);
        verify(crewMemberRepository).deleteByUserId(userId);
        verify(crewRepository, never()).deleteByCrewId(org.mockito.ArgumentMatchers.anyLong());
        verify(sessionRepository, never()).deleteByCrewId(org.mockito.ArgumentMatchers.anyLong());
    }

    private void verifyCrewDeletion(InOrder inOrder, Long crewId) {
        inOrder.verify(photoRepository).clearCrewPloggingSessionByCrewId(crewId);
        inOrder.verify(participantRepository).deleteByCrewId(crewId);
        inOrder.verify(sessionRepository).deleteByCrewId(crewId);
        inOrder.verify(crewMemberRepository).deleteByCrewId(crewId);
        inOrder.verify(crewRepository).deleteByCrewId(crewId);
    }
}
