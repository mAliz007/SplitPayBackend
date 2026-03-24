package com.SplitPay.repository;

import com.SplitPay.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    // This allows us to search MongoDB by email: db.users.find({email: "..."})
    Optional<User> findByEmail(String email);
}