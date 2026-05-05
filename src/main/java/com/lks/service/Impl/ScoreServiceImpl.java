package com.lks.service.Impl;

import com.lks.bean.Score;
import com.lks.mapper.ScoreMapper;
import com.lks.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreServiceImpl implements ScoreService {
	@Autowired
	private ScoreMapper scoreMapper;

	/*
	 * @Override public List<?> getAverageFractionX() { List<String> averageFraction
	 * = scoreMapper.getAverageFractionX(); return averageFraction; }
	 * 
	 * @Override public List<Double> getAverageFractionY() { List<Double>
	 * averageFraction = scoreMapper.getAverageFractionY(); return averageFraction;
	 * }
	 * 
	 * @SuppressWarnings({ "rawtypes", "unchecked" })
	 * 
	 * @Override public Map getMovieCount() { List<String> countX =
	 * scoreMapper.getCountX(); List<Integer> countY = scoreMapper.getCountY(); Map
	 * map = new HashMap(); map.put("countX", countX); map.put("countY", countY);
	 * return map; }
	 */
	@Override
    public List<String> getAverageFractionX() {
        return scoreMapper.getAverageFractionX();
    }

//    @Override
//    public List<Double> getAverageFractionY() {
//        return scoreMapper.getAverageFractionY();
//    }
	@Override
	public List<Double> getAverageFractionY() {
	    List<Double> averageFractionY = scoreMapper.getAverageFractionY();
	    return averageFractionY.stream()
	            .map(score -> score != null ? score : 0.0) // Multiply by 10, round to 2 decimals
	            .toList();
	}

    @Override
    public Map<String, Object> getMovieCount() {
        List<String> countX = scoreMapper.getCountX();
        List<Integer> countY = scoreMapper.getCountY();

        Map<String, Object> response = new HashMap<>();
        response.put("countX", countX);
        response.put("countY", countY);

        return response;
    }

    @Override
    public int getTotalRatedMovies() {
        return scoreMapper.getTotalRatedMovies();
    }

	@Override
	public List<Score> getAvgScoreListDesc() {
		return scoreMapper.getAvgScoreListDesc();
	}

	@Override
	public List<Score> getMovieAvgScoreListDesc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getMovieAvgScoreListDesc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getMovieAvgScoreListAsc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getMovieAvgScoreListAsc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getTVAvgScoreListDesc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getTVAvgScoreListDesc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getTVAvgScoreListAsc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getTVAvgScoreListAsc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getMovieScoresCountDesc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getMovieScoresCountDesc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getMovieScoresCountAsc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getMovieScoresCountAsc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getTVScoresCountDesc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getTVScoresCountDesc(limit, offset, country, genre, year);
	}

	@Override
	public List<Score> getTVScoresCountAsc(int limit, int offset, String country, String genre, String year) {
		return scoreMapper.getTVScoresCountAsc(limit, offset, country, genre, year);
	}

	@Override
	public List<String> getAvgScoreByIdAndTitle(String VId, String videoType) {
		return scoreMapper.getAvgScoreByIdAndTitle(VId, videoType);
	}

	@Override
	public List<Score> getScoreByUId(String UId) {

		return scoreMapper.getScoreByUId(UId);
	}

	public Score queryById(Integer id) {
		return scoreMapper.queryById(id);
	}

	public Page<Score> paginQuery(Score score, PageRequest pageRequest) {

		long total = scoreMapper.count(score);
		return new PageImpl<>(scoreMapper.queryAllByLimit(score, pageRequest), pageRequest, total);
	}

	@Transactional
	public Score insert(Score score) {
		scoreMapper.insert(score);

		if ("movie".equals(score.getVideoType())) {
			// Insert the release year into the movies_release_year table
			scoreMapper.insertReleaseYear(score.getvId(), score.getReleaseYear());

			// Insert genres into the movies_genres table
			if (score.getGenres() != null && !score.getGenres().isEmpty()) {
				scoreMapper.insertGenres(score.getvId(), score.getGenres());
			}

			// Insert countries into the movies_countries table
			if (score.getCountries() != null && !score.getCountries().isEmpty()) {
				// Step 1: Retrieve country IDs based on short names
				List<Integer> countryIds = scoreMapper.getCountryIdsByShortNames(score.getCountries());

				// Insert the country IDs into the movies_countries table
				if (!countryIds.isEmpty()) {
					scoreMapper.insertCountries(score.getvId(), countryIds);
				}
			}
		} else if ("tv".equals(score.getVideoType())) {
			scoreMapper.insertTvReleaseYear(score.getvId(), score.getReleaseYear());
			if (score.getGenres() != null && !score.getGenres().isEmpty()) {
				scoreMapper.insertTvGenres(score.getvId(), score.getGenres());
			}
			if (score.getCountries() != null && !score.getCountries().isEmpty()) {
				List<Integer> countryIds = scoreMapper.getTvCountryIdsByShortNames(score.getCountries());
				if (!countryIds.isEmpty()) {
					scoreMapper.insertTvCountries(score.getvId(), countryIds);
				}
			}
		}
		updateOrInsertPopularity(score.getvId(), score.getVideoType(), score.getPopularity());

		
		return score;
	}
	
	@Transactional
	public boolean updateOrInsertPopularity(int vId, String videoType, Double popularity) {
	    // Check if the popularity record exists
	    Double existingPopularity = scoreMapper.getPopularityByVIdAndType(vId, videoType);

	    if (existingPopularity == null) {
	        System.out.println("Inserting new popularity: " + popularity);
	        return scoreMapper.insertPopularity(vId, videoType, popularity) > 0;
	    } else {
	        final double TOLERANCE = 0.0001;

	        if (Math.abs(existingPopularity - popularity) > TOLERANCE) {
	            System.out.println("Updating popularity: Old=" + existingPopularity + ", New=" + popularity);
	            return scoreMapper.updatePopularity(vId, videoType, popularity) > 0;
	        } else {
	            System.out.println("No update needed: Old=" + existingPopularity + ", New=" + popularity);
	        }
	    }

	    // No changes were made
	    return false;
	}


	public Score update(Score score) {
		scoreMapper.update(score);
		return queryById(score.getId());
	}

	public boolean deleteById(Integer id) {
		int total = scoreMapper.deleteById(id);
		return total > 0;
	}
	
	@Override
	public List<Score> getTop20ByPopularityDesc() {
	    return scoreMapper.getTop20ByPopularityDesc();
	}

}