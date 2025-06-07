package com.todoapp.user_service.repository;


import com.todoapp.user_service.entity.Jwt;
import com.todoapp.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JwtRepository extends JpaRepository<Jwt, Long> {
    List<Jwt> findByUser(User user);
    void deleteByUser(User user);
    Optional<Jwt> findByToken(String token);
}