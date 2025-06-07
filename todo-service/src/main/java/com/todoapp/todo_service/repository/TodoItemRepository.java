package com.todoapp.todo_service.repository;

import com.todoapp.todo_service.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {

    List<TodoItem> findByUserEmailAndTitleContainingIgnoreCase(String userEmail, String title);


    List<TodoItem> findByUserEmail(String userEmail);


    Optional<TodoItem> findByIdAndUserEmail(Long id, String userEmail);
}