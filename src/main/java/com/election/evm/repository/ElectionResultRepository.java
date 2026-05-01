package com.election.evm.repository;

import com.election.evm.entity.ElectionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ElectionResultRepository extends JpaRepository<ElectionResult, String> {
    Optional<ElectionResult> findByConstituencyIgnoreCaseAndBoothNameIgnoreCase(String constituency, String boothName);
}
