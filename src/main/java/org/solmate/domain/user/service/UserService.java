package org.solmate.domain.user.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.s3.S3Service;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;

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

    public void checkNicknameNotDuplicated(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new GeneralException(ErrorStatus.NICKNAME_ALREADY_EXISTS);
        }
    }

    public void checkPassword(Long userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new GeneralException(ErrorStatus.INVALID_PASSWORD);
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

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new GeneralException(ErrorStatus.USER_ALREADY_WITHDRAWN);
        }

        if (user.getImageUrl() != null) {
            s3Service.deleteFile(user.getImageUrl());
        }

        user.withdraw();
    }

    @Transactional
    public void withdrawWithPassword(Long userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new GeneralException(ErrorStatus.INVALID_PASSWORD);
        }

        if (user.isWithdrawn()) {
            throw new GeneralException(ErrorStatus.USER_ALREADY_WITHDRAWN);
        }

        if (user.getImageUrl() != null) {
            s3Service.deleteFile(user.getImageUrl());
        }

        user.withdraw();
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (user.getImageUrl() != null) {
            s3Service.deleteFile(user.getImageUrl());
            user.updateImageUrl(null);
        }
    }

    @Transactional
    public void updateProfile(Long userId, MultipartFile image, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (image != null && !image.isEmpty()) {
            if (user.getImageUrl() != null) {
                s3Service.deleteFile(user.getImageUrl());
            }
            String imageUrl = s3Service.uploadFile(image, "profile/" + userId);
            user.updateImageUrl(imageUrl);
        }

        if (nickname != null && !nickname.isBlank()) {
            checkNicknameNotDuplicated(nickname);
            user.updateNickname(nickname);
        }
    }
}