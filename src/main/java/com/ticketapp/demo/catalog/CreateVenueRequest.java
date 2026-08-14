package com.ticketapp.demo.catalog;

import jakarta.validation.constraints.NotBlank;

public record CreateVenueRequest(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String timezone
) {
}