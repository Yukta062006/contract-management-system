package com.example.contract.service;

import com.example.contract.dto.QuestionResponse;
import com.example.contract.util.SampleDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link KeywordQuestionAnswerService} verifying Q&A functionality,
 * keyword matching, synonym recognition, scoring, and not-found fallbacks.
 */
class KeywordQuestionAnswerServiceTest {

    private KeywordQuestionAnswerService qaService;
    private String contractText;

    @BeforeEach
    void setUp() {
        qaService = new KeywordQuestionAnswerService();
        contractText = SampleDataGenerator.SAMPLE_TEXT;
    }

    @Test
    @DisplayName("Should correctly answer salary question")
    void testSalaryQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "What is the salary?");

        assertNotNull(response);
        assertEquals(1L, response.getContractId());
        assertTrue(response.isFound());
        assertEquals("Rs. 50,000 per month", response.getAnswer());
    }

    @Test
    @DisplayName("Should correctly answer salary question with synonyms (e.g. compensation, pay)")
    void testSalarySynonyms() {
        QuestionResponse payResponse = qaService.answerQuestion(1L, contractText, "What is the monthly pay?");
        assertTrue(payResponse.isFound());
        assertEquals("Rs. 50,000 per month", payResponse.getAnswer());

        QuestionResponse compResponse = qaService.answerQuestion(1L, contractText, "What is the compensation?");
        assertTrue(compResponse.isFound());
        assertEquals("Rs. 50,000 per month", compResponse.getAnswer());
    }

    @Test
    @DisplayName("Should correctly answer notice period question")
    void testNoticePeriodQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "What is the notice period?");

        assertNotNull(response);
        assertTrue(response.isFound());
        assertEquals("30 days", response.getAnswer());
    }

    @Test
    @DisplayName("Should correctly answer joining date question")
    void testJoiningDateQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "When is the joining date?");

        assertNotNull(response);
        assertTrue(response.isFound());
        assertEquals("01 January 2026", response.getAnswer());
    }

    @Test
    @DisplayName("Should correctly answer joining date with synonyms (e.g. start date)")
    void testJoiningDateSynonym() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "What is the start date?");

        assertNotNull(response);
        assertTrue(response.isFound());
        assertEquals("01 January 2026", response.getAnswer());
    }

    @Test
    @DisplayName("Should correctly answer work location question")
    void testWorkLocationQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "Where is the work location?");

        assertNotNull(response);
        assertTrue(response.isFound());
        assertEquals("Pune", response.getAnswer());
    }

    @Test
    @DisplayName("Should correctly answer employee name question")
    void testEmployeeNameQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "Who is the employee?");

        assertNotNull(response);
        assertTrue(response.isFound());
        assertEquals("Rahul Sharma", response.getAnswer());
    }

    @Test
    @DisplayName("Should return found=false for unknown question (bonus policy)")
    void testUnknownQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "What is the bonus policy?");

        assertNotNull(response);
        assertFalse(response.isFound());
        assertEquals("Information not found in the contract.", response.getAnswer());
    }

    @Test
    @DisplayName("Should return found=false when contract text is empty or blank")
    void testEmptyContractText() {
        QuestionResponse response = qaService.answerQuestion(1L, "   ", "What is the salary?");

        assertNotNull(response);
        assertFalse(response.isFound());
        assertEquals("Information not found in the contract.", response.getAnswer());
    }

    @Test
    @DisplayName("Should return found=false when question is blank")
    void testBlankQuestion() {
        QuestionResponse response = qaService.answerQuestion(1L, contractText, "   ");

        assertNotNull(response);
        assertFalse(response.isFound());
    }
}
