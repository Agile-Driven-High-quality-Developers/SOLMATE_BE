package org.solmate.domain.social.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 유저 목록 조회 응답 래퍼 DTO
 *
 * 필드 설명:
 * - hasAcceptedMentor : 현재 로그인 유저에게 이미 수락된 멘토가 있는지 여부
 *                       true이면 프론트에서 NONE 상태의 멘토신청 버튼을 모두 비활성화
 * - users             : 유저 목록 (UserListItemResponse 리스트)
 * - nextCursor        : 다음 페이지 요청 시 사용할 커서 (마지막 유저의 userId)
 *                       hasNext=false이면 null
 * - hasNext           : 다음 페이지 존재 여부
 */
@Getter
@AllArgsConstructor
public class UserListResponse {

    private boolean hasAcceptedMentor;
    private List<UserListItemResponse> users;
    private Long nextCursor;
    private boolean hasNext;
}
