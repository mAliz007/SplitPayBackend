package com.SplitPay.service;

import com.SplitPay.model.User;
import com.SplitPay.model.UserSession;
import com.SplitPay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.SplitPay.repository.UserSessionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

    public String registerUser(String email, String rawPassword) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.isVerified()) {
                // Fintech security: Don't allow re-registration of active accounts
                throw new RuntimeException("USER_ALREADY_EXISTS");
            } else {
                // User exists but isn't verified. Send a fresh 10-min link.
                String token = jwtService.generateVerificationToken(email);
                emailService.sendVerificationEmail(email, token);
                return "VERIFICATION_SENT_AGAIN";
            }
        }

        // New User Flow
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setVerified(false);
        userRepository.save(newUser);

        String token = jwtService.generateVerificationToken(email);
        emailService.sendVerificationEmail(email, token);
        return "SUCCESS";
    }

    public void verifyUser(String token) {
        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("TOKEN_EXPIRED_OR_INVALID");
        }

        String email = jwtService.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        user.setVerified(true);
        // Clear token or update state to prevent double-use if desired
        userRepository.save(user);
    }

    public User verifyCredentials(String email, String password) {
        // 1. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // 2. Check if the password matches the BCrypt hash in DB
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("INVALID_CREDENTIALS");
        }

        // 3. Ensure they verified their email
        if (!user.isVerified()) {
            throw new RuntimeException("ACCOUNT_NOT_VERIFIED");
        }

        return user;
    }

    // And let's add the session saving logic while we are here:
    public void saveSession(String userId, String refreshToken) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setRefreshToken(refreshToken);
        // Set expiry to 7 days from now
        session.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));

        userSessionRepository.save(session);
    }
}