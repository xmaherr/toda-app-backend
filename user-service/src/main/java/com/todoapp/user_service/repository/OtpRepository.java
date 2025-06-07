package com.todoapp.user_service.repository;


import com.todoapp.user_service.entity.Otp;
import com.todoapp.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserAndOtpCode(User user, String otpCode);
    Optional<Otp> findByUser(User user);
    void deleteByUser(User user);
    Optional<Otp> findByUserAndOtpCodeAndExpirationTimeAfter(User user, String otpCode, LocalDateTime currentTime);
}
