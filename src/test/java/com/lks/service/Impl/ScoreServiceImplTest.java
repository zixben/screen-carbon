package com.lks.service.Impl;

import com.lks.bean.Score;
import com.lks.dto.ScoreSubmissionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreServiceImplTest {

    private final ScoreServiceImpl service = new ScoreServiceImpl();

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
}
