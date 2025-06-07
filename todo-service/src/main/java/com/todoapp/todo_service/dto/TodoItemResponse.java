package com.todoapp.todo_service.dto;

import com.todoapp.todo_service.entity.enums.Priority;
import com.todoapp.todo_service.entity.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoItemResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private Priority priority;
    private Status status;
    private String userEmail;
}