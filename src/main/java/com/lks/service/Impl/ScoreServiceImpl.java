package com.lks.service.Impl;

import com.lks.bean.Score;
import com.lks.dto.ScoreSubmissionRequest;
import com.lks.mapper.ScoreMapper;
import com.lks.service.ScoreService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScoreServiceImpl implements ScoreService {
	private static final int QUESTION_COUNT = 15;
	private static final int OPTION_COUNT = 5;
	private static final int MIN_RELEASE_YEAR = 1888;
	private static final int MAX_FUTURE_RELEASE_YEARS = 5;
	private static final int MAX_VIDEO_NAME_LENGTH = 255;
	private static final int MAX_IMAGE_URL_LENGTH = 1000;
	private static final int MAX_GENRES = 30;
	private static final int MAX_COUNTRIES = 50;
	private static final Set<String> VALID_VIDEO_TYPES = Set.of("movie", "tv");
	private static final Set<String> ALLOWED_IMAGE_HOSTS = Set.of("image.tmdb.org");
	private static final int[][] QUESTION_OPTION_SCORES = {
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

	private final ScoreMapper scoreMapper;

	public ScoreServiceImpl(ScoreMapper scoreMapper) {
		this.scoreMapper = scoreMapper;
	}

	@Override
    public List<String> getAverageFractionX() {
        return scoreMapper.getAverageFractionX();
    }

	@Override
	public List<Double> getAverageFractionY() {
	    List<Double> averageFractionY = scoreMapper.getAverageFractionY();
	    return averageFractionY.stream()
	            .map(score -> score != null ? score : 0.0)
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

	@Override
	@Transactional
	public Score submit(ScoreSubmissionRequest request, Integer authenticatedUserId) {
		return insert(buildScoreFromSubmission(request, authenticatedUserId));
	}

	Score buildScoreFromSubmission(ScoreSubmissionRequest request, Integer authenticatedUserId) {
		List<Integer> answers = validateSubmission(request);

		Score score = new Score();
		score.setuId(authenticatedUserId);
		score.setvId(request.getvId());
		score.setVideoName(request.getVideoName().trim());
		score.setVideoType(request.getVideoType().trim().toLowerCase());
		score.setvImg(trimToNull(request.getvImg()));
		score.setScore(calculateScore(answers));
		score.setReleaseYear(request.getReleaseYear());
		score.setGenres(request.getGenres());
		score.setCountries(normalizeCountries(request.getCountries()));
		return score;
	}

	@Transactional
	public Score insert(Score score) {
		scoreMapper.insert(score);

		if ("movie".equals(score.getVideoType())) {
			scoreMapper.insertReleaseYear(score.getvId(), score.getReleaseYear());

			if (score.getGenres() != null && !score.getGenres().isEmpty()) {
				scoreMapper.insertGenres(score.getvId(), score.getGenres());
			}

			if (score.getCountries() != null && !score.getCountries().isEmpty()) {
				List<Integer> countryIds = scoreMapper.getCountryIdsByShortNames(score.getCountries());

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
		if (score.getPopularity() != null) {
			updateOrInsertPopularity(score.getvId(), score.getVideoType(), score.getPopularity());
		}

		
		return score;
	}
	
	@Transactional
	public boolean updateOrInsertPopularity(int vId, String videoType, Double popularity) {
		if (popularity == null || !Double.isFinite(popularity) || popularity < 0) {
			return false;
		}

	    Double existingPopularity = scoreMapper.getPopularityByVIdAndType(vId, videoType);

	    if (existingPopularity == null) {
	        return scoreMapper.insertPopularity(vId, videoType, popularity) > 0;
	    } else {
	        final double TOLERANCE = 0.0001;

	        if (Math.abs(existingPopularity - popularity) > TOLERANCE) {
	            return scoreMapper.updatePopularity(vId, videoType, popularity) > 0;
	        }
	    }

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

	private List<Integer> validateSubmission(ScoreSubmissionRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Invalid score submission.");
		}
		if (!request.getUnsupportedFields().isEmpty()) {
			String fieldName = request.getUnsupportedFields().keySet().iterator().next();
			throw new IllegalArgumentException("Unsupported score submission field: " + fieldName);
		}
		if (request.getvId() == null || request.getvId() <= 0) {
			throw new IllegalArgumentException("Video id is required.");
		}
		if (isBlank(request.getVideoType()) || !VALID_VIDEO_TYPES.contains(request.getVideoType().trim().toLowerCase())) {
			throw new IllegalArgumentException("Video type must be movie or tv.");
		}
		validateVideoName(request.getVideoName());
		validateImageUrl(request.getvImg());
		validateReleaseYear(request.getReleaseYear());
		List<Integer> answers = validateAnswers(request.getAnswers());
		validateGenres(request.getGenres());
		validateCountries(request.getCountries());
		return answers;
	}

	private void validateVideoName(String videoName) {
		if (isBlank(videoName) || videoName.trim().length() > MAX_VIDEO_NAME_LENGTH) {
			throw new IllegalArgumentException("Video name is required.");
		}
		if (containsHtmlBoundary(videoName) || containsControlCharacter(videoName)) {
			throw new IllegalArgumentException("Video name contains invalid characters.");
		}
	}

	private void validateImageUrl(String imageUrl) {
		String normalizedImageUrl = trimToNull(imageUrl);
		if (normalizedImageUrl == null) {
			return;
		}
		if (normalizedImageUrl.length() > MAX_IMAGE_URL_LENGTH) {
			throw new IllegalArgumentException("Video image URL is too long.");
		}

		URI uri;
		try {
			uri = URI.create(normalizedImageUrl);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Video image URL is invalid.");
		}

		if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
				|| !ALLOWED_IMAGE_HOSTS.contains(uri.getHost().toLowerCase())) {
			throw new IllegalArgumentException("Video image URL is invalid.");
		}
	}

	private void validateReleaseYear(Integer releaseYear) {
		int maxReleaseYear = Year.now().getValue() + MAX_FUTURE_RELEASE_YEARS;
		if (releaseYear == null || releaseYear < MIN_RELEASE_YEAR || releaseYear > maxReleaseYear) {
			throw new IllegalArgumentException("Release year is invalid.");
		}
	}

	private List<Integer> validateAnswers(List<?> answers) {
		if (answers == null || answers.size() != QUESTION_COUNT) {
			throw new IllegalArgumentException("Exactly 15 answers are required.");
		}
		List<Integer> normalizedAnswers = new ArrayList<>(QUESTION_COUNT);
		for (int i = 0; i < answers.size(); i++) {
			Object submittedAnswer = answers.get(i);
			if (!(submittedAnswer instanceof Integer answer) || answer < 1 || answer > OPTION_COUNT) {
				throw new IllegalArgumentException("Answer " + (i + 1) + " is invalid.");
			}
			normalizedAnswers.add(answer);
		}
		return normalizedAnswers;
	}

	private void validateGenres(List<Integer> genres) {
		if (genres == null) {
			return;
		}
		if (genres.size() > MAX_GENRES) {
			throw new IllegalArgumentException("Too many genres were submitted.");
		}
		for (Integer genre : genres) {
			if (genre == null || genre <= 0) {
				throw new IllegalArgumentException("Genre id is invalid.");
			}
		}
	}

	private void validateCountries(List<String> countries) {
		if (countries == null) {
			return;
		}
		if (countries.size() > MAX_COUNTRIES) {
			throw new IllegalArgumentException("Too many countries were submitted.");
		}
		for (String country : countries) {
			if (country == null || !country.trim().matches("[A-Za-z]{2}")) {
				throw new IllegalArgumentException("Country code is invalid.");
			}
		}
	}

	private String calculateScore(List<Integer> answers) {
		int scoreSum = 0;
		for (int questionIndex = 0; questionIndex < QUESTION_COUNT; questionIndex++) {
			int answerIndex = answers.get(questionIndex) - 1;
			scoreSum += QUESTION_OPTION_SCORES[questionIndex][answerIndex];
		}
		return BigDecimal.valueOf(scoreSum)
				.divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}

	private List<String> normalizeCountries(List<String> countries) {
		if (countries == null) {
			return null;
		}
		return countries.stream()
				.map(country -> country.trim().toUpperCase())
				.toList();
	}

	private String trimToNull(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}

	private boolean containsHtmlBoundary(String value) {
		return value.indexOf('<') >= 0 || value.indexOf('>') >= 0;
	}

	private boolean containsControlCharacter(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isISOControl(value.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
