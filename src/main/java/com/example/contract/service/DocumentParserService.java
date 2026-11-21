package com.example.contract.service;

import com.example.contract.exception.DocumentParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Service responsible for parsing documents (PDF and DOCX) and extracting text content.
 * Uses Apache PDFBox 3.x for PDF parsing and Apache POI 5.x for DOCX parsing.
 */
@Service
public class DocumentParserService {

    /**
     * Extracts text from a document located at the given path based on its file extension/type.
     *
     * @param filePath Path of the file on disk
     * @param fileType File type ("PDF" or "DOCX")
     * @return Normalized extracted text content
     * @throws DocumentParsingException if extraction fails, file is corrupted, or no text is found
     */
    public String extractText(Path filePath, String fileType) {
        if (filePath == null) {
            throw new DocumentParsingException("File path cannot be null.");
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new DocumentParsingException("File does not exist at path: " + filePath);
        }

        if (file.length() == 0) {
            throw new DocumentParsingException("The uploaded document is completely empty (0 bytes).");
        }

        String rawText;
        if ("PDF".equalsIgnoreCase(fileType)) {
            rawText = parsePdf(file);
        } else if ("DOCX".equalsIgnoreCase(fileType)) {
            rawText = parseDocx(file);
        } else {
            throw new DocumentParsingException("Unsupported file type for parsing: " + fileType);
        }

        String normalizedText = normalizeText(rawText);

        if (normalizedText.isBlank()) {
            throw new DocumentParsingException(
                    "The uploaded document contains no readable text. Scanned images or empty documents are not supported."
            );
        }

        return normalizedText;
    }

    /**
     * Parses a PDF file using Apache PDFBox 3.x Loader.
     */
    private String parsePdf(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new DocumentParsingException("Password-protected or encrypted PDFs are not supported.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (DocumentParsingException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new DocumentParsingException(
                    "Failed to parse PDF document. The file may be corrupted or invalid: " + ex.getMessage(),
                    ex
            );
        } catch (Exception ex) {
            throw new DocumentParsingException(
                    "Unexpected error while reading PDF document: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Parses a DOCX document using Apache POI XWPFDocument.
     * Extracts paragraphs and all table contents while preserving line breaks.
     */
    private String parseDocx(File file) {
        try (InputStream inputStream = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder contentBuilder = new StringBuilder();

            // Extract all paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    contentBuilder.append(text).append("\n");
                }
            }

            // Extract all table data (rows and cells)
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder rowBuilder = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isBlank()) {
                            if (!rowBuilder.isEmpty()) {
                                rowBuilder.append(" | ");
                            }
                            rowBuilder.append(cellText.trim());
                        }
                    }
                    if (!rowBuilder.isEmpty()) {
                        contentBuilder.append(rowBuilder).append("\n");
                    }
                }
            }

            return contentBuilder.toString();
        } catch (Exception ex) {
            throw new DocumentParsingException(
                    "Failed to parse DOCX document. The file may be corrupted or invalid: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Normalizes extracted text:
     * - Replaces Windows/Mac line endings with standard Unix line feed '\n'
     * - Trims trailing whitespace on lines
     * - Collapses more than two consecutive empty lines into a single blank line
     * - Strips outer whitespace
     */
    public String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n");

        // Remove extra blank lines
        normalized = normalized.replaceAll("\n{3,}", "\n\n");

        return normalized.trim();
    }
}
