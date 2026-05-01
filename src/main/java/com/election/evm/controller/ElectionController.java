package com.election.evm.controller;

import com.election.evm.dto.ApiResponse;
import com.election.evm.dto.BulkUploadResult;
import com.election.evm.dto.ElectionResultRequest;
import com.election.evm.entity.ElectionResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Election Controller
 * Handles election result operations
 * Accessible to ADMIN and ANALYST roles
 */
@RestController
@RequestMapping("/api/election-results")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class ElectionController {
    private final EvmService service;

    public ElectionController(EvmService service) {
        this.service = service;
    }

    /**
     * Get all election results
     * Accessible to all authorized users (ADMIN, CITIZEN, OBSERVER, ANALYST)
     * @return List of all election results
     */
    @GetMapping
    public ApiResponse<List<ElectionResult>> getElectionResults() {
        return service.getElectionResults();
    }

    /**
     * Create new election result
     * Requires ADMIN or ANALYST role
     * @param request - ElectionResultRequest with result details
     * @return Created election result
     */
    @PostMapping
    public ApiResponse<ElectionResult> createElectionResult(@Valid @RequestBody ElectionResultRequest request) {
        return service.createElectionResult(request);
    }

    /**
     * Upload Excel file containing election results, turnout and regional data
     * Requires ANALYST role
     * @param file - Excel file to import
     * @return Bulk upload response with updated analytics
     */
    @PostMapping("/bulk-upload")
    public ApiResponse<BulkUploadResult> bulkUploadElectionData(@RequestParam("file") MultipartFile file) {
        return service.bulkUploadElectionData(file);
    }

    /**
     * Update election result by ID
     * Requires ADMIN or ANALYST role
     * @param resultId - Election result ID to update
     * @param request - Updated election result details
     * @return Updated election result
     */
    @PutMapping("/{id}")
    public ApiResponse<ElectionResult> updateElectionResult(
            @PathVariable("id") String resultId,
            @Valid @RequestBody ElectionResultRequest request) {
        return service.updateElectionResult(resultId, request);
    }

    /**
     * Delete election result by ID
     * Requires ADMIN or ANALYST role
     * @param resultId - Election result ID to delete
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteElectionResult(@PathVariable("id") String resultId) {
        return service.deleteElectionResult(resultId);
    }
}
