package com.election.evm.config;

import com.election.evm.entity.ElectionResult;
import com.election.evm.entity.User;
import com.election.evm.repository.ElectionResultRepository;
import com.election.evm.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ElectionResultRepository electionResultRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public DataInitializer(
            UserRepository userRepository,
            ElectionResultRepository electionResultRepository,
            PasswordEncoder passwordEncoder,
            ModelMapper modelMapper
    ) {
        this.userRepository = userRepository;
        this.electionResultRepository = electionResultRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedElectionResults();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmailIgnoreCase("admin@ems.local")) {
            return;
        }

        User admin = new User();
        admin.setName("System Admin");
        admin.setEmail("admin@ems.local");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("admin");
        userRepository.save(admin);
    }

    private void seedElectionResults() {
        if (electionResultRepository.count() > 0) {
            return;
        }

        electionResultRepository.save(createResult(
                "North City",
                "Booth 12 - Community Hall",
                "Aditi Sharma",
                "Progress Alliance",
                1423,
                2980,
                "final"
        ));

        electionResultRepository.save(createResult(
                "West Valley",
                "Booth 44 - Government School",
                "Rohit Verma",
                "Civic Front",
                1189,
                2510,
                "final"
        ));

        electionResultRepository.save(createResult(
                "South Ridge",
                "Booth 8 - Primary School",
                "Neha Iyer",
                "People First",
                1335,
                2876,
                "in-progress"
        ));
    }

    private ElectionResult createResult(
            String constituency,
            String boothName,
            String winner,
            String party,
            int votes,
            int totalVotes,
            String status
    ) {
        ElectionResult result = new ElectionResult();
        result.setConstituency(constituency);
        result.setBoothName(boothName);
        result.setWinner(winner);
        result.setParty(party);
        result.setVotes(votes);
        result.setTotalVotes(totalVotes);
        result.setStatus(status);
        return result;
    }
}
