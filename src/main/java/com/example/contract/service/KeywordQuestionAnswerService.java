package com.example.contract.service;

import com.example.contract.dto.QuestionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword-based implementation of {@link QuestionAnswerService}.
 *
 * <p>Execution pipeline:
 * <ol>
 *   <li>Convert question to lowercase.</li>
 *   <li>Remove unnecessary punctuation and strip stop words.</li>
 *   <li>Extract meaningful keywords and expand them using a domain-specific synonyms dictionary.</li>
 *   <li>Segment contract text into lines and sentences.</li>
 *   <li>Score each line based on concept label matches, exact phrase matches, and keyword frequency.</li>
 *   <li>Extract the precise answer value (e.g., from label:value structures) or the best sentence.</li>
 *   <li>Return a not-found response if no line meets the confidence threshold.</li>
 * </ol>
 */
@Service
public class KeywordQuestionAnswerService implements QuestionAnswerService {

    private static final String NOT_FOUND_MESSAGE = "Information not found in the contract.";
    private static final int MIN_CONFIDENCE_SCORE = 3;

    // Common English stop words plus generic contract query words
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "what", "when", "where", "which", "who", "whom", "whose", "why", "how",
            "is", "are", "was", "were", "be", "been", "being",
            "do", "does", "did", "have", "has", "had", "having",
            "the", "a", "an", "and", "or", "but", "if", "because", "as", "until", "while",
            "of", "at", "by", "for", "with", "about", "against", "between", "into", "through",
            "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out",
            "on", "off", "over", "under", "again", "further", "then", "once",
            "here", "there", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such",
            "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "can", "will", "just",
            "should", "now", "tell", "me", "find", "give", "show", "please", "mentioned", "contract",
            "agreement", "document"
    ));

    // Synonym map mapping canonical concepts to related contract terms
    private static final Map<String, List<String>> SYNONYMS_MAP = new HashMap<>();

    static {
        SYNONYMS_MAP.put("salary", Arrays.asList(
                "salary", "pay", "compensation", "remuneration", "ctc", "stipend", "wage", "gross salary", "monthly salary"
        ));
        SYNONYMS_MAP.put("notice", Arrays.asList(
                "notice period", "notice", "resignation", "resignation notice"
        ));
        SYNONYMS_MAP.put("joining", Arrays.asList(
                "joining date", "start date", "commencement date", "date of joining", "joining"
        ));
        SYNONYMS_MAP.put("location", Arrays.asList(
                "work location", "working location", "location", "workplace", "office", "place of work", "job location"
        ));
        SYNONYMS_MAP.put("employee", Arrays.asList(
                "employee name", "candidate name", "employee", "name"
        ));
        SYNONYMS_MAP.put("probation", Arrays.asList(
                "probation", "probation period", "probationary period"
        ));
        SYNONYMS_MAP.put("termination", Arrays.asList(
                "termination", "termination clause", "dismissal", "termination of employment"
        ));
        SYNONYMS_MAP.put("leave", Arrays.asList(
                "leave", "leave policy", "leaves", "vacation", "annual leave", "sick leave", "paid time off"
        ));
        SYNONYMS_MAP.put("role", Arrays.asList(
                "designation", "job title", "role", "position"
        ));
    }

    @Override
    public QuestionResponse answerQuestion(Long contractId, String extractedText, String question) {
        if (question == null || question.isBlank()) {
            return new QuestionResponse(contractId, question, "Question cannot be empty.", false);
        }

        if (extractedText == null || extractedText.isBlank()) {
            return new QuestionResponse(contractId, question, NOT_FOUND_MESSAGE, false);
        }

        // 1 & 2. Preprocess question and extract keywords
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return new QuestionResponse(contractId, question, NOT_FOUND_MESSAGE, false);
        }

        // 3. Expand keywords with domain synonyms and multi-word phrases
        Set<String> searchTerms = expandWithSynonyms(question, keywords);

        // 4. Split contract text into lines and candidate sentences
        List<String> lines = splitIntoLines(extractedText);

        // 5. Score candidate lines
        String bestLine = null;
        int highestScore = 0;

        for (String line : lines) {
            int score = scoreLine(line, question, searchTerms);
            if (score > highestScore) {
                highestScore = score;
                bestLine = line;
            }
        }

        // 6. Validate confidence threshold
        if (highestScore < MIN_CONFIDENCE_SCORE || bestLine == null) {
            return new QuestionResponse(contractId, question, NOT_FOUND_MESSAGE, false);
        }

        // 7. Extract the exact value (from key: value pairs) or return the relevant clean line
        String extractedAnswer = formatAnswer(bestLine);

        return new QuestionResponse(contractId, question, extractedAnswer, true);
    }

    /**
     * Extracts non-stop-word keywords from a query string.
     */
    private List<String> extractKeywords(String question) {
        String cleaned = question.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = cleaned.split("\\s+");

        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank() && !STOP_WORDS.contains(token) && token.length() > 1) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    /**
     * Expands keywords using phrase detection and the synonym dictionary.
     */
    private Set<String> expandWithSynonyms(String rawQuestion, List<String> keywords) {
        Set<String> terms = new HashSet<>(keywords);
        String lowerQuestion = rawQuestion.toLowerCase();

        // Check each synonym category
        for (Map.Entry<String, List<String>> entry : SYNONYMS_MAP.entrySet()) {
            String concept = entry.getKey();
            List<String> synonyms = entry.getValue();

            boolean conceptMatches = false;

            // Check if any synonym is contained in the raw question
            for (String synonym : synonyms) {
                if (lowerQuestion.contains(synonym)) {
                    conceptMatches = true;
                    break;
                }
            }

            // Or if any extracted keyword matches the concept
            if (!conceptMatches) {
                for (String kw : keywords) {
                    if (synonyms.contains(kw) || kw.equals(concept)) {
                        conceptMatches = true;
                        break;
                    }
                }
            }

            if (conceptMatches) {
                terms.addAll(synonyms);
                terms.add(concept);
            }
        }

        return terms;
    }

    /**
     * Splits extracted text into individual non-empty lines.
     */
    private List<String> splitIntoLines(String text) {
        String[] rawLines = text.split("\n+");
        List<String> result = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Scores a document line against search terms and the original question.
     * Prefers direct key-value label matches like 'Salary:' or 'Notice Period:'.
     */
    private int scoreLine(String line, String rawQuestion, Set<String> searchTerms) {
        String lowerLine = line.toLowerCase();
        int score = 0;

        // Check if the line has a label structure: e.g. "Notice Period: 30 days"
        int colonIdx = line.indexOf(':');
        int dashIdx = line.indexOf(" - ");
        int separatorIdx = colonIdx != -1 ? colonIdx : dashIdx;

        String labelPart = separatorIdx != -1 ? lowerLine.substring(0, separatorIdx).trim() : "";

        // High priority: Check if the label before the colon matches one of our search terms
        if (!labelPart.isEmpty()) {
            for (String term : searchTerms) {
                if (labelPart.equals(term)) {
                    score += 15;
                } else if (labelPart.contains(term)) {
                    score += 10;
                }
            }
        }

        // Multi-word phrase matching in the line
        for (String term : searchTerms) {
            if (term.contains(" ")) { // phrase like "notice period", "working location"
                if (lowerLine.contains(term)) {
                    score += 8;
                }
            } else { // single word
                if (containsWord(lowerLine, term)) {
                    score += 3;
                }
            }
        }

        return score;
    }

    /**
     * Checks if a sentence contains a whole word.
     */
    private boolean containsWord(String text, String word) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
        return pattern.matcher(text).find();
    }

    /**
     * Extracts the specific answer from a matched line.
     * If the line is in "Key: Value" format, extracts and returns the clean Value.
     * Otherwise returns the cleaned line.
     */
    private String formatAnswer(String line) {
        String trimmed = line.trim();

        // Check for table row formatting (e.g., "Salary | Rs. 50,000 per month")
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            if (parts.length >= 2) {
                String candidate = parts[1].trim();
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        // Check for standard colon label formatting: "Salary: Rs. 50,000 per month"
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex != -1 && colonIndex < trimmed.length() - 1) {
            String label = trimmed.substring(0, colonIndex).trim();
            String value = trimmed.substring(colonIndex + 1).trim();

            // Only treat as label-value if label is concise (under 40 chars)
            if (!value.isBlank() && label.length() <= 40) {
                return value;
            }
        }

        // Check for hyphen separator: "Working Location - Pune"
        int hyphenIndex = trimmed.indexOf(" - ");
        if (hyphenIndex != -1 && hyphenIndex < trimmed.length() - 3) {
            String label = trimmed.substring(0, hyphenIndex).trim();
            String value = trimmed.substring(hyphenIndex + 3).trim();
            if (!value.isBlank() && label.length() <= 40) {
                return value;
            }
        }

        return trimmed;
    }
}
