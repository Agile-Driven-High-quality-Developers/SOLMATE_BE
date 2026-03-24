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
    PASSWORD_RESET_SUCCESS("AUTH_200", HttpStatus.OK, "비밀번호 재설정 성공"),
    PASSWORD_CHECK_SUCCESS("USER_200", HttpStatus.OK, "비밀번호 확인 성공"),

    /**
     * Trade (모의투자)
     */
    MOCK_DATA_INIT_SUCCESS("TRADE_200", HttpStatus.OK, "Mock 시세 데이터 초기화 성공"),
    BUY_ORDER_SUCCESS("TRADE_201", HttpStatus.CREATED, "매수 주문 접수 성공"),
    SELL_ORDER_SUCCESS("TRADE_201", HttpStatus.CREATED, "매도 주문 접수 성공"),
    HOLDINGS_SUCCESS("TRADE_200", HttpStatus.OK, "보유 종목 조회 성공"),
    TRADE_HISTORY_SUCCESS("TRADE_200", HttpStatus.OK, "매매내역 조회 성공"),
    PROFILE_UPDATE_SUCCESS("USER_200", HttpStatus.OK, "프로필 업데이트 성공"),
    PROFILE_IMAGE_DELETE_SUCCESS("USER_200", HttpStatus.OK, "프로필 이미지 삭제 성공"),
    WITHDRAW_SUCCESS("USER_200", HttpStatus.OK, "회원 탈퇴 성공"),
    MENTORING_REQUEST_SUCCESS("MENTORING_201", HttpStatus.CREATED, "멘토링 신청 성공"),
    MENTORING_ACCEPT_SUCCESS("MENTORING_200_1", HttpStatus.OK, "멘토링 수락 성공"),
    MENTORING_REJECT_SUCCESS("MENTORING_200_2", HttpStatus.OK, "멘토링 거절 성공"),
    MENTORING_MY_STATUS_SUCCESS("MENTORING_200_3", HttpStatus.OK, "내 멘토링 신청 현황 조회 성공"),
    MENTORING_CANCEL_SUCCESS("MENTORING_200_4", HttpStatus.OK, "멘토링 취소 성공"),

    /**
     * Follow
     */
    FOLLOW_SUCCESS("FOLLOW_201", HttpStatus.CREATED, "팔로우 성공"),
    UNFOLLOW_SUCCESS("FOLLOW_200", HttpStatus.OK, "팔로우 취소 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}