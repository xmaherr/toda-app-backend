package com.todoapp.todo_service.service;

import com.todoapp.todo_service.dto.TodoItemRequest;
import com.todoapp.todo_service.dto.TodoItemResponse;
import com.todoapp.todo_service.entity.ItemDetails;
import com.todoapp.todo_service.entity.TodoItem;
import com.todoapp.todo_service.exception.ResourceNotFoundException;
import com.todoapp.todo_service.repository.ItemDetailsRepository;
import com.todoapp.todo_service.repository.TodoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final TodoItemRepository todoItemRepository;
    private final ItemDetailsRepository itemDetailsRepository;

    @Autowired
    public TodoService(TodoItemRepository todoItemRepository, ItemDetailsRepository itemDetailsRepository) {
        this.todoItemRepository = todoItemRepository;
        this.itemDetailsRepository = itemDetailsRepository;
    }


    private TodoItemResponse convertToDto(TodoItem todoItem) {
        return new TodoItemResponse(
                todoItem.getId(),
                todoItem.getTitle(),
                todoItem.getItemDetails().getDescription(),
                todoItem.getItemDetails().getCreatedAt(),
                todoItem.getItemDetails().getPriority(),
                todoItem.getItemDetails().getStatus(),
                todoItem.getUserEmail()
        );
    }


    public TodoItemResponse addTodoItem(String userEmail, TodoItemRequest request) {

        ItemDetails itemDetails = new ItemDetails();
        itemDetails.setDescription(request.getDescription());
        itemDetails.setCreatedAt(LocalDateTime.now());
        itemDetails.setPriority(request.getPriority());
        itemDetails.setStatus(request.getStatus());
        ItemDetails savedItemDetails = itemDetailsRepository.save(itemDetails); // Save details first


        TodoItem todoItem = new TodoItem();
        todoItem.setTitle(request.getTitle());
        todoItem.setUserEmail(userEmail);
        todoItem.setItemDetails(savedItemDetails);

        TodoItem savedTodoItem = todoItemRepository.save(todoItem);
        return convertToDto(savedTodoItem);
    }


    public List<TodoItemResponse> getAllTodoItems(String userEmail) {
        List<TodoItem> todoItems = todoItemRepository.findByUserEmail(userEmail);
        return todoItems.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    public TodoItemResponse getTodoItemById(Long id, String userEmail) {
        TodoItem todoItem = todoItemRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Todo item not found or you don't have access to it with ID: " + id));
        return convertToDto(todoItem);
    }


    public TodoItemResponse updateTodoItem(Long id, String userEmail, TodoItemRequest request) {
        TodoItem existingTodoItem = todoItemRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Todo item not found or you don't have access to it with ID: " + id));


        existingTodoItem.setTitle(request.getTitle());


        ItemDetails existingItemDetails = existingTodoItem.getItemDetails();
        if (existingItemDetails == null) {

            existingItemDetails = new ItemDetails();
            existingItemDetails.setCreatedAt(LocalDateTime.now());
            existingTodoItem.setItemDetails(itemDetailsRepository.save(existingItemDetails));
        }
        existingItemDetails.setDescription(request.getDescription());
        existingItemDetails.setPriority(request.getPriority());
        existingItemDetails.setStatus(request.getStatus());
        itemDetailsRepository.save(existingItemDetails);

        TodoItem updatedTodoItem = todoItemRepository.save(existingTodoItem);
        return convertToDto(updatedTodoItem);
    }


    public void deleteTodoItem(Long id, String userEmail) {
        TodoItem todoItem = todoItemRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Todo item not found or you don't have access to it with ID: " + id));

        itemDetailsRepository.delete(todoItem.getItemDetails());
        todoItemRepository.delete(todoItem);
    }


    public List<TodoItemResponse> searchTodoItemsByTitle(String userEmail, String title) {
        List<TodoItem> todoItems = todoItemRepository.findByUserEmailAndTitleContainingIgnoreCase(userEmail, title);
        return todoItems.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}