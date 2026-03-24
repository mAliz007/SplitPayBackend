package com.SplitPay.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "users")
public class User {
    // Getters and Setters
    @Id
    private String id;
    private String email;
    private String password; // Will be null for pure Google users
    private boolean isVerified = false;

    // --- OAuth2 Fields ---
    private String googleId;    // The unique 'sub' ID from Google
    private String name;        // User's full name from Google
    private String picture;     // Profile picture URL
    private AuthProvider provider = AuthProvider.LOCAL; // Default to local

    public enum AuthProvider {
        LOCAL, GOOGLE
    }

    // Default Constructor
    public User() {}

}