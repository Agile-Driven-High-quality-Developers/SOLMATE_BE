package org.solmate.common.status;

import org.solmate.common.base.BaseStatus;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {

    /**
     * Common
     */
    BAD_REQUEST("COMM_400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("COMM_401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("COMM_403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("COMM_404", HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("COMM_405", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다."),
    INTERNAL_SERVER_ERROR("COMM_500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    /**
     * Auth
     */
    INVALID_PASSWORD_FORMAT("AUTH_400", HttpStatus.BAD_REQUEST, "비밀번호 양식이 올바르지 않습니다."),
    INVALID_PASSWORD("AUTH_401", HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN("AUTH_401", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    VERIFICATION_CODE_EXPIRED("AUTH_400", HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),
    INVALID_VERIFICATION_CODE("AUTH_400", HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    EMAIL_VERIFICATION_NOT_FOUND("AUTH_404", HttpStatus.NOT_FOUND, "이메일 인증 요청을 먼저 진행해주세요."),

    /**
     * User
     */
    USER_NOT_FOUND("USER_404", HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    EMAIL_NOT_FOUND("USER_404", HttpStatus.NOT_FOUND, "존재하지 않는 이메일입니다."),
    EMAIL_ALREADY_EXISTS("USER_409", HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    NICKNAME_ALREADY_EXISTS("USER_409", HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    EMAIL_NOT_VERIFIED("USER_400", HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_SEND_FAILED("USER_500", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),

    /**
     * S3
     */
    S3_FILE_UPLOAD_FAILED("S3_500", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    /**
     * Trade
     */
    ACCOUNT_NOT_FOUND("TRADE_404_1", HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),
    INSUFFICIENT_CASH("TRADE_400_1", HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    STOCK_NOT_FOUND("TRADE_404_2", HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."),
    INSUFFICIENT_HOLDINGS("TRADE_400_2", HttpStatus.BAD_REQUEST, "보유 수량이 부족합니다."),
    STOCK_PRICE_NOT_FOUND("TRADE_404_3", HttpStatus.NOT_FOUND, "Redis에 해당 종목 시세가 없습니다."),

    /**
     * Mentoring
     */
    /**
     * Notification
     */
    NOTIFICATION_NOT_FOUND("NOTIFICATION_404", HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_UNAUTHORIZED("NOTIFICATION_403", HttpStatus.FORBIDDEN, "해당 알림에 대한 권한이 없습니다."),
    NOTIFICATION_INVALID_TYPE("NOTIFICATION_400", HttpStatus.BAD_REQUEST, "해당 알림에서 수행할 수 없는 작업입니다."),

    MENTORING_SELF_REQUEST("MENTORING_400_1", HttpStatus.BAD_REQUEST, "자기 자신에게 멘토링을 신청할 수 없습니다."),
    MENTORING_ALREADY_HAS_MENTOR("MENTORING_400_2", HttpStatus.BAD_REQUEST, "이미 멘토가 있습니다."),
    MENTORING_ALREADY_REQUESTED("MENTORING_409", HttpStatus.CONFLICT, "이미 멘토링 요청을 보냈습니다."),
    MENTORING_RELATION_NOT_FOUND("MENTORING_404", HttpStatus.NOT_FOUND, "멘토링 요청을 찾을 수 없습니다."),
    MENTORING_UNAUTHORIZED("MENTORING_403", HttpStatus.FORBIDDEN, "해당 멘토링 요청에 대한 권한이 없습니다."),
    MENTORING_ALREADY_RESPONDED("MENTORING_400_3", HttpStatus.BAD_REQUEST, "이미 처리된 멘토링 요청입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
