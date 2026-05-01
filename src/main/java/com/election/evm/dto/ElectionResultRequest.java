package com.election.evm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ElectionResultRequest(
        @NotBlank String constituency,
        @NotBlank String boothName,
        @NotBlank String winner,
        @NotBlank String party,
        @NotNull @PositiveOrZero Integer votes,
        @NotNull @PositiveOrZero Integer totalVotes,
        @NotBlank String status
) {
}
