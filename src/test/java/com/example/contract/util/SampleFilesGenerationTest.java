package com.example.contract.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that generates and validates the assignment sample files in the samples/ folder.
 */
class SampleFilesGenerationTest {

    @Test
    @DisplayName("Should generate sample PDF, DOCX, TXT, and corrupted files in samples/ folder")
    void testGenerateSamples() throws IOException {
        Path samplesDir = Paths.get("samples");
        SampleDataGenerator.generateAllSamples(samplesDir.toString());

        File pdfFile = samplesDir.resolve("sample_employment_agreement.pdf").toFile();
        File docxFile = samplesDir.resolve("sample_employment_agreement.docx").toFile();
        File txtFile = samplesDir.resolve("sample_unsupported.txt").toFile();
        File corruptedFile = samplesDir.resolve("sample_corrupted.pdf").toFile();

        assertTrue(pdfFile.exists() && pdfFile.length() > 0, "PDF sample should exist and not be empty");
        assertTrue(docxFile.exists() && docxFile.length() > 0, "DOCX sample should exist and not be empty");
        assertTrue(txtFile.exists() && txtFile.length() > 0, "TXT sample should exist and not be empty");
        assertTrue(corruptedFile.exists() && corruptedFile.length() > 0, "Corrupted sample should exist");
    }
}
