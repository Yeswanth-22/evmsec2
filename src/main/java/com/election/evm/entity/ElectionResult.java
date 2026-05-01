package com.election.evm.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "election_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String constituency;

    @Column(nullable = false)
    private String boothName;

    @Column(nullable = false)
    private String winner;

    @Column(nullable = false)
    private String party;

    @Column(nullable = false)
    private int votes;

    @Column(nullable = false)
    private int totalVotes;

    @Column(nullable = false)
    private String status;
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
