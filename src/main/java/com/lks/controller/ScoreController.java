package com.lks.controller;

import com.lks.bean.Score;
import com.lks.bean.User;
import com.lks.dto.ScoreResultResponse;
import com.lks.dto.ScoreSubmissionRequest;
import com.lks.exception.RateLimitExceededException;
import com.lks.service.RequestRateLimiter;
import com.lks.service.ScoreService;
//import com.mysql.cj.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/score")
public class ScoreController {
	private static final String LAST_SUBMITTED_SCORE_ATTRIBUTE = "lastSubmittedScore";
	private static final int MAX_SCORE_SUBMISSIONS_PER_WINDOW = 30;
	private static final Duration SCORE_SUBMISSION_RATE_LIMIT_WINDOW = Duration.ofHours(1);

	@Autowired
	private ScoreService scoreServiceImpl;

	@Autowired
	private RequestRateLimiter requestRateLimiter = new RequestRateLimiter();

	private final ScoreListRequestValidator scoreListRequestValidator = new ScoreListRequestValidator();

//	@GetMapping("/getAvgFraction")
//	public ResponseEntity<Map> getAverageFraction() {
//		List<String> averageFractionX = scoreServiceImpl.getAverageFractionX();
//		List<String> averageFractionY = scoreServiceImpl.getAverageFractionY();
//
//		Map map = new HashMap();
//		map.put("avgX", averageFractionX);
//		map.put("avgY", averageFractionY);
//		return ResponseEntity.ok(map);
//	}
//	@GetMapping("/getAvgFraction")
//    public ResponseEntity<Map<String, Object>> getAverageFraction() {
//        List<String> averageFractionX = scoreServiceImpl.getAverageFractionX();
//        List<Double> averageFractionY = scoreServiceImpl.getAverageFractionY();
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("avgX", averageFractionX);
//        response.put("avgY", averageFractionY);
//
//        return ResponseEntity.ok(response);
//    }
	@GetMapping("/getAvgFraction")
	public ResponseEntity<Map<String, Object>> getAverageFraction() {
	    List<String> averageFractionX = scoreServiceImpl.getAverageFractionX();
	    List<Double> averageFractionY = scoreServiceImpl.getAverageFractionY();

	    if (averageFractionX == null || averageFractionY == null || averageFractionX.isEmpty() || averageFractionY.isEmpty()) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("avgX", List.of("No Data"));
	        response.put("avgY", List.of(0.0));
	        return ResponseEntity.ok(response);
	    }

	    Map<String, Object> response = new HashMap<>();
	    response.put("avgX", averageFractionX);
	    response.put("avgY", averageFractionY);
	    return ResponseEntity.ok(response);
	}

	@GetMapping("/getOrderAvg")
	public ResponseEntity<List<Score>> getOrderAvg() {
		List<Score> avgScoreListDesc = scoreServiceImpl.getAvgScoreListDesc();
		return ResponseEntity.ok(avgScoreListDesc);
	}

	@GetMapping("/getMovieAvgDesc")
	public ResponseEntity<List<Score>> getMovieAvgDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> avgMovieScoreListDesc = scoreServiceImpl.getMovieAvgScoreListDesc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(avgMovieScoreListDesc);
	}

	@GetMapping("/getMovieAvgAsc")
	public ResponseEntity<List<Score>> getMovieAvgAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> avgMovieScoreListAsc = scoreServiceImpl.getMovieAvgScoreListAsc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(avgMovieScoreListAsc);
	}

	@GetMapping("/getTVAvgDesc")
	public ResponseEntity<List<Score>> getTVAvgDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> avgTVScoreListDesc = scoreServiceImpl.getTVAvgScoreListDesc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(avgTVScoreListDesc);
	}

	@GetMapping("/getTVAvgAsc")
	public ResponseEntity<List<Score>> getTVAvgAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> avgTVScoreListAsc = scoreServiceImpl.getTVAvgScoreListAsc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(avgTVScoreListAsc);
	}

	@GetMapping("/getMovieScoreCountDesc")
	public ResponseEntity<List<Score>> getMovieScoreCountDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> movieScoreCountDesc = scoreServiceImpl.getMovieScoresCountDesc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(movieScoreCountDesc);
	}

	@GetMapping("/getMovieScoreCountAsc")
	public ResponseEntity<List<Score>> getMovieScoreCountAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> movieScoreCountAsc = scoreServiceImpl.getMovieScoresCountAsc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(movieScoreCountAsc);
	}

	@GetMapping("/getTVScoreCountDesc")
	public ResponseEntity<List<Score>> getTVScoreCountDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> tvScoreCountDesc = scoreServiceImpl.getTVScoresCountDesc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(tvScoreCountDesc);
	}

	@GetMapping("/getTVScoreCountAsc")
	public ResponseEntity<List<Score>> getTVScoreCountAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		ScoreListRequestValidator.ScoreListFilters filters = scoreListRequestValidator.validate(limit, offset, country,
				genre, year);
		List<Score> tvScoreCountAsc = scoreServiceImpl.getTVScoresCountAsc(filters.limit(), filters.offset(),
				filters.country(), filters.genre(), filters.year());
		return ResponseEntity.ok(tvScoreCountAsc);
	}

	@GetMapping("/getScoreAvg/{vId}/{videoType}")
	public ResponseEntity<List<String>> getScoreAvg(@PathVariable("vId") String vId,
			@PathVariable("videoType") String videoType) {
		List<String> getAvgScoreByIdAndTitle = scoreServiceImpl.getAvgScoreByIdAndTitle(vId, videoType);
		return ResponseEntity.ok(getAvgScoreByIdAndTitle);
	}

