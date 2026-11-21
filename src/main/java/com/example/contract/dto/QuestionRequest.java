package com.example.contract.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for asking a question about a contract's contents.
 */
public class QuestionRequest {

    @NotBlank(message = "Question cannot be blank")
    private String question;

    public QuestionRequest() {
    }

    public QuestionRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
