package com.lks.controller;

import com.lks.bean.Score;
import com.lks.bean.User;
import com.lks.dto.ScoreSubmissionRequest;
import com.lks.service.ScoreService;
//import com.mysql.cj.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/score")
public class ScoreController {

	@Autowired
	private ScoreService scoreServiceImpl;

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
		List<Score> avgMovieScoreListDesc = scoreServiceImpl.getMovieAvgScoreListDesc(limit, offset, country, genre,
				year);
		return ResponseEntity.ok(avgMovieScoreListDesc);
	}

	@GetMapping("/getMovieAvgAsc")
	public ResponseEntity<List<Score>> getMovieAvgAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> avgMovieScoreListAsc = scoreServiceImpl.getMovieAvgScoreListAsc(limit, offset, country, genre,
				year);
		return ResponseEntity.ok(avgMovieScoreListAsc);
	}

	@GetMapping("/getTVAvgDesc")
	public ResponseEntity<List<Score>> getTVAvgDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> avgTVScoreListDesc = scoreServiceImpl.getTVAvgScoreListDesc(limit, offset, country, genre, year);
		return ResponseEntity.ok(avgTVScoreListDesc);
	}

	@GetMapping("/getTVAvgAsc")
	public ResponseEntity<List<Score>> getTVAvgAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> avgTVScoreListAsc = scoreServiceImpl.getTVAvgScoreListAsc(limit, offset, country, genre, year);
		return ResponseEntity.ok(avgTVScoreListAsc);
	}

	@GetMapping("/getMovieScoreCountDesc")
	public ResponseEntity<List<Score>> getMovieScoreCountDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> movieScoreCountDesc = scoreServiceImpl.getMovieScoresCountDesc(limit, offset, country, genre, year);
		return ResponseEntity.ok(movieScoreCountDesc);
	}

	@GetMapping("/getMovieScoreCountAsc")
	public ResponseEntity<List<Score>> getMovieScoreCountAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> movieScoreCountAsc = scoreServiceImpl.getMovieScoresCountAsc(limit, offset, country, genre, year);
		return ResponseEntity.ok(movieScoreCountAsc);
	}

	@GetMapping("/getTVScoreCountDesc")
	public ResponseEntity<List<Score>> getTVScoreCountDesc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> tvScoreCountDesc = scoreServiceImpl.getTVScoresCountDesc(limit, offset, country, genre, year);
		return ResponseEntity.ok(tvScoreCountDesc);
	}

	@GetMapping("/getTVScoreCountAsc")
	public ResponseEntity<List<Score>> getTVScoreCountAsc(@RequestParam int limit, @RequestParam int offset,
			@RequestParam(required = false) String country, @RequestParam(required = false) String genre,
			@RequestParam(required = false) String year) {
		List<Score> tvScoreCountAsc = scoreServiceImpl.getTVScoresCountAsc(limit, offset, country, genre, year);
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
	public ResponseEntity<List<Score>> getScoreList(@PathVariable("uid") String uid) {

		return ResponseEntity.ok(scoreServiceImpl.getScoreByUId(uid));
	}

	/**
	 * 
	 *
	 * @param score
	 * @return
	 */
	@PostMapping("/add")
	public ResponseEntity<Score> add(@RequestBody ScoreSubmissionRequest request,
			@SessionAttribute(name = "loggedInUser", required = false) User loggedInUser) {
		Integer authenticatedUserId = loggedInUser != null ? loggedInUser.getId() : null;
		return ResponseEntity.ok(scoreServiceImpl.submit(request, authenticatedUserId));
	}

	/**
	 * 
	 *
	 * @param score
	 * @return
	 */

	@PutMapping("/update")
	public ResponseEntity<Score> edit(Score score) {
		return ResponseEntity.ok(scoreServiceImpl.update(score));
	}

	/**
	 * 
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping("/delete")
	public ResponseEntity<Boolean> deleteById(Integer id) {
		return ResponseEntity.ok(scoreServiceImpl.deleteById(id));
	}
}
