package com.election.evm.repository;

import com.election.evm.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, String> {
    List<Incident> findByCreatedByIdOrderByCreatedAtDesc(String createdById);
}
