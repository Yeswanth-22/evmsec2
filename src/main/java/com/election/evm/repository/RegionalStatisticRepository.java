package com.election.evm.repository;

import com.election.evm.entity.RegionalStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionalStatisticRepository extends JpaRepository<RegionalStatistic, String> {
    Optional<RegionalStatistic> findByRegionIgnoreCase(String region);
}
