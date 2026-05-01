package com.election.evm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OtpSendRequest(
        @NotBlank @Email String email
) {
}
