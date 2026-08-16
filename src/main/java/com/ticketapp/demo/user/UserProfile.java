package com.ticketapp.demo.user;

import java.util.UUID;

public record UserProfile(
        UUID userId,
        String email,
        String displayName,
        String role
) {
}
