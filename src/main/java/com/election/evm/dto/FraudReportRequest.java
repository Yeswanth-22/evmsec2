package com.election.evm.dto;

import jakarta.validation.constraints.NotBlank;

public record FraudReportRequest(
        @NotBlank String title,
        @NotBlank String category,
        String status,
        @NotBlank String description,
        @NotBlank String location,
        String createdBy,
        String createdById
) {
}
