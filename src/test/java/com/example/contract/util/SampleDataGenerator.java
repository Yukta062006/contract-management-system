package com.example.contract.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility for generating sample PDF, DOCX, TXT, and corrupted files
 * to satisfy assignment test cases and sample document requirements.
 */
public class SampleDataGenerator {

    public static final String SAMPLE_TEXT = """
            EMPLOYMENT AGREEMENT

            This Employment Agreement is entered into by and between Seventh Ray Consulting and Technology Private Limited and the Employee.

            1. Employee Name: Rahul Sharma
            2. Role: Software Engineer
            3. Joining Date: 01 January 2026
            4. Working Location: Pune
            5. Salary: Rs. 50,000 per month
            6. Probation: 3 months
            7. Notice Period: 30 days
            8. Leave: 18 days of paid annual leave per calendar year
            9. Confidentiality: The Employee agrees not to disclose proprietary software, client records, or trade secrets during or after employment.
            10. Termination: Either party may terminate employment by giving written notice as specified in the notice period clause.
            """;

    public static void generateAllSamples(String targetDir) throws IOException {
        Path dir = Paths.get(targetDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        generatePdf(dir.resolve("sample_employment_agreement.pdf").toFile());
        generateDocx(dir.resolve("sample_employment_agreement.docx").toFile());
        generateTxt(dir.resolve("sample_unsupported.txt").toFile());
        generateCorrupted(dir.resolve("sample_corrupted.pdf").toFile());
    }

    public static void generatePdf(File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.beginText();
                contentStream.setLeading(16f);
                contentStream.newLineAtOffset(50, 750);

                String[] lines = SAMPLE_TEXT.split("\n");
                for (String line : lines) {
                    contentStream.showText(line.trim());
                    contentStream.newLine();
                }

                contentStream.endText();
            }

            document.save(file);
        }
    }

    public static void generateDocx(File file) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(file)) {

            // Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("EMPLOYMENT AGREEMENT");
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            // Introductory Paragraph
            XWPFParagraph introPara = document.createParagraph();
            XWPFRun introRun = introPara.createRun();
            introRun.setText("This Employment Agreement is entered into by and between Seventh Ray Consulting and Technology Private Limited and the Employee.");

            // Table of Key Terms
            XWPFTable table = document.createTable();

            addTableRow(table, "Employee Name", "Rahul Sharma");
            addTableRow(table, "Role", "Software Engineer");
            addTableRow(table, "Joining Date", "01 January 2026");
            addTableRow(table, "Working Location", "Pune");
            addTableRow(table, "Salary", "Rs. 50,000 per month");
            addTableRow(table, "Probation", "3 months");
            addTableRow(table, "Notice Period", "30 days");
            addTableRow(table, "Leave", "18 days of paid annual leave per calendar year");
            addTableRow(table, "Termination", "Either party may terminate employment by giving written notice as specified.");

            // Remove empty default first row if present
            if (table.getRows().size() > 9) {
                table.removeRow(0);
            }

            // Confidentiality clause paragraph
            XWPFParagraph confPara = document.createParagraph();
            XWPFRun confRun = confPara.createRun();
            confRun.setText("Confidentiality: The Employee agrees not to disclose proprietary software, client records, or trade secrets during or after employment.");

            document.write(out);
        }
    }

    private static void addTableRow(XWPFTable table, String key, String value) {
        XWPFTableRow row = table.createRow();
        row.getCell(0).setText(key);
        if (row.getTableCells().size() > 1) {
            row.getCell(1).setText(value);
        } else {
            row.addNewTableCell().setText(value);
        }
    }

    public static void generateTxt(File file) throws IOException {
        Files.writeString(file.toPath(), SAMPLE_TEXT, StandardCharsets.UTF_8);
    }

    public static void generateCorrupted(File file) throws IOException {
        byte[] junk = "%PDF-1.4 CORRUPTED DATA NOT A REAL PDF".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), junk);
    }
}
