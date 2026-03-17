package org.solmate.domain.todo.service;

import java.util.List;

import org.solmate.domain.todo.dto.request.TodoCreateRequest;
import org.solmate.domain.todo.dto.request.TodoUpdateRequest;
import org.solmate.domain.todo.dto.response.TodoResponse;

public interface TodoService {

    TodoResponse create(TodoCreateRequest request);          // 생성

    List<TodoResponse> getAll();                             // 전체 조회

    TodoResponse getOne(Long id);                            // 단건 조회

    TodoResponse update(Long id, TodoUpdateRequest request); // 수정

    void delete(Long id);                                    // 삭제
}
