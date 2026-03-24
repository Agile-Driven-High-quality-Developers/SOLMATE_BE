package org.solmate.domain.social.controller;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.social.dto.request.CreateCommentRequest;
import org.solmate.domain.social.dto.request.UpdateCommentRequest;
import org.solmate.domain.social.dto.response.CommentResponse;
import org.solmate.domain.social.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "매매일지에 댓글을 작성합니다. 멘토/멘티 관계일 때만 가능합니다.")
    @PostMapping("/diaries/{diaryId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long diaryId,
            Authentication authentication,
            @RequestBody CreateCommentRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(SuccessStatus.SUCCESS_201, commentService.createComment(diaryId, userId, request));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다.")
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestBody UpdateCommentRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(SuccessStatus.SUCCESS_200, commentService.updateComment(commentId, userId, request));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        commentService.deleteComment(commentId, userId);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, null);
    }
}
