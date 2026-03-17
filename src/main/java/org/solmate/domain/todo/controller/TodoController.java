package org.solmate.domain.todo.controller;

import java.util.List;

import org.solmate.common.response.ApiResponse;
import org.solmate.common.status.SuccessStatus;
import org.solmate.domain.todo.dto.request.TodoCreateRequest;
import org.solmate.domain.todo.dto.request.TodoUpdateRequest;
import org.solmate.domain.todo.dto.response.TodoResponse;
import org.solmate.domain.todo.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    // 생성
    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> create(@Valid @RequestBody TodoCreateRequest request) {
        return ApiResponse.success(SuccessStatus.SUCCESS_201, todoService.create(request));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getAll() {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, todoService.getAll());
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoResponse>> getOne(@PathVariable Long id) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, todoService.getOne(id));
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TodoUpdateRequest request) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, todoService.update(id, request));
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ApiResponse.success(SuccessStatus.SUCCESS_204);
    }
}
