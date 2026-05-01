package com.election.evm.dto;

import jakarta.validation.constraints.NotBlank;

public record IncidentRequest(
        @NotBlank String title,
        @NotBlank String location,
        @NotBlank String severity,
        @NotBlank String status,
        @NotBlank String details,
        String createdBy,
        String createdById
) {
}
