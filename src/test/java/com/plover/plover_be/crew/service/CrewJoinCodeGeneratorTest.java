package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CrewJoinCodeGeneratorTest {

    @DisplayName("난수를 앞자리 0을 유지하는 숫자 6자리 참여 코드로 생성한다")
    @Test
    void generate_formats_six_digit_numeric_code() {
        CrewRepository crewRepository = mock(CrewRepository.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        given(secureRandom.nextInt(1_000_000)).willReturn(527);
        given(crewRepository.existsByJoinCode("000527")).willReturn(false);
        CrewJoinCodeGenerator generator = new CrewJoinCodeGenerator(crewRepository, secureRandom);

        String joinCode = generator.generate();

        assertThat(joinCode).isEqualTo("000527");
    }

    @DisplayName("중복 참여 코드가 생성되면 새로운 코드로 재시도한다")
    @Test
    void generate_retries_when_code_exists() {
        CrewRepository crewRepository = mock(CrewRepository.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        given(secureRandom.nextInt(1_000_000)).willReturn(123456, 654321);
        given(crewRepository.existsByJoinCode("123456")).willReturn(true);
        given(crewRepository.existsByJoinCode("654321")).willReturn(false);
        CrewJoinCodeGenerator generator = new CrewJoinCodeGenerator(crewRepository, secureRandom);

        String joinCode = generator.generate();

        assertThat(joinCode).isEqualTo("654321");
    }

    @DisplayName("20회 연속 중복이면 참여 코드 충돌 예외가 발생한다")
    @Test
    void generate_throws_exception_after_twenty_collisions() {
        CrewRepository crewRepository = mock(CrewRepository.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        given(secureRandom.nextInt(1_000_000)).willReturn(123456);
        given(crewRepository.existsByJoinCode("123456")).willReturn(true);
        CrewJoinCodeGenerator generator = new CrewJoinCodeGenerator(crewRepository, secureRandom);

        assertThatThrownBy(generator::generate)
                .isInstanceOf(CrewException.class);
        verify(crewRepository, times(20)).existsByJoinCode("123456");
    }
}
