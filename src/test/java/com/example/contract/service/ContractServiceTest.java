package com.example.contract.service;

import com.example.contract.dto.ContractResponse;
import com.example.contract.dto.ContractSummaryResponse;
import com.example.contract.dto.QuestionResponse;
import com.example.contract.entity.Contract;
import com.example.contract.enums.ContractStatus;
import com.example.contract.exception.ContractNotFoundException;
import com.example.contract.exception.DocumentParsingException;
import com.example.contract.exception.InvalidFileException;
import com.example.contract.exception.InvalidStatusTransitionException;
import com.example.contract.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContractService} using Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentParserService documentParserService;

    @Mock
    private QuestionAnswerService questionAnswerService;

    private ContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
                contractRepository,
                fileStorageService,
                documentParserService,
                questionAnswerService
        );
    }

    @Test
    @DisplayName("Should successfully upload and process a valid PDF contract without description")
    void testUploadContractSuccessWithoutDescription() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "agreement.pdf", "application/pdf", "dummy pdf content".getBytes()
        );
        Path mockPath = Paths.get("uploads/agreement.pdf");

        when(fileStorageService.storeFile(file)).thenReturn(mockPath);
        when(documentParserService.extractText(mockPath, "PDF")).thenReturn("Extracted agreement text");

        Contract savedEntity = new Contract("agreement.pdf", mockPath.toString(), "PDF", ContractStatus.DRAFT, "Extracted agreement text", null);
        savedEntity.setId(10L);
        when(contractRepository.save(any(Contract.class))).thenReturn(savedEntity);

        ContractResponse response = contractService.uploadContract(file);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("agreement.pdf", response.getFileName());
        assertEquals("PDF", response.getFileType());
        assertEquals(ContractStatus.DRAFT, response.getStatus());
        assertEquals("Extracted agreement text", response.getExtractedText());
        assertNull(response.getContractDescription());

        verify(fileStorageService).storeFile(file);
        verify(documentParserService).extractText(mockPath, "PDF");
        verify(contractRepository).save(any(Contract.class));
        verify(fileStorageService, never()).deleteFile(any(Path.class));
    }

    @Test
    @DisplayName("Should successfully upload contract with description")
    void testUploadContractSuccessWithDescription() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "consulting_agreement.pdf", "application/pdf", "dummy content".getBytes()
        );
        Path mockPath = Paths.get("uploads/consulting_agreement.pdf");
        String description = "Consulting agreement for Q1 delivery with Seventh Ray";

        when(fileStorageService.storeFile(file)).thenReturn(mockPath);
        when(documentParserService.extractText(mockPath, "PDF")).thenReturn("Extracted consulting terms");

        Contract savedEntity = new Contract("consulting_agreement.pdf", mockPath.toString(), "PDF", ContractStatus.DRAFT, "Extracted consulting terms", description);
        savedEntity.setId(15L);
        when(contractRepository.save(any(Contract.class))).thenReturn(savedEntity);

        ContractResponse response = contractService.uploadContract(file, description);

        assertNotNull(response);
        assertEquals(15L, response.getId());
        assertEquals("consulting_agreement.pdf", response.getFileName());
        assertEquals(description, response.getContractDescription());
    }

    @Test
    @DisplayName("Should reject unsupported file formats (e.g. .txt)")
    void testUploadContractUnsupportedFormat() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "document.txt", "text/plain", "dummy text".getBytes()
        );

        assertThrows(InvalidFileException.class, () -> contractService.uploadContract(txtFile, "Some description"));

        verify(fileStorageService, never()).storeFile(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should clean up stored file and throw DocumentParsingException if parsing fails")
    void testUploadContractParsingFailureAndCleanup() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.pdf", "application/pdf", "broken content".getBytes()
        );
        Path mockPath = Paths.get("uploads/broken.pdf");

        when(fileStorageService.storeFile(file)).thenReturn(mockPath);
        when(documentParserService.extractText(mockPath, "PDF"))
                .thenThrow(new DocumentParsingException("Corrupted PDF file"));

        assertThrows(DocumentParsingException.class, () -> contractService.uploadContract(file, "Broken file"));

        verify(fileStorageService).deleteFile(mockPath);
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return contract details including description when contract exists")
    void testGetContractByIdSuccess() {
        Contract contract = new Contract("sample.pdf", "uploads/sample.pdf", "PDF", ContractStatus.DRAFT, "Sample content", "Employment Contract for Developer");
        contract.setId(1L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        ContractResponse response = contractService.getContractById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("sample.pdf", response.getFileName());
        assertEquals("Sample content", response.getExtractedText());
        assertEquals("Employment Contract for Developer", response.getContractDescription());
    }

    @Test
    @DisplayName("Should throw ContractNotFoundException when contract does not exist")
    void testGetContractByIdNotFound() {
        when(contractRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ContractNotFoundException.class, () -> contractService.getContractById(999L));
    }

    @Test
    @DisplayName("Should return all contracts in summary form with descriptions")
    void testGetAllContracts() {
        Contract c1 = new Contract("c1.pdf", "p1", "PDF", ContractStatus.DRAFT, "t1", "Vendor Contract");
        c1.setId(1L);
        Contract c2 = new Contract("c2.docx", "p2", "DOCX", ContractStatus.REVIEW, "t2", "NDA Agreement");
        c2.setId(2L);

        when(contractRepository.findAll()).thenReturn(List.of(c1, c2));

        List<ContractSummaryResponse> summaries = contractService.getAllContracts();

        assertEquals(2, summaries.size());
        assertEquals("c1.pdf", summaries.get(0).getFileName());
        assertEquals("Vendor Contract", summaries.get(0).getContractDescription());
        assertEquals("c2.docx", summaries.get(1).getFileName());
        assertEquals("NDA Agreement", summaries.get(1).getContractDescription());
    }

    @Test
    @DisplayName("Should successfully update contract status from DRAFT to REVIEW")
    void testUpdateContractStatusSuccess() {
        Contract contract = new Contract("c.pdf", "p", "PDF", ContractStatus.DRAFT, "t", "Test description");
        contract.setId(1L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(contract)).thenReturn(contract);

        ContractResponse response = contractService.updateContractStatus(1L, ContractStatus.REVIEW);

        assertNotNull(response);
        assertEquals(ContractStatus.REVIEW, response.getStatus());
        verify(contractRepository).save(contract);
    }

    @Test
    @DisplayName("Should reject invalid status transition (e.g. DRAFT to APPROVED)")
    void testUpdateContractStatusInvalidTransition() {
        Contract contract = new Contract("c.pdf", "p", "PDF", ContractStatus.DRAFT, "t");
        contract.setId(1L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThrows(InvalidStatusTransitionException.class, () ->
                contractService.updateContractStatus(1L, ContractStatus.APPROVED));

        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delegate question to QuestionAnswerService")
    void testAskQuestion() {
        Contract contract = new Contract("c.pdf", "p", "PDF", ContractStatus.DRAFT, "Notice Period: 30 days");
        contract.setId(1L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(questionAnswerService.answerQuestion(1L, contract.getExtractedText(), "What is the notice period?"))
                .thenReturn(new QuestionResponse(1L, "What is the notice period?", "30 days", true));

        QuestionResponse response = contractService.askQuestion(1L, "What is the notice period?");

        assertNotNull(response);
        assertTrue(response.isFound());
        assertEquals("30 days", response.getAnswer());
    }
}
