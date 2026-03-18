package org.solmate.domain.user.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.EMAIL_NOT_FOUND));
    }

    public void checkEmailNotDuplicated(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void updatePassword(User user, String encodedPassword) {
        user.updatePassword(encodedPassword);
    }

    @Transactional
    public void updateNickname(User user, String nickname) {
        user.updateNickname(nickname);
    }
}