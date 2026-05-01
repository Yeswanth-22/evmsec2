package com.election.evm.dto;

import com.election.evm.entity.User;

public record AuthResponse(
        String token,
        User user
) {
}
