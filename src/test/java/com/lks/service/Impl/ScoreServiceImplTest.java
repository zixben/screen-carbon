package com.lks.service.Impl;

import com.lks.bean.Score;
import com.lks.dto.ScoreSubmissionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreServiceImplTest {

    private final ScoreServiceImpl service = new ScoreServiceImpl(null);
    private static final int[][] EXPECTED_QUESTION_OPTION_SCORES = {
            { 4, 3, 2, 1, 0 },
            { 0, 1, 2, 3, 4 },
            { 4, 3, 2, 1, 0 },
            { 4, 3, 2, 1, 0 },
            { 0, 1, 2, 3, 4 },
            { 0, 1, 2, 3, 4 },
            { 4, 3, 2, 1, 0 },
            { 4, 3, 2, 1, 0 },
            { 0, 1, 2, 3, 4 },
            { 0, 1, 2, 3, 4 },
            { 4, 3, 2, 1, 0 },
            { 0, 1, 2, 3, 4 },
            { 4, 3, 2, 1, 0 },
            { 4, 3, 2, 1, 0 },
            { 4, 3, 2, 1, 0 }
    };

    @Test
    void buildScoreFromSubmissionCalculatesScoreAndUsesAuthenticatedUser() {
        Score score = service.buildScoreFromSubmission(validRequest(maximumScoreAnswers()), 42);

        assertEquals(42, score.getuId());
        assertEquals(123, score.getvId());
        assertEquals("movie", score.getVideoType());
        assertEquals("Test Movie", score.getVideoName());
        assertEquals("10", score.getScore());
        assertNull(score.getPopularity());
    }

    @Test
    void buildScoreFromSubmissionCalculatesNeutralScore() {
        ScoreSubmissionRequest request = validRequest(List.of(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3));

        Score score = service.buildScoreFromSubmission(request, null);

        assertNull(score.getuId());
        assertEquals("5", score.getScore());
    }

    @Test
    void buildScoreFromSubmissionCalculatesMinimumScore() {
        Score score = service.buildScoreFromSubmission(validRequest(minimumScoreAnswers()), null);

        assertEquals("0", score.getScore());
    }

    @Test
    void buildScoreFromSubmissionUsesPublishedQuestionOptionScoringMatrix() {
        for (int questionIndex = 0; questionIndex < EXPECTED_QUESTION_OPTION_SCORES.length; questionIndex++) {
            for (int option = 1; option <= 5; option++) {
                List<Integer> answers = neutralAnswers();
                answers.set(questionIndex, option);

                Score score = service.buildScoreFromSubmission(validRequest(answers), null);
                int expectedPointTotal = 30 - 2 + EXPECTED_QUESTION_OPTION_SCORES[questionIndex][option - 1];

                assertEquals(scoreFromPointTotal(expectedPointTotal), score.getScore(),
                        "Question " + (questionIndex + 1) + " option " + option + " score changed");
            }
        }
    }

    @Test
    void buildScoreFromSubmissionRoundsRepeatingDecimalScoresToFourPlaces() {
        List<Integer> answers = minimumScoreAnswers();
        answers.set(0, 4);

        Score score = service.buildScoreFromSubmission(validRequest(answers), null);

        assertEquals("0.1667", score.getScore());
    }

    @Test
    void rateTemplateOptionValuesMatchBackendScoringMatrix() throws Exception {
        String rateTemplate = Files.readString(Path.of("src/main/resources/templates/rate.html"));

        for (int question = 1; question <= EXPECTED_QUESTION_OPTION_SCORES.length; question++) {
            for (int option = 1; option <= 5; option++) {
                Pattern inputPattern = Pattern.compile(
                        "<input[^>]*id=\"question" + question + "Option" + option + "\"[^>]*value=\"(\\d)\"",
                        Pattern.DOTALL);
                Matcher matcher = inputPattern.matcher(rateTemplate);

                assertTrue(matcher.find(), "Missing input for question " + question + " option " + option);
                assertEquals(String.valueOf(EXPECTED_QUESTION_OPTION_SCORES[question - 1][option - 1]),
                        matcher.group(1), "Template value mismatch for question " + question + " option " + option);
            }
        }
    }

    @Test
    void buildScoreFromSubmissionRejectsClientControlledScoreField() {
        ScoreSubmissionRequest request = validRequest(maximumScoreAnswers());
        request.addUnsupportedField("score", 999);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.buildScoreFromSubmission(request, 42));

        assertTrue(exception.getMessage().contains("Unsupported score submission field: score"));
    }

    @Test
    void buildScoreFromSubmissionRejectsInvalidAnswerValue() {
        ScoreSubmissionRequest request = validRequest(List.of(1, 5, 1, 1, 5, 5, 1, 1, 5, 5, 1, 5, 1, 1, 6));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.buildScoreFromSubmission(request, 42));

        assertEquals("Answer 15 is invalid.", exception.getMessage());
    }

    @Test
    void buildScoreFromSubmissionRejectsNonIntegerAnswer() {
        ScoreSubmissionRequest request = validRequest(List.of(1, 5, 1, 1, 5, 5, 1, 1, 5, 5, 1, 5, 1, 1, "1"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.buildScoreFromSubmission(request, 42));

        assertEquals("Answer 15 is invalid.", exception.getMessage());
    }

    @Test
    void buildScoreFromSubmissionRejectsHtmlInVideoName() {
        ScoreSubmissionRequest request = validRequest(maximumScoreAnswers());
        request.setVideoName("<img src=x onerror=alert(1)>");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.buildScoreFromSubmission(request, 42));

        assertEquals("Video name contains invalid characters.", exception.getMessage());
    }

    @Test
    void buildScoreFromSubmissionRejectsNonTmdbImageUrl() {
        ScoreSubmissionRequest request = validRequest(maximumScoreAnswers());
        request.setvImg("https://example.com/poster.jpg");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.buildScoreFromSubmission(request, 42));

        assertEquals("Video image URL is invalid.", exception.getMessage());
    }

    private ScoreSubmissionRequest validRequest(List<?> answers) {
        ScoreSubmissionRequest request = new ScoreSubmissionRequest();
        request.setvId(123);
        request.setVideoType("MOVIE");
        request.setVideoName(" Test Movie ");
        request.setvImg("https://image.tmdb.org/t/p/original/poster.jpg");
        request.setReleaseYear(2024);
        request.setGenres(List.of(18, 35));
        request.setCountries(List.of("gb", "US"));
        request.setAnswers(answers);
        return request;
    }

    private List<Integer> maximumScoreAnswers() {
        return List.of(1, 5, 1, 1, 5, 5, 1, 1, 5, 5, 1, 5, 1, 1, 1);
    }

    private List<Integer> minimumScoreAnswers() {
        return new ArrayList<>(List.of(5, 1, 5, 5, 1, 1, 5, 5, 1, 1, 5, 1, 5, 5, 5));
    }

    private List<Integer> neutralAnswers() {
        return new ArrayList<>(List.of(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3));
    }

    private String scoreFromPointTotal(int pointTotal) {
        return BigDecimal.valueOf(pointTotal)
                .divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
