package org.solmate.common.status;

import org.solmate.common.base.BaseStatus;
import org.springframework.http.HttpStatus;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {

    // 예시
    ERROR_STATUS("SOLMATE_400", HttpStatus.BAD_REQUEST, "Bad Request"),

    /**
     * Common
     */
    BAD_REQUEST("COMM_400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("COMM_401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("COMM_403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("COMM_404", HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("COMM_405", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다."),
    INTERNAL_SERVER_ERROR("COMM_500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");



    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
