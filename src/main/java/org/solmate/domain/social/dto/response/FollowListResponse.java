package org.solmate.domain.social.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 팔로워 / 팔로잉 목록 응답 DTO (커서 기반 페이지네이션)
 *
 * 필드 설명:
 * - users      : 팔로워 또는 팔로잉 유저 목록
 * - nextCursor : 다음 페이지 요청 시 사용할 커서 (마지막 페이지면 null)
 * - hasNext    : 다음 페이지 존재 여부
 */
@Getter
@AllArgsConstructor
public class FollowListResponse {

    private List<FollowListItemResponse> users;
    private Long nextCursor;
    private boolean hasNext;
}
