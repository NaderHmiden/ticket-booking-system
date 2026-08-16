package com.ticketapp.demo.user;

import com.ticketapp.demo.user.internal.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfile me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new IllegalArgumentException("Unauthenticated");
        }
        return authService.profile(userId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        String message = e.getMessage() == null ? "bad request" : e.getMessage();
        HttpStatus status = message.toLowerCase().contains("unauthenticated")
                ? HttpStatus.UNAUTHORIZED
                : (message.toLowerCase().contains("already") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(status).body(Map.of("error", status.name(), "message", message));
    }
}
