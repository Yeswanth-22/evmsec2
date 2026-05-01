package com.election.evm.repository;

import com.election.evm.entity.AnalystReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalystReportRepository extends JpaRepository<AnalystReport, String> {
    List<AnalystReport> findByCreatedByIdOrderByUpdatedAtDesc(String createdById);
}
