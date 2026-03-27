package org.solmate.domain.user.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.s3.S3Service;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.auth.enums.OAuthProvider;
import org.solmate.domain.auth.repository.LoginTypeRepository;
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
    private final LoginTypeRepository loginTypeRepository;


    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.EMAIL_NOT_FOUND));
    }

    public void checkEmailNotDuplicated(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            if (loginTypeRepository.existsByUserAndLoginType(user, OAuthProvider.GOOGLE)) {
                throw new GeneralException(ErrorStatus.EMAIL_REGISTERED_WITH_GOOGLE);
            }
            throw new GeneralException(ErrorStatus.EMAIL_ALREADY_EXISTS);
        });
    }

    public void checkNicknameNotDuplicated(String nickname) {
        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new GeneralException(ErrorStatus.NICKNAME_ALREADY_EXISTS);
        }
    }

    public void checkNicknameForUser(Long userId, String nickname) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (nickname.equals(user.getNickname())) {
            throw new GeneralException(ErrorStatus.NICKNAME_SAME_AS_CURRENT);
        }
        checkNicknameNotDuplicated(nickname);
    }

    public void checkPassword(Long userId, String rawPassword) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
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
    public void withdraw(Long userId, String rawPassword) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        boolean isGoogleUser = loginTypeRepository.existsByUserAndLoginType(user, OAuthProvider.GOOGLE);

        if (!isGoogleUser) {
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new GeneralException(ErrorStatus.INVALID_PASSWORD);
            }
            if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                throw new GeneralException(ErrorStatus.INVALID_PASSWORD);
            }
        }

        if (user.getImageUrl() != null) {
            s3Service.deleteFile(user.getImageUrl());
        }

        user.withdraw();
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (user.getImageUrl() != null) {
            s3Service.deleteFile(user.getImageUrl());
            user.updateImageUrl(null);
        }
    }

    @Transactional
    public void updateProfile(Long userId, MultipartFile image, String nickname) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (image != null && !image.isEmpty()) {
            if (user.getImageUrl() != null) {
                s3Service.deleteFile(user.getImageUrl());
            }
            String imageUrl = s3Service.uploadFile(image, "profile/" + userId);
            user.updateImageUrl(imageUrl);
        }

        if (nickname != null && !nickname.isBlank()) {

            if (nickname.equals(user.getNickname())) {
                throw new GeneralException(ErrorStatus.NICKNAME_SAME_AS_CURRENT);
            }
            checkNicknameNotDuplicated(nickname);
            user.updateNickname(nickname);
        }
    }
}