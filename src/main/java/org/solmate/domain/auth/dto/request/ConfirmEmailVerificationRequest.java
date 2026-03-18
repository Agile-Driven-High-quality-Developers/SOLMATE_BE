package org.solmate.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmEmailVerificationRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 30, message = "이메일은 30자를 초과할 수 없습니다.")
        String email,

        @NotBlank(message = "인증코드는 필수입니다.")
        @Size(min = 4, max = 4, message = "인증코드는 4자리여야 합니다.")
        String emailVerificationCode
) {
}
