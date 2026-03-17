package org.solmate.common.status;

import org.solmate.common.base.BaseStatus;
import org.springframework.http.HttpStatus;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SuccessStatus implements BaseStatus {

    SUCCESS_200("SOLMATE_200", HttpStatus.OK, "성공입니다."),
    SUCCESS_201("SOLMATE_201", HttpStatus.CREATED, "성공입니다."),
    SUCCESS_204("SOLMATE_204", HttpStatus.NO_CONTENT, "성공입니다.");


    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}
