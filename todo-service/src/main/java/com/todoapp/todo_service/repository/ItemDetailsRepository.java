package com.todoapp.todo_service.repository;

import com.todoapp.todo_service.entity.ItemDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemDetailsRepository extends JpaRepository<ItemDetails, Long> {

}