//	@GetMapping("/getCountFraction")
//	public ResponseEntity<Map> getCountMovieFraction() {
//		Map movieCount = scoreServiceImpl.getMovieCount();
//		return ResponseEntity.ok(movieCount);
//	}
	@GetMapping("/getCountFraction")
    public ResponseEntity<Map<String, Object>> getCountMovieFraction() {
        Map<String, Object> movieCount = scoreServiceImpl.getMovieCount();
        return ResponseEntity.ok(movieCount);
    }

    @GetMapping("/getTotalRated")
    public ResponseEntity<Map<String, Object>> getTotalRatedMovies() {
        int totalRatedMovies = scoreServiceImpl.getTotalRatedMovies();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", totalRatedMovies);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/getTop20Popularity")
    public ResponseEntity<List<Score>> getTop20ByPopularity() {
        List<Score> top20Popularity = scoreServiceImpl.getTop20ByPopularityDesc();
        return ResponseEntity.ok(top20Popularity);
    }


	/**
	 * 
	 *
	 * @param id
	 * 
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Score> queryById(@PathVariable("id") Integer id) {
		return ResponseEntity.ok(scoreServiceImpl.queryById(id));
	}

	/**
	 * 
	 *
	 * @param score
	 * @param pageRequest
	 * 
	 */
	@GetMapping("/getPage")
	public ResponseEntity<Page<Score>> paginQuery(Score score, PageRequest pageRequest) {
		return ResponseEntity.ok(scoreServiceImpl.paginQuery(score, pageRequest));
	}

	@GetMapping("/getScoreList/{uid}")
	public ResponseEntity<?> getScoreList(@PathVariable("uid") String uid,
			@SessionAttribute(name = "loggedInUser", required = false) User loggedInUser) {
		if (loggedInUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login required."));
		}
		if (!isAdmin(loggedInUser) && !String.valueOf(loggedInUser.getId()).equals(uid)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Cannot view another user's scores."));
		}

		return ResponseEntity.ok(scoreServiceImpl.getScoreByUId(uid));
	}

	@GetMapping("/last-submission")
	public ResponseEntity<?> getLastSubmittedScore(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No recent score submission."));
		}
		Object scoreResult = session.getAttribute(LAST_SUBMITTED_SCORE_ATTRIBUTE);
		if (!(scoreResult instanceof ScoreResultResponse response)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No recent score submission."));
		}
		return ResponseEntity.ok(response);
	}

	/**
	 * 
	 *
	 * @param score
	 * @return
	 */
	@PostMapping("/add")
	public ResponseEntity<ScoreResultResponse> add(@RequestBody ScoreSubmissionRequest request,
			@SessionAttribute(name = "loggedInUser", required = false) User loggedInUser,
			HttpSession session) {
		enforceScoreSubmissionRateLimit(session);
		Integer authenticatedUserId = loggedInUser != null ? loggedInUser.getId() : null;
		Score savedScore = scoreServiceImpl.submit(request, authenticatedUserId);
		ScoreResultResponse response = ScoreResultResponse.from(savedScore);
		session.setAttribute(LAST_SUBMITTED_SCORE_ATTRIBUTE, response);
		return ResponseEntity.ok(response);
	}

	/**
	 * 
	 *
	 * @param score
	 * @return
	 */

	@PutMapping("/update")
	public ResponseEntity<?> edit(Score score,
			@SessionAttribute(name = "loggedInUser", required = false) User loggedInUser) {
		if (!isAdmin(loggedInUser)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
		return ResponseEntity.ok(scoreServiceImpl.update(score));
	}

	/**
	 * 
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteById(Integer id,
			@SessionAttribute(name = "loggedInUser", required = false) User loggedInUser) {
		if (!isAdmin(loggedInUser)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
		return ResponseEntity.ok(scoreServiceImpl.deleteById(id));
	}

	private boolean isAdmin(User user) {
		return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
	}

	private void enforceScoreSubmissionRateLimit(HttpSession session) {
		String sessionId = session == null ? "unknown" : session.getId();
		String key = "score:add:session:" + sessionId;
		if (!requestRateLimiter.tryAcquire(key, MAX_SCORE_SUBMISSIONS_PER_WINDOW, SCORE_SUBMISSION_RATE_LIMIT_WINDOW)) {
			throw new RateLimitExceededException("Too many score submissions. Please try again later.");
		}
	}
}
