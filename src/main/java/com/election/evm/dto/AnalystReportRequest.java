package com.election.evm.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalystReportRequest(
        @NotBlank String title,
        @NotBlank String summary,
        @NotBlank String recommendation,
        @NotBlank String status,
        String createdBy,
        String createdById
) {
}
