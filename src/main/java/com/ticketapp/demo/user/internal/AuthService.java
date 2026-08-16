package com.ticketapp.demo.user.internal;

import com.ticketapp.demo.user.AuthResponse;
import com.ticketapp.demo.user.LoginRequest;
import com.ticketapp.demo.user.RegisterRequest;
import com.ticketapp.demo.user.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                "USER"
        );
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfile profile(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserProfile(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    private AuthResponse toAuthResponse(AppUser user) {
        String token = jwtService.issueToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
