package com.example.contract.service;

import com.example.contract.exception.DocumentParsingException;
import com.example.contract.util.SampleDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DocumentParserService} verifying PDF and DOCX parsing,
 * as well as handling corrupted, empty, or unsupported documents.
 */
class DocumentParserServiceTest {

    private DocumentParserService documentParserService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        documentParserService = new DocumentParserService();
    }

    @Test
    @DisplayName("Should extract text successfully from a valid PDF document")
    void testExtractTextFromValidPdf() throws IOException {
        File pdfFile = tempDir.resolve("valid.pdf").toFile();
        SampleDataGenerator.generatePdf(pdfFile);

        String extracted = documentParserService.extractText(pdfFile.toPath(), "PDF");

        assertNotNull(extracted);
        assertFalse(extracted.isBlank());
        assertTrue(extracted.contains("Rahul Sharma"));
        assertTrue(extracted.contains("Rs. 50,000 per month"));
        assertTrue(extracted.contains("30 days"));
    }

    @Test
    @DisplayName("Should extract text successfully from a valid DOCX document including tables")
    void testExtractTextFromValidDocx() throws IOException {
        File docxFile = tempDir.resolve("valid.docx").toFile();
        SampleDataGenerator.generateDocx(docxFile);

        String extracted = documentParserService.extractText(docxFile.toPath(), "DOCX");

        assertNotNull(extracted);
        assertFalse(extracted.isBlank());
        assertTrue(extracted.contains("Rahul Sharma"));
        assertTrue(extracted.contains("Rs. 50,000 per month"));
        assertTrue(extracted.contains("Pune"));
    }

    @Test
    @DisplayName("Should throw DocumentParsingException when parsing a corrupted PDF")
    void testExtractTextFromCorruptedPdf() throws IOException {
        File corruptedPdf = tempDir.resolve("corrupted.pdf").toFile();
        SampleDataGenerator.generateCorrupted(corruptedPdf);

        assertThrows(DocumentParsingException.class, () ->
                documentParserService.extractText(corruptedPdf.toPath(), "PDF"));
    }

    @Test
    @DisplayName("Should throw DocumentParsingException when document is completely empty")
    void testExtractTextFromEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.pdf").toFile();
        Files.createFile(emptyFile.toPath());

        assertThrows(DocumentParsingException.class, () ->
                documentParserService.extractText(emptyFile.toPath(), "PDF"));
    }

    @Test
    @DisplayName("Should throw DocumentParsingException when file does not exist")
    void testExtractTextFromNonExistentFile() {
        Path nonExistent = tempDir.resolve("does_not_exist.pdf");

        assertThrows(DocumentParsingException.class, () ->
                documentParserService.extractText(nonExistent, "PDF"));
    }
}
