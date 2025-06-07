package com.todoapp.user_service.repository;




import com.todoapp.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // لإرجاع قيمة قد تكون موجودة أو لا

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);


}