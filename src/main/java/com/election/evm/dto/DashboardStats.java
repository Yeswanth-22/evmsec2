package com.election.evm.dto;

public record DashboardStats(
        int users,
        int incidents,
        int fraudReports,
        int analystReports,
        int electionResults
) {
}
