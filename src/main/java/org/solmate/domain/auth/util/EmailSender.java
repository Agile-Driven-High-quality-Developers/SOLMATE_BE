package org.solmate.domain.auth.util;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderAddress;

    // 이메일 인증 코드 발송
    public void send(String recipientEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("[SOLMATE] 이메일 인증 코드입니다.");
            helper.setText(buildHtmlContent(verificationCode), true);

            mailSender.send(message);

            log.info("이메일 전송 완료 → {}", recipientEmail);
        } catch (Exception e) {
            log.error("이메일 전송 실패 → {}", recipientEmail, e);
            throw new GeneralException(ErrorStatus.EMAIL_SEND_FAILED);
        }
    }

    // 비밀번호 재설정 인증 코드 발송
    public void sendPasswordReset(String recipientEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("[SOLMATE] 비밀번호 재설정 인증 코드입니다.");
            helper.setText(buildPasswordResetHtmlContent(verificationCode), true);

            mailSender.send(message);

            log.info("비밀번호 재설정 이메일 전송 완료 → {}", recipientEmail);
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 전송 실패 → {}", recipientEmail, e);
            throw new GeneralException(ErrorStatus.EMAIL_SEND_FAILED);
        }
    }

    private String buildHtmlContent(String code) {
        Context context = new Context();
        context.setVariable("verificationCode", code);
        return templateEngine.process("mail/verificationEmail", context);
    }

    private String buildPasswordResetHtmlContent(String code) {
        Context context = new Context();
        context.setVariable("verificationCode", code);
        return templateEngine.process("mail/passwordResetEmail", context);
    }
}
