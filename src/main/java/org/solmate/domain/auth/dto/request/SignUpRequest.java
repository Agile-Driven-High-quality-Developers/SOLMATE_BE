package org.solmate.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SignUpRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 30, message = "이메일은 30자를 초과할 수 없습니다.")
        String email,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20, message = "닉네임은 20자를 초과할 수 없습니다.")
        String nickname,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}
