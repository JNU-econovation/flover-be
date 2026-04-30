package com.flover.flover_be.user.service;

import com.flover.flover_be.user.domain.User;
import com.flover.flover_be.user.dto.UserDto;
import com.flover.flover_be.user.exception.UserErrorCode;
import com.flover.flover_be.user.exception.UserException;
import com.flover.flover_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto.NicknameResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (!nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
            throw new UserException(UserErrorCode.NICKNAME_ALREADY_USED);
        }

        user.updateNickname(nickname);
        return new UserDto.NicknameResponse(user.getId(), user.getNickname());
    }
}
