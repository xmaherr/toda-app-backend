package com.todoapp.todo_service.controller;

import com.todoapp.todo_service.dto.TodoItemRequest;
import com.todoapp.todo_service.dto.TodoItemResponse;
import com.todoapp.todo_service.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoItemController {

    private final TodoService todoService;

    @Autowired
    public TodoItemController(TodoService todoService) {
        this.todoService = todoService;
    }



    @PostMapping
    public ResponseEntity<TodoItemResponse> addTodoItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TodoItemRequest request) {


        TodoItemResponse response = todoService.addTodoItem(userDetails.getUsername(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }



    @GetMapping
    public ResponseEntity<List<TodoItemResponse>> getAllTodoItems(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<TodoItemResponse> response = todoService.getAllTodoItems(userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @GetMapping("/{id}")
    public ResponseEntity<TodoItemResponse> getTodoItemById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        TodoItemResponse response = todoService.getTodoItemById(id, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @PutMapping("/{id}")
    public ResponseEntity<TodoItemResponse> updateTodoItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody TodoItemRequest request) {

        TodoItemResponse response = todoService.updateTodoItem(id, userDetails.getUsername(), request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTodoItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        todoService.deleteTodoItem(id, userDetails.getUsername());
        return new ResponseEntity<>("Todo item deleted successfully!", HttpStatus.OK);
    }



    @GetMapping("/search")
    public ResponseEntity<List<TodoItemResponse>> searchTodoItemsByTitle(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title) {

        List<TodoItemResponse> response = todoService.searchTodoItemsByTitle(userDetails.getUsername(), title);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}