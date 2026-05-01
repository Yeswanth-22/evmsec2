package com.election.evm.service;

import com.election.evm.dto.AnalystReportRequest;
import com.election.evm.dto.ApiResponse;
import com.election.evm.dto.AuthResponse;
import com.election.evm.dto.AuthRequest;
import com.election.evm.dto.BulkUploadResult;
import com.election.evm.dto.DashboardStats;
import com.election.evm.dto.ElectionResultRequest;
import com.election.evm.dto.FraudReportRequest;
import com.election.evm.dto.IncidentRequest;
import com.election.evm.dto.LoginRequest;
import com.election.evm.dto.UserRequest;
import com.election.evm.entity.AnalystReport;
import com.election.evm.entity.ElectionResult;
import com.election.evm.entity.FraudReport;
import com.election.evm.entity.Incident;
import com.election.evm.entity.RegionalStatistic;
import com.election.evm.entity.TurnoutData;
import com.election.evm.entity.User;
import com.election.evm.repository.AnalystReportRepository;
import com.election.evm.repository.ElectionResultRepository;
import com.election.evm.repository.FraudReportRepository;
import com.election.evm.repository.IncidentRepository;
import com.election.evm.repository.RegionalStatisticRepository;
import com.election.evm.repository.TurnoutDataRepository;
import com.election.evm.repository.UserRepository;
import com.election.evm.security.JwtService;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class EvmService {
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final FraudReportRepository fraudReportRepository;
    private final AnalystReportRepository analystReportRepository;
    private final ElectionResultRepository electionResultRepository;
    private final RegionalStatisticRepository regionalStatisticRepository;
    private final TurnoutDataRepository turnoutDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public EvmService(
            UserRepository userRepository,
            IncidentRepository incidentRepository,
            FraudReportRepository fraudReportRepository,
            AnalystReportRepository analystReportRepository,
            ElectionResultRepository electionResultRepository,
            RegionalStatisticRepository regionalStatisticRepository,
            TurnoutDataRepository turnoutDataRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService
    ) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.fraudReportRepository = fraudReportRepository;
        this.analystReportRepository = analystReportRepository;
        this.electionResultRepository = electionResultRepository;
        this.regionalStatisticRepository = regionalStatisticRepository;
        this.turnoutDataRepository = turnoutDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    @Transactional
    public ApiResponse<User> register(AuthRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ApiResponse.failure("Email is already registered.");
        }

        if (!otpService.isVerified(email)) {
            return ApiResponse.failure("Please verify OTP for this email before registering.");
        }

        if (!request.otp().trim().isEmpty()) {
            ApiResponse<Void> otpCheck = otpService.verifyOtp(email, request.otp());
            if (!otpCheck.success()) {
                return ApiResponse.failure(otpCheck.message());
            }
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(normalizeRole(request.role()));

        user = userRepository.save(user);
        otpService.clear(email);
        return ApiResponse.success("Registration successful.", sanitizeUser(user));
    }

    public ApiResponse<AuthResponse> login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);

        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().getPassword())) {
            return ApiResponse.failure("Invalid email or password.");
        }

        User current = user.get();
        String token = jwtService.generateToken(String.valueOf(current.getId()), current.getEmail(), current.getRole());
        return ApiResponse.success("Login successful.", new AuthResponse(token, sanitizeUser(current)));
    }

    public ApiResponse<AuthResponse> refreshToken(String token) {
        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            String userId = jwtService.extractUserId(token);

            Optional<User> user = userRepository.findByEmailIgnoreCase(email);
            if (user.isEmpty()) {
                return ApiResponse.failure("User not found.");
            }

            String newToken = jwtService.generateToken(userId, email, role);
            return ApiResponse.success("Token refreshed.", new AuthResponse(newToken, sanitizeUser(user.get())));
        } catch (Exception e) {
            return ApiResponse.failure("Invalid or expired token.");
        }
    }

    public ApiResponse<AuthResponse> handleOAuthSuccess() {
        User actor = getAuthenticatedUser();
        String token = jwtService.generateToken(String.valueOf(actor.getId()), actor.getEmail(), actor.getRole());
        return ApiResponse.success("OAuth login successful.", new AuthResponse(token, sanitizeUser(actor)));
    }

    public ApiResponse<User> getCurrentUser() {
        User actor = getAuthenticatedUser();
        return ApiResponse.success("Current user fetched.", sanitizeUser(actor));
    }

    public ApiResponse<List<User>> getUsers() {
        return ApiResponse.success("Users fetched.", userRepository.findAll().stream().map(this::sanitizeUser).toList());
    }

    @Transactional
    public ApiResponse<User> createUser(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (request.password() == null || request.password().isBlank()) {
            return ApiResponse.failure("Password is required.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ApiResponse.failure("Email already exists.");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(normalizeRole(request.role()));

        user = userRepository.save(user);
        return ApiResponse.success("User created.", sanitizeUser(user));
    }

    @Transactional
    public ApiResponse<User> updateUser(Long userId, UserRequest request) {
        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty()) {
            return ApiResponse.failure("User not found.");
        }

        String email = normalizeEmail(request.email());
        Optional<User> existingByEmail = userRepository.findByEmailIgnoreCase(email);
        if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(userId)) {
            return ApiResponse.failure("Email already exists.");
        }

        User user = found.get();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(normalizeRole(request.role()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        user = userRepository.save(user);
        return ApiResponse.success("User updated.", sanitizeUser(user));
    }

    @Transactional
    public ApiResponse<Void> deleteUser(Long userId) {
        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty()) {
            return ApiResponse.failure("User not found.");
        }

        User user = found.get();
        if (isLastAdmin(user)) {
            return ApiResponse.failure("At least one admin user must remain.");
        }

        userRepository.deleteById(userId);
        return ApiResponse.success("User removed.");
    }

    public ApiResponse<List<Incident>> getIncidents() {
        return ApiResponse.success("Incidents fetched.", incidentRepository.findAll());
    }

    @Transactional
    public ApiResponse<Incident> createIncident(IncidentRequest request) {
        User actor = getAuthenticatedUser();

        Incident incident = new Incident();
        incident.setTitle(request.title().trim());
        incident.setLocation(request.location().trim());
        incident.setSeverity(request.severity().trim());
        incident.setStatus(request.status().trim());
        incident.setDetails(request.details().trim());
        incident.setCreatedBy(actor.getName());
        incident.setCreatedById(String.valueOf(actor.getId()));

        incident = incidentRepository.save(incident);
        return ApiResponse.success("Incident added.", incident);
    }

    @Transactional
    public ApiResponse<Incident> updateIncident(String incidentId, IncidentRequest request) {
        User actor = getAuthenticatedUser();
        Optional<Incident> found = incidentRepository.findById(incidentId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Incident not found.");
        }

        Incident incident = found.get();
        if (!canMutateOwnRecord(actor, incident.getCreatedById())) {
            return ApiResponse.failure("You are not allowed to update this incident.");
        }

        incident.setTitle(request.title().trim());
        incident.setLocation(request.location().trim());
        incident.setSeverity(request.severity().trim());
        incident.setStatus(request.status().trim());
        incident.setDetails(request.details().trim());

        incident = incidentRepository.save(incident);
        return ApiResponse.success("Incident updated.", incident);
    }

    @Transactional
    public ApiResponse<Void> deleteIncident(String incidentId) {
        User actor = getAuthenticatedUser();
        Optional<Incident> found = incidentRepository.findById(incidentId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Incident not found.");
        }

        if (!canMutateOwnRecord(actor, found.get().getCreatedById())) {
            return ApiResponse.failure("You are not allowed to delete this incident.");
        }

        incidentRepository.deleteById(incidentId);
        return ApiResponse.success("Incident deleted.");
    }

    public ApiResponse<List<FraudReport>> getFraudReports() {
        return ApiResponse.success("Fraud reports fetched.", fraudReportRepository.findAll());
    }

    @Transactional
    public ApiResponse<FraudReport> createFraudReport(FraudReportRequest request) {
        User actor = getAuthenticatedUser();

        FraudReport report = new FraudReport();
        report.setTitle(request.title().trim());
        report.setCategory(request.category().trim());
        report.setStatus(defaultText(request.status(), "submitted"));
        report.setDescription(request.description().trim());
        report.setLocation(request.location().trim());
        report.setCreatedBy(actor.getName());
        report.setCreatedById(String.valueOf(actor.getId()));

        report = fraudReportRepository.save(report);
        return ApiResponse.success("Fraud report submitted.", report);
    }

    @Transactional
    public ApiResponse<FraudReport> updateFraudReport(String reportId, FraudReportRequest request) {
        User actor = getAuthenticatedUser();
        Optional<FraudReport> found = fraudReportRepository.findById(reportId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Fraud report not found.");
        }

        FraudReport report = found.get();
        if (!canMutateOwnRecord(actor, report.getCreatedById())) {
            return ApiResponse.failure("You are not allowed to update this fraud report.");
        }

        report.setTitle(request.title().trim());
        report.setCategory(request.category().trim());
        report.setStatus(defaultText(request.status(), report.getStatus()));
        report.setDescription(request.description().trim());
        report.setLocation(request.location().trim());

        report = fraudReportRepository.save(report);
        return ApiResponse.success("Fraud report updated.", report);
    }

    @Transactional
    public ApiResponse<Void> deleteFraudReport(String reportId) {
        User actor = getAuthenticatedUser();
        Optional<FraudReport> found = fraudReportRepository.findById(reportId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Fraud report not found.");
        }

        if (!canMutateOwnRecord(actor, found.get().getCreatedById())) {
            return ApiResponse.failure("You are not allowed to delete this fraud report.");
        }

        fraudReportRepository.deleteById(reportId);
        return ApiResponse.success("Fraud report deleted.");
    }

    public ApiResponse<List<AnalystReport>> getAnalystReports() {
        return ApiResponse.success("Analyst reports fetched.", analystReportRepository.findAll());
    }

    @Transactional
    public ApiResponse<AnalystReport> createAnalystReport(AnalystReportRequest request) {
        User actor = getAuthenticatedUser();

        AnalystReport report = new AnalystReport();
        report.setTitle(request.title().trim());
        report.setSummary(request.summary().trim());
        report.setRecommendation(request.recommendation().trim());
        report.setStatus(request.status().trim());
        report.setCreatedBy(actor.getName());
        report.setCreatedById(String.valueOf(actor.getId()));

        report = analystReportRepository.save(report);
        return ApiResponse.success("Analysis report created.", report);
    }

    @Transactional
    public ApiResponse<AnalystReport> updateAnalystReport(String reportId, AnalystReportRequest request) {
        User actor = getAuthenticatedUser();
        Optional<AnalystReport> found = analystReportRepository.findById(reportId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Analyst report not found.");
        }

        AnalystReport report = found.get();
        if (!canMutateOwnRecord(actor, report.getCreatedById())) {
            return ApiResponse.failure("You are not allowed to update this analyst report.");
        }

        report.setTitle(request.title().trim());
        report.setSummary(request.summary().trim());
        report.setRecommendation(request.recommendation().trim());
        report.setStatus(request.status().trim());

        report = analystReportRepository.save(report);
        return ApiResponse.success("Analysis report updated.", report);
    }

    @Transactional
    public ApiResponse<Void> deleteAnalystReport(String reportId) {
        User actor = getAuthenticatedUser();
        Optional<AnalystReport> found = analystReportRepository.findById(reportId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Analyst report not found.");
        }

        if (!canMutateOwnRecord(actor, found.get().getCreatedById())) {
            return ApiResponse.failure("You are not allowed to delete this analyst report.");
        }

        analystReportRepository.deleteById(reportId);
        return ApiResponse.success("Analysis report deleted.");
    }

    public ApiResponse<List<ElectionResult>> getElectionResults() {
        return ApiResponse.success("Election results fetched.", electionResultRepository.findAll());
    }

    @Transactional
    public ApiResponse<ElectionResult> createElectionResult(ElectionResultRequest request) {
        ElectionResult result = new ElectionResult();
        result.setConstituency(request.constituency().trim());
        result.setBoothName(request.boothName().trim());
        result.setWinner(request.winner().trim());
        result.setParty(request.party().trim());
        result.setVotes(request.votes());
        result.setTotalVotes(request.totalVotes());
        result.setStatus(request.status().trim());

        result = electionResultRepository.save(result);
        return ApiResponse.success("Election result added.", result);
    }

    @Transactional
    public ApiResponse<BulkUploadResult> bulkUploadElectionData(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.failure("No file provided for upload.");
        }

        List<String> errors = new ArrayList<>();
        List<ElectionResult> uploadedElectionResults = new ArrayList<>();
        List<FraudReport> uploadedFraudReports = new ArrayList<>();
        List<RegionalStatistic> uploadedRegionalStatistics = new ArrayList<>();
        List<TurnoutData> uploadedTurnout = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName().trim();
                if (sheetName.equalsIgnoreCase("ElectionResults")) {
                    parseElectionResultsSheet(sheet, uploadedElectionResults, errors);
                } else if (sheetName.equalsIgnoreCase("FraudReports")) {
                    parseFraudReportsSheet(sheet, uploadedFraudReports, errors);
                } else if (sheetName.equalsIgnoreCase("RegionalStatistics")) {
                    parseRegionalStatisticsSheet(sheet, uploadedRegionalStatistics, errors);
                } else if (sheetName.equalsIgnoreCase("TurnoutData") || sheetName.equalsIgnoreCase("Turnout")) {
                    parseTurnoutDataSheet(sheet, uploadedTurnout, errors);
                } else {
                    errors.add("Skipped unknown sheet: " + sheetName);
                }
            }
        } catch (IOException e) {
            return ApiResponse.failure("Unable to read Excel file: " + e.getMessage());
        }

        DashboardStats stats = new DashboardStats(
                (int) userRepository.count(),
                (int) incidentRepository.count(),
                (int) fraudReportRepository.count(),
                (int) analystReportRepository.count(),
                (int) electionResultRepository.count()
        );

        BulkUploadResult result = new BulkUploadResult(
                uploadedElectionResults,
                uploadedFraudReports,
                uploadedRegionalStatistics,
                uploadedTurnout,
                stats,
                errors
        );

        return ApiResponse.success("Bulk Excel upload processed.", result);
    }

    private void parseElectionResultsSheet(Sheet sheet, List<ElectionResult> uploadedElectionResults, List<String> errors) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String constituency = getCellValueAsString(row.getCell(0));
            String boothName = getCellValueAsString(row.getCell(1));
            String winner = getCellValueAsString(row.getCell(2));
            String party = getCellValueAsString(row.getCell(3));
            Integer votes = getCellValueAsInteger(row.getCell(4));
            Integer totalVotes = getCellValueAsInteger(row.getCell(5));
            String status = getCellValueAsString(row.getCell(6));

            if (constituency.isBlank() || boothName.isBlank() || winner.isBlank() || party.isBlank() || votes == null || totalVotes == null) {
                errors.add("ElectionResults sheet row " + (rowIndex + 1) + " is missing required values.");
                continue;
            }

            ElectionResult electionResult = electionResultRepository
                    .findByConstituencyIgnoreCaseAndBoothNameIgnoreCase(constituency, boothName)
                    .orElseGet(ElectionResult::new);

            electionResult.setConstituency(constituency.trim());
            electionResult.setBoothName(boothName.trim());
            electionResult.setWinner(winner.trim());
            electionResult.setParty(party.trim());
            electionResult.setVotes(votes);
            electionResult.setTotalVotes(totalVotes);
            electionResult.setStatus(status.isBlank() ? "reported" : status.trim());

            electionResult = electionResultRepository.save(electionResult);
            uploadedElectionResults.add(electionResult);
        }
    }

    private void parseFraudReportsSheet(Sheet sheet, List<FraudReport> uploadedFraudReports, List<String> errors) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String title = getCellValueAsString(row.getCell(0));
            String category = getCellValueAsString(row.getCell(1));
            String location = getCellValueAsString(row.getCell(2));
            String status = getCellValueAsString(row.getCell(3));
            String description = getCellValueAsString(row.getCell(4));

            if (title.isBlank() || category.isBlank() || location.isBlank() || description.isBlank()) {
                errors.add("FraudReports sheet row " + (rowIndex + 1) + " is missing required values.");
                continue;
            }

            FraudReport fraudReport = fraudReportRepository
                    .findByTitleIgnoreCaseAndLocationIgnoreCase(title, location)
                    .orElseGet(FraudReport::new);

            fraudReport.setTitle(title.trim());
            fraudReport.setCategory(category.trim());
            fraudReport.setLocation(location.trim());
            fraudReport.setStatus(status.isBlank() ? "submitted" : status.trim());
            fraudReport.setDescription(description.trim());
            fraudReport.setCreatedBy("Data Analyst");
            fraudReport.setCreatedById("system");

            fraudReport = fraudReportRepository.save(fraudReport);
            uploadedFraudReports.add(fraudReport);
        }
    }

    private void parseRegionalStatisticsSheet(Sheet sheet, List<RegionalStatistic> uploadedRegionalStatistics, List<String> errors) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String region = getCellValueAsString(row.getCell(0));
            Integer totalVotes = getCellValueAsInteger(row.getCell(1));
            Integer anomalyCount = getCellValueAsInteger(row.getCell(2));

            if (region.isBlank() || totalVotes == null) {
                errors.add("RegionalStatistics sheet row " + (rowIndex + 1) + " is missing required values.");
                continue;
            }

            RegionalStatistic statistic = regionalStatisticRepository
                    .findByRegionIgnoreCase(region)
                    .orElseGet(RegionalStatistic::new);

            statistic.setRegion(region.trim());
            statistic.setTotalVotes(totalVotes);
            statistic.setAnomalyCount(anomalyCount == null ? 0 : anomalyCount);
            statistic.setUpdatedAt(Instant.now());

            statistic = regionalStatisticRepository.save(statistic);
            uploadedRegionalStatistics.add(statistic);
        }
    }

    private void parseTurnoutDataSheet(Sheet sheet, List<TurnoutData> uploadedTurnout, List<String> errors) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String region = getCellValueAsString(row.getCell(0));
            Integer registeredVoters = getCellValueAsInteger(row.getCell(1));
            Integer votesCast = getCellValueAsInteger(row.getCell(2));
            Double turnoutPercentage = getCellValueAsDouble(row.getCell(3));

            if (region.isBlank() || registeredVoters == null || votesCast == null) {
                errors.add("TurnoutData sheet row " + (rowIndex + 1) + " is missing required values.");
                continue;
            }

            TurnoutData turnoutData = turnoutDataRepository
                    .findByRegionIgnoreCase(region)
                    .orElseGet(TurnoutData::new);

            turnoutData.setRegion(region.trim());
            turnoutData.setRegisteredVoters(registeredVoters);
            turnoutData.setVotesCast(votesCast);
            turnoutData.setTurnoutPercentage(turnoutPercentage == null ? 0.0 : turnoutPercentage);
            turnoutData.setUpdatedAt(Instant.now());

            turnoutData = turnoutDataRepository.save(turnoutData);
            uploadedTurnout.add(turnoutData);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    @Transactional
    public ApiResponse<ElectionResult> updateElectionResult(String resultId, ElectionResultRequest request) {
        Optional<ElectionResult> found = electionResultRepository.findById(resultId);
        if (found.isEmpty()) {
            return ApiResponse.failure("Election result not found.");
        }

        ElectionResult result = found.get();
        result.setConstituency(request.constituency().trim());
        result.setBoothName(request.boothName().trim());
        result.setWinner(request.winner().trim());
        result.setParty(request.party().trim());
        result.setVotes(request.votes());
        result.setTotalVotes(request.totalVotes());
        result.setStatus(request.status().trim());

        result = electionResultRepository.save(result);
        return ApiResponse.success("Election result updated.", result);
    }

    @Transactional
    public ApiResponse<Void> deleteElectionResult(String resultId) {
        if (!electionResultRepository.existsById(resultId)) {
            return ApiResponse.failure("Election result not found.");
        }
        electionResultRepository.deleteById(resultId);
        return ApiResponse.success("Election result deleted.");
    }

    public ApiResponse<DashboardStats> getDashboardStats() {
        DashboardStats stats = new DashboardStats(
                (int) userRepository.count(),
                (int) incidentRepository.count(),
                (int) fraudReportRepository.count(),
                (int) analystReportRepository.count(),
                (int) electionResultRepository.count()
        );
        return ApiResponse.success("Dashboard stats fetched.", stats);
    }

    private boolean isLastAdmin(User user) {
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            return false;
        }
        long adminCount = userRepository.countByRoleIgnoreCase("admin");
        return adminCount <= 1;
    }

    private User sanitizeUser(User source) {
        User safe = new User();
        safe.setId(source.getId());
        safe.setName(source.getName());
        safe.setEmail(source.getEmail());
        safe.setRole(source.getRole());
        return safe;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private boolean canMutateOwnRecord(User actor, String ownerId) {
        if (actor == null) {
            return false;
        }

        if ("admin".equalsIgnoreCase(actor.getRole())) {
            return true;
        }

        return String.valueOf(actor.getId()).equals(ownerId);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
