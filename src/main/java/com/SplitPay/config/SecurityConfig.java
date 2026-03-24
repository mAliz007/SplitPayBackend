package com.SplitPay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. Origins (Ensure your Blazor port 5293 is here)
        config.setAllowedOrigins(List.of(
                "http://localhost:5293",
                "https://localhost:7001",
                "http://localhost:5145"
        ));

        // 2. Methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 3. Headers (Add "Accept" and others that Blazor's HttpClient sends)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));

        // 4. THE CRITICAL MISSING PIECE:
        // Without this, 'Access-Control-Allow-Credentials' is blank, and the browser blocks it.
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Hook in the CORS config you already wrote
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Disable CSRF (Stateless API with JWT/Cookies doesn't need it for now)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Define the "Public" vs "Private" zones
                .authorizeHttpRequests(auth -> auth
                        // This MUST match your AuthController @RequestMapping("/api/auth")
                        .requestMatchers("/api/auth/**").permitAll()

                        // Everything else (like future Dashboard data) requires login
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}