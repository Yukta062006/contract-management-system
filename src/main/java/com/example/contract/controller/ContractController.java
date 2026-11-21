package com.example.contract.controller;

import com.example.contract.dto.ContractResponse;
import com.example.contract.dto.ContractSummaryResponse;
import com.example.contract.dto.QuestionRequest;
import com.example.contract.dto.QuestionResponse;
import com.example.contract.dto.StatusUpdateRequest;
import com.example.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller exposing endpoints for contract management:
 * uploading documents with descriptions, retrieving summaries, fetching detailed documents,
 * transitioning lifecycle status, and asking natural language questions.
 */
@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
@Tag(name = "Contract Management API", description = "Endpoints for managing contracts, lifecycle status, descriptions, and Q&A")
public class ContractController {

    private final ContractService contractService;

    /**
     * Constructor injection of ContractService.
     */
    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * API 1: Upload a new contract document (PDF or DOCX) with optional description.
     */
    @Operation(summary = "Upload a contract", description = "Uploads a PDF or DOCX contract with an optional description, extracts its text, and stores it with DRAFT status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contract uploaded and processed successfully",
                    content = @Content(schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or unsupported format"),
            @ApiResponse(responseCode = "413", description = "File exceeds maximum size limit (15MB)"),
            @ApiResponse(responseCode = "422", description = "Document parsing failure or empty document")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ContractResponse> uploadContract(
            @Parameter(description = "Contract document file (.pdf or .docx)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Meaningful contract description", required = false)
            @RequestParam(value = "description", required = false) String description) {
        ContractResponse response = contractService.uploadContract(file, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API 2: Get all contracts in summary form (including descriptions).
     */
    @Operation(summary = "Get all contracts", description = "Returns a summary list of all uploaded contracts with descriptions (without heavy extracted text).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contracts retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<ContractSummaryResponse>> getAllContracts() {
        List<ContractSummaryResponse> contracts = contractService.getAllContracts();
        return ResponseEntity.ok(contracts);
    }

    /**
     * API 3: Get contract by ID with full extracted text and description.
     */
    @Operation(summary = "Get contract by ID", description = "Retrieves complete contract details including full extracted text and contract description.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getContractById(
            @Parameter(description = "Contract unique ID", required = true)
            @PathVariable("id") Long id) {
        ContractResponse response = contractService.getContractById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * API 4: Update contract status following strict workflow: DRAFT -> REVIEW -> APPROVED.
     */
    @Operation(summary = "Update contract status", description = "Transitions contract status along the valid workflow: DRAFT -> REVIEW -> APPROVED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = ContractResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Contract not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ContractResponse> updateContractStatus(
            @Parameter(description = "Contract unique ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        ContractResponse response = contractService.updateContractStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * API 5: Ask questions about contract contents.
     */
    @Operation(summary = "Ask question about a contract", description = "Queries extracted text using keyword matching and synonyms to return precise answers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question processed successfully",
                    content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid question"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @PostMapping("/{id}/questions")
    public ResponseEntity<QuestionResponse> askQuestion(
            @Parameter(description = "Contract unique ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = contractService.askQuestion(id, request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
