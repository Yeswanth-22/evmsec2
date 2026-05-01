package com.election.evm.dto;

import com.election.evm.entity.ElectionResult;
import com.election.evm.entity.FraudReport;
import com.election.evm.entity.RegionalStatistic;
import com.election.evm.entity.TurnoutData;

import java.util.List;

public record BulkUploadResult(
        List<ElectionResult> electionResults,
        List<FraudReport> fraudReports,
        List<RegionalStatistic> regionalStatistics,
        List<TurnoutData> turnoutData,
        DashboardStats dashboardStats,
        List<String> errors
) {
}
