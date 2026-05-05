package com.lks.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.lks.bean.Score;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ScoreService {

//	List getAverageFractionX();
//
//	List getAverageFractionY();
//
//	Map getMovieCount();
	List<String> getAverageFractionX();

    List<Double> getAverageFractionY();

    Map<String, Object> getMovieCount();

    int getTotalRatedMovies();

	List<Score> getAvgScoreListDesc();

	List<Score> getMovieAvgScoreListDesc(int limit, int offset, String country, String genre, String year);

	List<Score> getMovieAvgScoreListAsc(int limit, int offset, String country, String genre, String year);

	List<Score> getTVAvgScoreListDesc(int limit, int offset, String country, String genre, String year);

	List<Score> getTVAvgScoreListAsc(int limit, int offset, String country, String genre, String year);

	List<Score> getMovieScoresCountDesc(int limit, int offset, String country, String genre, String year);

	List<Score> getMovieScoresCountAsc(int limit, int offset, String country, String genre, String year);

	List<Score> getTVScoresCountDesc(int limit, int offset, String country, String genre, String year);

	List<Score> getTVScoresCountAsc(int limit, int offset, String country, String genre, String year);

	List<String> getAvgScoreByIdAndTitle(String VId, String videoType);

	List<Score> getScoreByUId(String UId);

	Score queryById(Integer id);

	Page<Score> paginQuery(Score score, PageRequest pageRequest);

	Score insert(Score score);

	Score update(Score score);
	
	boolean updateOrInsertPopularity(int vId, String videoType, Double popularity);

	boolean deleteById(Integer id);

	List<Score> getTop20ByPopularityDesc();

}
