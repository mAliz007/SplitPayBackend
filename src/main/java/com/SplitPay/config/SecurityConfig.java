package com.SplitPay.config;

import com.SplitPay.model.User;
import com.SplitPay.repository.UserRepository; // Import your repo
import com.SplitPay.service.JwtService;      // Import your JwtService
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5293", "https://localhost:7001", "http://localhost:5145"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                // --- NEW OAUTH2 LOGIC ---
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                            String email = oAuth2User.getAttribute("email");
                            String name = oAuth2User.getAttribute("name");
                            String googleId = oAuth2User.getAttribute("sub");
                            String picture = oAuth2User.getAttribute("picture");

                            // 1. Find or Create User
                            User user = userRepository.findByEmail(email).orElseGet(() -> {
                                User newUser = new User();
                                newUser.setEmail(email);
                                newUser.setName(name);
                                newUser.setGoogleId(googleId);
                                newUser.setPicture(picture);
                                newUser.setProvider(User.AuthProvider.GOOGLE);
                                newUser.setVerified(true);
                                return userRepository.save(newUser);
                            });

                            // 2. Generate Tokens (Reusing your JWT Logic)
                            String accessToken = jwtService.generateAccessToken(user.getEmail());
                            String refreshToken = jwtService.generateRefreshToken(user.getEmail());

                            // 3. Set HttpOnly Cookies
                            ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                                    .httpOnly(true).secure(false).path("/").maxAge(15 * 60).sameSite("Lax").build();

                            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                                    .httpOnly(true).secure(false).path("/api/auth/refresh").maxAge(7 * 24 * 60 * 60).sameSite("Lax").build();

                            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                            // 4. Final Redirect to Blazor Dashboard
                            response.sendRedirect("http://localhost:5293/dashboard");
                        })
                );

        return http.build();
    }
}