package com.lks.controller;

import com.lks.bean.Score;
import com.lks.bean.User;
import com.lks.dto.ScoreResultResponse;
import com.lks.dto.ScoreSubmissionRequest;
import com.lks.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoreControllerTest {

    @Test
    void addStoresSafeScoreResultInSession() {
        ScoreService scoreService = mock(ScoreService.class);
        ScoreController controller = new ScoreController();
        ReflectionTestUtils.setField(controller, "scoreServiceImpl", scoreService);
        ScoreSubmissionRequest request = new ScoreSubmissionRequest();
        User user = new User();
        user.setId(42);
        MockHttpSession session = new MockHttpSession();
        Score savedScore = savedScore();
        when(scoreService.submit(request, 42)).thenReturn(savedScore);

        ResponseEntity<ScoreResultResponse> response = controller.add(request, user, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ScoreResultResponse body = response.getBody();
        assertEquals(new ScoreResultResponse(99, 123, "movie", "8.3333"), body);
        assertEquals(body, session.getAttribute("lastSubmittedScore"));
    }

    @Test
    void getLastSubmittedScoreReturnsSessionResult() {
        ScoreController controller = new ScoreController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        ScoreResultResponse scoreResult = new ScoreResultResponse(99, 123, "movie", "8.3333");
        session.setAttribute("lastSubmittedScore", scoreResult);
        request.setSession(session);

        ResponseEntity<?> response = controller.getLastSubmittedScore(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(scoreResult, response.getBody());
    }

    @Test
    void getLastSubmittedScoreReturnsNotFoundWithoutSessionResult() {
        ScoreController controller = new ScoreController();
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.getLastSubmittedScore(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Score savedScore() {
        Score score = new Score();
        score.setId(99);
        score.setvId(123);
        score.setVideoType("movie");
        score.setScore("8.3333");
        score.setuId(42);
        return score;
    }
}
