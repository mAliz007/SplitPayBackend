package com.SplitPay.repository;

import com.SplitPay.model.UserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserSessionRepository extends MongoRepository<UserSession, String> {
    Optional<UserSession> findByRefreshToken(String token);
    void deleteByUserId(String userId); // Use this for "Logout from all devices"
}