package org.solmate.domain.todo.service;

import java.util.List;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.todo.dto.request.TodoCreateRequest;
import org.solmate.domain.todo.dto.request.TodoUpdateRequest;
import org.solmate.domain.todo.dto.response.TodoResponse;
import org.solmate.domain.todo.entity.Todo;
import org.solmate.domain.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;

    @Override
    @Transactional
    public TodoResponse create(TodoCreateRequest request) {
        Todo todo = Todo.builder()
                .title(request.getTitle())
                .build();

        return new TodoResponse(todoRepository.save(todo));
    }

    @Override
    public List<TodoResponse> getAll() {
        return todoRepository.findAll().stream()
                .map(TodoResponse::new)
                .toList();
    }

    @Override
    public TodoResponse getOne(Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        return new TodoResponse(todo);
    }

    @Override
    @Transactional
    public TodoResponse update(Long id, TodoUpdateRequest request) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        todo.update(request.getTitle());

        return new TodoResponse(todo);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        todoRepository.delete(todo);
    }
}
