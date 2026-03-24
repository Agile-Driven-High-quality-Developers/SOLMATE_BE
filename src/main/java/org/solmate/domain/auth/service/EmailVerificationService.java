package org.solmate.domain.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.auth.entity.EmailVerification;
import org.solmate.domain.auth.repository.EmailVerificationRepository;
import org.solmate.domain.auth.util.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailSender emailSender;
    private static final SecureRandom random = new SecureRandom();

    @Value("${mail.verification.expiration}")
    private long expirationSeconds;

    // 이메일 인증 코드 요청
    @Transactional
    public void requestEmailVerificationCode(String email) {
        String verificationCode = createEmailVerificationCode();
        emailVerificationRepository.findByEmail(email)
                .ifPresentOrElse(
                        existing -> updateEmailVerification(existing, verificationCode),
                        () -> registerEmailVerification(email, verificationCode)
                );

        emailSender.send(email, verificationCode);
    }

    // 비밀번호 재설정 인증 코드 요청
    @Transactional
    public void requestPasswordResetCode(String email) {
        String verificationCode = createEmailVerificationCode();
        emailVerificationRepository.findByEmail(email)
                .ifPresentOrElse(
                        existing -> updateEmailVerification(existing, verificationCode),
                        () -> registerEmailVerification(email, verificationCode)
                );

        emailSender.sendPasswordReset(email, verificationCode);
    }

    // 이메일 인증 코드 확인
    @Transactional
    public void confirmEmailVerificationCode(String email, String code) {
        EmailVerification emailVerification = findEmailVerificationByEmail(email);

        validateCodeExpiration(emailVerification.getUpdatedAt());
        validateVerificationCode(code, emailVerification.getVerificationCode());

        emailVerification.updateIsVerified(true);
    }

    // 이메일 검증 여부 검사
    @Transactional
    public void isEmailVerified(String email) {
        EmailVerification emailVerification = findEmailVerificationByEmail(email);

        if (!emailVerification.isVerified()) {
            throw new GeneralException(ErrorStatus.EMAIL_NOT_VERIFIED);
        }
        emailVerificationRepository.deleteById(emailVerification.getEmailVerificationId());
    }

    private void updateEmailVerification(EmailVerification emailVerification, String verificationCode) {
        emailVerification.updateIsVerified(false);
        emailVerification.updateVerificationCode(verificationCode);
    }


    private void registerEmailVerification(String email, String verificationCode) {
        EmailVerification newVerification = EmailVerification.builder()
                .email(email)
                .verificationCode(verificationCode)
                .isVerified(false)
                .build();

        emailVerificationRepository.save(newVerification);
    }

    // 이메일 정보 조회
    private EmailVerification findEmailVerificationByEmail(String email) {
        return emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.EMAIL_VERIFICATION_NOT_FOUND));
    }

    // 인증 코드 만료 여부
    private void validateCodeExpiration(LocalDateTime updatedAt) {
        LocalDateTime now = LocalDateTime.now();
        if (updatedAt.isBefore(now.minusSeconds(expirationSeconds))) {
            throw new GeneralException(ErrorStatus.VERIFICATION_CODE_EXPIRED);
        }
    }

    // 인증 코드 일치 여부
    private void validateVerificationCode(String inputCode, String storedCode) {
        if (!storedCode.equals(inputCode)) {
            throw new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE);
        }
    }


    private String createEmailVerificationCode() {
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
