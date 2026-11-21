package com.example.contract.controller;

import com.example.contract.dto.ContractResponse;
import com.example.contract.dto.ContractSummaryResponse;
import com.example.contract.dto.QuestionResponse;
import com.example.contract.enums.ContractStatus;
import com.example.contract.exception.ContractNotFoundException;
import com.example.contract.exception.DocumentParsingException;
import com.example.contract.exception.InvalidFileException;
import com.example.contract.exception.InvalidStatusTransitionException;
import com.example.contract.service.ContractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller layer integration tests testing all 5 REST endpoints,
 * validation, serialization, description processing, and error response handling.
 */
@WebMvcTest(ContractController.class)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContractService contractService;

    // --- API 1: Upload Contract ---

    @Test
    @DisplayName("POST /api/contracts should return 201 on valid file upload without description")
    void testUploadContractSuccessWithoutDescription() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "agreement.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes()
        );

        ContractResponse response = new ContractResponse(
                1L, "agreement.pdf", "PDF", ContractStatus.DRAFT, "Sample extracted text", null, LocalDateTime.now()
        );

        when(contractService.uploadContract(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/contracts").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fileName").value("agreement.pdf"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.extractedText").value("Sample extracted text"));
    }

    @Test
    @DisplayName("POST /api/contracts should return 201 and associate description when provided")
    void testUploadContractSuccessWithDescription() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "agreement.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes()
        );
        String description = "Employment agreement for Senior Developer with 30-day notice";

        ContractResponse response = new ContractResponse(
                2L, "agreement.pdf", "PDF", ContractStatus.DRAFT, "Sample text", description, LocalDateTime.now()
        );

        when(contractService.uploadContract(any(), eq(description))).thenReturn(response);

        mockMvc.perform(multipart("/api/contracts")
                        .file(file)
                        .param("description", description))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.fileName").value("agreement.pdf"))
                .andExpect(jsonPath("$.contractDescription").value(description));
    }

    @Test
    @DisplayName("POST /api/contracts should return 400 for unsupported file type")
    void testUploadContractInvalidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "plain text".getBytes()
        );

        when(contractService.uploadContract(any(), any()))
                .thenThrow(new InvalidFileException("Unsupported file format"));

        mockMvc.perform(multipart("/api/contracts").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported file format"));
    }

    @Test
    @DisplayName("POST /api/contracts should return 422 when document parsing fails")
    void testUploadContractParsingFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.pdf", "application/pdf", "corrupted".getBytes()
        );

        when(contractService.uploadContract(any(), any()))
                .thenThrow(new DocumentParsingException("Corrupted document"));

        mockMvc.perform(multipart("/api/contracts").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Corrupted document"));
    }

    // --- API 2: Get All Contracts ---

    @Test
    @DisplayName("GET /api/contracts should return 200 and list of contract summaries with descriptions")
    void testGetAllContractsSuccess() throws Exception {
        ContractSummaryResponse summary = new ContractSummaryResponse(
                1L, "agreement.pdf", "PDF", ContractStatus.DRAFT, "Key employment contract", LocalDateTime.now()
        );

        when(contractService.getAllContracts()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].fileName").value("agreement.pdf"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].contractDescription").value("Key employment contract"));
    }

    // --- API 3: Get Contract by ID ---

    @Test
    @DisplayName("GET /api/contracts/{id} should return 200 with full contract info and description")
    void testGetContractByIdSuccess() throws Exception {
        ContractResponse response = new ContractResponse(
                1L, "agreement.pdf", "PDF", ContractStatus.DRAFT, "Full contract text", "Signed agreement", LocalDateTime.now()
        );

        when(contractService.getContractById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/contracts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fileName").value("agreement.pdf"))
                .andExpect(jsonPath("$.extractedText").value("Full contract text"))
                .andExpect(jsonPath("$.contractDescription").value("Signed agreement"));
    }

    @Test
    @DisplayName("GET /api/contracts/{id} should return 404 when contract not found")
    void testGetContractByIdNotFound() throws Exception {
        when(contractService.getContractById(999L))
                .thenThrow(new ContractNotFoundException(999L));

        mockMvc.perform(get("/api/contracts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Contract with ID 999 was not found."));
    }

    // --- API 4: Update Contract Status ---

    @Test
    @DisplayName("PUT /api/contracts/{id}/status should return 200 on valid status transition")
    void testUpdateContractStatusSuccess() throws Exception {
        ContractResponse response = new ContractResponse(
                1L, "agreement.pdf", "PDF", ContractStatus.REVIEW, "Full contract text", "Reviewed doc", LocalDateTime.now()
        );

        when(contractService.updateContractStatus(eq(1L), eq(ContractStatus.REVIEW))).thenReturn(response);

        mockMvc.perform(put("/api/contracts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW"));
    }

    @Test
    @DisplayName("PUT /api/contracts/{id}/status should return 409 on invalid transition")
    void testUpdateContractStatusInvalidTransition() throws Exception {
        when(contractService.updateContractStatus(eq(1L), eq(ContractStatus.APPROVED)))
                .thenThrow(new InvalidStatusTransitionException("Invalid status transition from DRAFT to APPROVED."));

        mockMvc.perform(put("/api/contracts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"APPROVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Invalid status transition from DRAFT to APPROVED."));
    }

    // --- API 5: Ask Question ---

    @Test
    @DisplayName("POST /api/contracts/{id}/questions should return 200 and answer")
    void testAskQuestionSuccess() throws Exception {
        QuestionResponse response = new QuestionResponse(
                1L, "What is the notice period?", "30 days", true
        );

        when(contractService.askQuestion(eq(1L), eq("What is the notice period?"))).thenReturn(response);

        mockMvc.perform(post("/api/contracts/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"What is the notice period?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value(1L))
                .andExpect(jsonPath("$.question").value("What is the notice period?"))
                .andExpect(jsonPath("$.answer").value("30 days"))
                .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    @DisplayName("POST /api/contracts/{id}/questions should return 400 for empty question")
    void testAskQuestionBlank() throws Exception {
        mockMvc.perform(post("/api/contracts/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
