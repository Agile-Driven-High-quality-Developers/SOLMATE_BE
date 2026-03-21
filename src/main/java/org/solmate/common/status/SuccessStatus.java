package org.solmate.common.status;

import org.solmate.common.base.BaseStatus;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SuccessStatus implements BaseStatus {

    /**
     * Common
     */
    SUCCESS_200("SOLMATE_200", HttpStatus.OK, "성공입니다."),
    SUCCESS_201("SOLMATE_201", HttpStatus.CREATED, "성공입니다."),
    SUCCESS_204("SOLMATE_204", HttpStatus.NO_CONTENT, "성공입니다."),

    /**
     * Auth
     */
    SIGNUP_SUCCESS("AUTH_201", HttpStatus.CREATED, "회원가입 성공"),
    LOGIN_SUCCESS("AUTH_200", HttpStatus.OK, "로그인 성공"),
    LOGOUT_SUCCESS("AUTH_200", HttpStatus.OK, "로그아웃 성공"),
    REISSUE_SUCCESS("AUTH_200", HttpStatus.OK, "토큰 재발급 성공"),
    EMAIL_SEND_SUCCESS("AUTH_200", HttpStatus.OK, "인증 메일 발송 성공"),
    EMAIL_VERIFY_SUCCESS("AUTH_200", HttpStatus.OK, "이메일 인증 성공"),
    NICKNAME_CHECK_SUCCESS("AUTH_200", HttpStatus.OK, "사용 가능한 닉네임입니다."),

    /**
     * Trade (모의투자)
     */
    MOCK_DATA_INIT_SUCCESS("TRADE_200", HttpStatus.OK, "Mock 시세 데이터 초기화 성공"),
    BUY_ORDER_SUCCESS("TRADE_201", HttpStatus.CREATED, "매수 주문 접수 성공"),
    SELL_ORDER_SUCCESS("TRADE_201", HttpStatus.CREATED, "매도 주문 접수 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}