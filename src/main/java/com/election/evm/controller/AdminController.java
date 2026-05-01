package com.election.evm.controller;

import com.election.evm.dto.AnalystReportRequest;
import com.election.evm.dto.ApiResponse;
import com.election.evm.dto.DashboardStats;
import com.election.evm.dto.FraudReportRequest;
import com.election.evm.dto.IncidentRequest;
import com.election.evm.dto.UserRequest;
import com.election.evm.entity.AnalystReport;
import com.election.evm.entity.FraudReport;
import com.election.evm.entity.Incident;
import com.election.evm.entity.User;
import com.election.evm.service.EvmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin Controller
 * Handles user management, incident reports, fraud reports, and analyst reports
 * Requires ADMIN role for most operations
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminController {
    private final EvmService service;

    public AdminController(EvmService service) {
        this.service = service;
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Get all users - ADMIN only
     * @return List of all users
     */
    @GetMapping("/users")
    public ApiResponse<List<User>> getUsers() {
        return service.getUsers();
    }

    /**
     * Create a new user - ADMIN only
     * @param request - UserRequest with user details
     * @return Created user
     */
    @PostMapping("/users")
    public ApiResponse<User> createUser(@Valid @RequestBody UserRequest request) {
        return service.createUser(request);
    }

    /**
     * Update user by ID - ADMIN only
     * @param userId - User ID to update
     * @param request - Updated user details
     * @return Updated user
     */
    @PutMapping("/users/{id}")
    public ApiResponse<User> updateUser(@PathVariable("id") Long userId, @Valid @RequestBody UserRequest request) {
        return service.updateUser(userId, request);
    }

    /**
     * Delete user by ID - ADMIN only
     * @param userId - User ID to delete
     * @return Success message
     */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable("id") Long userId) {
        return service.deleteUser(userId);
    }

    // ==================== INCIDENT MANAGEMENT ====================

    /**
     * Get all incidents
     * @return List of all incidents
     */
    @GetMapping("/incidents")
    public ApiResponse<List<Incident>> getIncidents() {
        return service.getIncidents();
    }

    /**
     * Create new incident
     * @param request - IncidentRequest with incident details
     * @return Created incident
     */
    @PostMapping("/incidents")
    public ApiResponse<Incident> createIncident(@Valid @RequestBody IncidentRequest request) {
        return service.createIncident(request);
    }

    /**
     * Update incident by ID
     * @param incidentId - Incident ID to update
     * @param request - Updated incident details
     * @return Updated incident
     */
    @PutMapping("/incidents/{id}")
    public ApiResponse<Incident> updateIncident(@PathVariable("id") String incidentId, @Valid @RequestBody IncidentRequest request) {
        return service.updateIncident(incidentId, request);
    }

    /**
     * Delete incident by ID
     * @param incidentId - Incident ID to delete
     * @return Success message
     */
    @DeleteMapping("/incidents/{id}")
    public ApiResponse<Void> deleteIncident(@PathVariable("id") String incidentId) {
        return service.deleteIncident(incidentId);
    }

    // ==================== FRAUD REPORT MANAGEMENT ====================

    /**
     * Get all fraud reports
     * @return List of all fraud reports
     */
    @GetMapping("/fraud-reports")
    public ApiResponse<List<FraudReport>> getFraudReports() {
        return service.getFraudReports();
    }

    /**
     * Create new fraud report
     * @param request - FraudReportRequest with report details
     * @return Created fraud report
     */
    @PostMapping("/fraud-reports")
    public ApiResponse<FraudReport> createFraudReport(@Valid @RequestBody FraudReportRequest request) {
        return service.createFraudReport(request);
    }

    /**
     * Update fraud report by ID
     * @param reportId - Report ID to update
     * @param request - Updated report details
     * @return Updated fraud report
     */
    @PutMapping("/fraud-reports/{id}")
    public ApiResponse<FraudReport> updateFraudReport(@PathVariable("id") String reportId, @Valid @RequestBody FraudReportRequest request) {
        return service.updateFraudReport(reportId, request);
    }

    /**
     * Delete fraud report by ID
     * @param reportId - Report ID to delete
     * @return Success message
     */
    @DeleteMapping("/fraud-reports/{id}")
    public ApiResponse<Void> deleteFraudReport(@PathVariable("id") String reportId) {
        return service.deleteFraudReport(reportId);
    }

    // ==================== ANALYST REPORT MANAGEMENT ====================

    /**
     * Get all analyst reports
     * @return List of all analyst reports
     */
    @GetMapping("/analyst-reports")
    public ApiResponse<List<AnalystReport>> getAnalystReports() {
        return service.getAnalystReports();
    }

    /**
     * Create new analyst report
     * @param request - AnalystReportRequest with report details
     * @return Created analyst report
     */
    @PostMapping("/analyst-reports")
    public ApiResponse<AnalystReport> createAnalystReport(@Valid @RequestBody AnalystReportRequest request) {
        return service.createAnalystReport(request);
    }

    /**
     * Update analyst report by ID
     * @param reportId - Report ID to update
     * @param request - Updated report details
     * @return Updated analyst report
     */
    @PutMapping("/analyst-reports/{id}")
    public ApiResponse<AnalystReport> updateAnalystReport(@PathVariable("id") String reportId, @Valid @RequestBody AnalystReportRequest request) {
        return service.updateAnalystReport(reportId, request);
    }

    /**
     * Delete analyst report by ID
     * @param reportId - Report ID to delete
     * @return Success message
     */
    @DeleteMapping("/analyst-reports/{id}")
    public ApiResponse<Void> deleteAnalystReport(@PathVariable("id") String reportId) {
        return service.deleteAnalystReport(reportId);
    }

    // ==================== DASHBOARD ====================

    /**
     * Get dashboard statistics - ADMIN only
     * @return DashboardStats with counts of all entities
     */
    @GetMapping("/dashboard/stats")
    public ApiResponse<DashboardStats> getDashboardStats() {
        return service.getDashboardStats();
    }
}
