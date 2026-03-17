package org.solmate.domain.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TodoUpdateRequest {

    @NotBlank(message = "할 일 제목은 필수입니다.")
    private String title;
}
