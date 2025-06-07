package com.todoapp.user_service.service;


import com.todoapp.user_service.dto.UserUpdateRequest;
import com.todoapp.user_service.entity.User;
import com.todoapp.user_service.entity.Otp;
import com.todoapp.user_service.repository.UserRepository;
import com.todoapp.user_service.repository.OtpRepository;
import com.todoapp.user_service.exception.ResourceNotFoundException; // هنبنيها بعدين
import com.todoapp.user_service.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // هنستخدمها لتشفير الباسورد
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // مهمة للعمليات على الداتا بيز
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;


    @Autowired
    public UserService(UserRepository userRepository, OtpRepository otpRepository, PasswordEncoder passwordEncoder, EmailService emailService, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
    }



    @Transactional
    public User registerUser(User user) {

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setEnabled(false);
        User savedUser = userRepository.save(user);


        String otpCode = generateOtp();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(10); 
        Otp otp = new Otp(null, otpCode, expirationTime, savedUser);
        otpRepository.save(otp);


         if (emailService != null) {
             emailService.sendOtpEmail(savedUser.getEmail(), otpCode);
         }

        return savedUser;
    }

    @Transactional
    public boolean activateUser(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));


        if (user.isEnabled()) {

            return true;
        }

        Optional<Otp> otpOptional = otpRepository.findByUserAndOtpCode(user, otpCode);

        if (otpOptional.isPresent()) {
            Otp otp = otpOptional.get();

            if (otp.getExpirationTime().isAfter(LocalDateTime.now())) {
                user.setEnabled(true);
                userRepository.save(user);
                otpRepository.deleteByUser(user);
                return true;
            }
        }
        return false;
    }

    public User loginUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is not activated. Please activate your account first.");
        }
        return user;
    }

    public void regenerateOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        otpRepository.findByUser(user).ifPresent(otpRepository::delete);

        String otpCode = generateOtp();
        Otp newOtp = new Otp(null, otpCode, LocalDateTime.now().plusMinutes(5), user);
        otpRepository.save(newOtp);

        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        otpRepository.findByUser(user).ifPresent(otpRepository::delete);

        String otpCode = generateOtp();
        Otp newOtp = new Otp(null, otpCode, LocalDateTime.now().plusMinutes(5), user); // OTP for 5 minutes
        otpRepository.save(newOtp);

        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    public void changePassword(String email, String otpCode, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Otp otp = otpRepository.findByUserAndOtpCodeAndExpirationTimeAfter(user, otpCode, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);


        otpRepository.delete(otp);
    }

    @Transactional
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateUser(String currentUserEmail, UserUpdateRequest request) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));


        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }


        if (request.getNewEmail() != null && !request.getNewEmail().isEmpty() && !request.getNewEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getNewEmail()).isPresent()) {
                throw new IllegalArgumentException("New email is already taken by another user.");
            }
            user.setEmail(request.getNewEmail());

             user.setEnabled(false);
             regenerateOtp(user.getEmail());
        }


        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));


        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password. Cannot delete account.");
        }


        otpRepository.findByUser(user).ifPresent(otpRepository::delete);

        userRepository.delete(user);
    }

    public User validateTokenAndGetUser(String token) {
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (jwtTokenProvider.validateToken(actualToken)) {
            String userEmail = jwtTokenProvider.getUsername(actualToken);
            return userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for provided token email: " + userEmail));
        } else {
            throw new IllegalArgumentException("Invalid or expired JWT token.");
        }
    }



    // --- Helper Methods ---


    private String generateOtp() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // TODO: Password Reset methods (forgetPassword, changePassword) will be added here
    // TODO: JWT related methods (login, checkToken) will be added here or in a separate AuthService
}
