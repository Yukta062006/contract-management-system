package com.example.contract.service;

import com.example.contract.dto.QuestionResponse;

/**
 * Service interface for querying and answering questions about contract documents.
 */
public interface QuestionAnswerService {

    /**
     * Searches extracted contract text and returns the most relevant answer.
     *
     * @param contractId    ID of the contract being queried
     * @param extractedText Full extracted text from the contract document
     * @param question      User's natural language question
     * @return {@link QuestionResponse} containing the answer and found flag
     */
    QuestionResponse answerQuestion(Long contractId, String extractedText, String question);
}
