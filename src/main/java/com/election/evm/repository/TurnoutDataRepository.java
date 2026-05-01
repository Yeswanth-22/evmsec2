package com.election.evm.repository;

import com.election.evm.entity.TurnoutData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TurnoutDataRepository extends JpaRepository<TurnoutData, String> {
    Optional<TurnoutData> findByRegionIgnoreCase(String region);
}
