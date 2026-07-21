package com.plover.plover_be.crew.service;

import com.plover.plover_be.crew.exception.CrewErrorCode;
import com.plover.plover_be.crew.exception.CrewException;
import com.plover.plover_be.crew.repository.CrewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class CrewJoinCodeGenerator {

    private static final int JOIN_CODE_BOUND = 1_000_000;
    private static final int MAX_GENERATION_ATTEMPTS = 20;

    private final CrewRepository crewRepository;
    private final SecureRandom secureRandom;

    @Autowired
    public CrewJoinCodeGenerator(CrewRepository crewRepository) {
        this(crewRepository, new SecureRandom());
    }

    CrewJoinCodeGenerator(CrewRepository crewRepository, SecureRandom secureRandom) {
        this.crewRepository = crewRepository;
        this.secureRandom = secureRandom;
    }

    public String generate() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String joinCode = format(secureRandom.nextInt(JOIN_CODE_BOUND));
            if (!crewRepository.existsByJoinCode(joinCode)) {
                return joinCode;
            }
        }
        throw new CrewException(CrewErrorCode.JOIN_CODE_CONFLICT);
    }

    static String format(int value) {
        return String.format(Locale.ROOT, "%06d", value);
    }
}
