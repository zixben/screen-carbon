package com.lks.mapper;

import java.util.List;

import com.lks.bean.Score;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.domain.Pageable;

public interface ScoreMapper {

    Score queryById(Integer id);

    List<Score> queryAllByLimit(Score score, Pageable pageable);

    @Select("SELECT CONCAT(video_name, ' (', video_type, ')') "
            + "FROM score "
            + "GROUP BY v_id, video_type, video_name "
            + "ORDER BY AVG(CAST(score AS DECIMAL(10, 4))) DESC")
    List<String> getAverageFractionX();

    @Select("SELECT ROUND(AVG(CAST(score AS DECIMAL(10, 4))), 4) AS avgPercentage "
            + "FROM score "
            + "GROUP BY v_id, video_type, video_name "
            + "ORDER BY avgPercentage DESC")
    List<Double> getAverageFractionY();

    @Select("SELECT CONCAT(video_name, ' (', video_type, ')') "
            + "FROM score "
            + "GROUP BY v_id, video_type, video_name "
            + "ORDER BY COUNT(*) DESC "
            + "LIMIT 10")
    List<String> getCountX();

    @Select("SELECT COUNT(*) AS total "
            + "FROM score "
            + "GROUP BY v_id, video_type, video_name "
            + "ORDER BY total DESC "
            + "LIMIT 10")
    List<Integer> getCountY();

    @Select("SELECT COUNT(*) FROM (SELECT v_id, video_type FROM score GROUP BY v_id, video_type) rated_titles")
    int getTotalRatedMovies();

	@Select("select *  from score where u_id = #{UId}")
	List<Score> getScoreByUId(@Param("UId") String UId);

	@Select("SELECT v_id, video_name, ROUND(AVG(score), 2) score, v_img, video_type FROM score GROUP BY v_id, video_name, v_img, video_type ORDER BY AVG(score) DESC;")
	List<Score> getAvgScoreListDesc();

	@Select("SELECT SUM(score)/COUNT(score) avgscore FROM score WHERE v_id = #{VId} AND video_type= #{videoType};")
	List<String> getAvgScoreByIdAndTitle(@Param("VId") String VId, @Param("videoType") String videoType);

	@Select("<script>" + "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) score, s.v_img, s.video_type "
			+ "FROM score s " + "LEFT JOIN movies_countries mc ON s.v_id = mc.movie_id "
			+ "LEFT JOIN countries c ON c.id = mc.country_id " + "LEFT JOIN movies_genres mg ON s.v_id = mg.movie_id "
			+ "LEFT JOIN genres g ON g.id = mg.genre_id "
			+ "LEFT JOIN movies_release_year mry ON mry.movie_id = s.v_id " + "WHERE s.video_type = 'movie' "
			+ "<if test='country != null and !country.isEmpty()'>" + "AND c.short_name = #{country} " + "</if>"
			+ "<if test='genre != null and !genre.isEmpty()'>" + "AND g.id = #{genre} " + "</if>"
			+ "<if test='year != null and !year.isEmpty()'>" + "AND mry.year = #{year} " + "</if>"
			+ "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type " + "ORDER BY AVG(s.score) DESC "
			+ "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getMovieAvgScoreListDesc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>" + "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) score, s.v_img, s.video_type "
			+ "FROM score s " + "LEFT JOIN movies_countries mc ON s.v_id = mc.movie_id "
			+ "LEFT JOIN countries c ON c.id = mc.country_id " + "LEFT JOIN movies_genres mg ON s.v_id = mg.movie_id "
			+ "LEFT JOIN genres g ON g.id = mg.genre_id "
			+ "LEFT JOIN movies_release_year mry ON mry.movie_id = s.v_id " + "WHERE s.video_type = 'movie' "
			+ "<if test='country != null and !country.isEmpty()'>" + "AND c.short_name = #{country} " + "</if>"
			+ "<if test='genre != null and !genre.isEmpty()'>" + "AND g.id = #{genre} " + "</if>"
			+ "<if test='year != null and !year.isEmpty()'>" + "AND mry.year = #{year} " + "</if>"
			+ "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type " + "ORDER BY AVG(s.score) ASC "
			+ "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getMovieAvgScoreListAsc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>" + "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) score, s.v_img, s.video_type "
			+ "FROM score s " + "LEFT JOIN tv_countries mc ON s.v_id = mc.tv_id "
			+ "LEFT JOIN countries c ON c.id = mc.country_id " + "LEFT JOIN tv_genres mg ON s.v_id = mg.tv_id "
			+ "LEFT JOIN genres g ON g.id = mg.genre_id " + "LEFT JOIN tv_release_year mry ON mry.tv_id = s.v_id "
			+ "WHERE s.video_type = 'tv' " + "<if test='country != null and !country.isEmpty()'>"
			+ "AND c.short_name = #{country} " + "</if>" + "<if test='genre != null and !genre.isEmpty()'>"
			+ "AND g.id = #{genre} " + "</if>" + "<if test='year != null and !year.isEmpty()'>"
			+ "AND mry.year = #{year} " + "</if>" + "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type "
			+ "ORDER BY AVG(s.score) DESC " + "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getTVAvgScoreListDesc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>" + "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) score, s.v_img, s.video_type "
			+ "FROM score s " + "LEFT JOIN tv_countries mc ON s.v_id = mc.tv_id "
			+ "LEFT JOIN countries c ON c.id = mc.country_id " + "LEFT JOIN tv_genres mg ON s.v_id = mg.tv_id "
			+ "LEFT JOIN genres g ON g.id = mg.genre_id " + "LEFT JOIN tv_release_year mry ON mry.tv_id = s.v_id "
			+ "WHERE s.video_type = 'tv' " + "<if test='country != null and !country.isEmpty()'>"
			+ "AND c.short_name = #{country} " + "</if>" + "<if test='genre != null and !genre.isEmpty()'>"
			+ "AND g.id = #{genre} " + "</if>" + "<if test='year != null and !year.isEmpty()'>"
			+ "AND mry.year = #{year} " + "</if>" + "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type "
			+ "ORDER BY AVG(s.score) ASC " + "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getTVAvgScoreListAsc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>"
			+ "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) AS score, s.v_img, s.video_type, subquery.score_count AS count "
			+ "FROM score s " + "LEFT JOIN (" + "    SELECT v_id, COUNT(*) AS score_count " + "    FROM score "
			+ "    WHERE video_type = 'movie' " + "    GROUP BY v_id" + ") subquery ON s.v_id = subquery.v_id "
			+ "LEFT JOIN movies_countries mc ON s.v_id = mc.movie_id "
			+ "LEFT JOIN countries c ON c.id = mc.country_id " + "LEFT JOIN movies_genres mg ON s.v_id = mg.movie_id "
			+ "LEFT JOIN genres g ON g.id = mg.genre_id "
			+ "LEFT JOIN movies_release_year mry ON mry.movie_id = s.v_id " + "WHERE s.video_type = 'movie' "
			+ "<if test='country != null and !country.isEmpty()'>" + "AND c.short_name = #{country} " + "</if>"
			+ "<if test='genre != null and !genre.isEmpty()'>" + "AND g.id = #{genre} " + "</if>"
			+ "<if test='year != null and !year.isEmpty()'>" + "AND mry.year = #{year} " + "</if>"
			+ "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type, subquery.score_count "
			+ "ORDER BY subquery.score_count DESC " + "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getMovieScoresCountDesc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>"
			+ "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) AS score, s.v_img, s.video_type, subquery.score_count AS count "
			+ "FROM score s " + "LEFT JOIN (" + "    SELECT v_id, COUNT(*) AS score_count " + "    FROM score "
			+ "    WHERE video_type = 'movie' " + "    GROUP BY v_id" + ") subquery ON s.v_id = subquery.v_id "
			+ "LEFT JOIN movies_countries mc ON s.v_id = mc.movie_id "
			+ "LEFT JOIN countries c ON c.id = mc.country_id " + "LEFT JOIN movies_genres mg ON s.v_id = mg.movie_id "
			+ "LEFT JOIN genres g ON g.id = mg.genre_id "
			+ "LEFT JOIN movies_release_year mry ON mry.movie_id = s.v_id " + "WHERE s.video_type = 'movie' "
			+ "<if test='country != null and !country.isEmpty()'>" + "AND c.short_name = #{country} " + "</if>"
			+ "<if test='genre != null and !genre.isEmpty()'>" + "AND g.id = #{genre} " + "</if>"
			+ "<if test='year != null and !year.isEmpty()'>" + "AND mry.year = #{year} " + "</if>"
			+ "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type, subquery.score_count "
			+ "ORDER BY subquery.score_count ASC " + "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getMovieScoresCountAsc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>"
			+ "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) AS score, s.v_img, s.video_type, subquery.score_count AS count "
			+ "FROM score s " + "LEFT JOIN (" + "    SELECT v_id, COUNT(*) AS score_count " + "    FROM score "
			+ "    WHERE video_type = 'tv' " + "    GROUP BY v_id" + ") subquery ON s.v_id = subquery.v_id "
			+ "LEFT JOIN tv_countries mc ON s.v_id = mc.tv_id " + "LEFT JOIN countries c ON c.id = mc.country_id "
			+ "LEFT JOIN tv_genres mg ON s.v_id = mg.tv_id " + "LEFT JOIN genres g ON g.id = mg.genre_id "
			+ "LEFT JOIN tv_release_year mry ON mry.tv_id = s.v_id " + "WHERE s.video_type = 'tv' "
			+ "<if test='country != null and !country.isEmpty()'>" + "AND c.short_name = #{country} " + "</if>"
			+ "<if test='genre != null and !genre.isEmpty()'>" + "AND g.id = #{genre} " + "</if>"
			+ "<if test='year != null and !year.isEmpty()'>" + "AND mry.year = #{year} " + "</if>"
			+ "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type, subquery.score_count "
			+ "ORDER BY subquery.score_count DESC " + "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getTVScoresCountDesc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);

	@Select("<script>"
			+ "SELECT s.v_id, s.video_name, ROUND(AVG(s.score), 2) AS score, s.v_img, s.video_type, subquery.score_count AS count "
			+ "FROM score s " + "LEFT JOIN (" + "    SELECT v_id, COUNT(*) AS score_count " + "    FROM score "
			+ "    WHERE video_type = 'tv' " + "    GROUP BY v_id" + ") subquery ON s.v_id = subquery.v_id "
			+ "LEFT JOIN tv_countries mc ON s.v_id = mc.tv_id " + "LEFT JOIN countries c ON c.id = mc.country_id "
			+ "LEFT JOIN tv_genres mg ON s.v_id = mg.tv_id " + "LEFT JOIN genres g ON g.id = mg.genre_id "
			+ "LEFT JOIN tv_release_year mry ON mry.tv_id = s.v_id " + "WHERE s.video_type = 'tv' "
			+ "<if test='country != null and !country.isEmpty()'>" + "AND c.short_name = #{country} " + "</if>"
			+ "<if test='genre != null and !genre.isEmpty()'>" + "AND g.id = #{genre} " + "</if>"
			+ "<if test='year != null and !year.isEmpty()'>" + "AND mry.year = #{year} " + "</if>"
			+ "GROUP BY s.v_id, s.video_name, s.v_img, s.video_type, subquery.score_count "
			+ "ORDER BY subquery.score_count ASC " + "LIMIT #{limit} OFFSET #{offset};" + "</script>")
	List<Score> getTVScoresCountAsc(@Param("limit") int limit, @Param("offset") int offset,
			@Param("country") String country, @Param("genre") String genre, @Param("year") String year);
	
	@Select("<script>" +
	        "SELECT p.v_id, p.video_type, p.popularity, " +
	        "       s.video_name, ROUND(AVG(s.score), 2) AS score, s.v_img " +
	        "FROM popularity p " +
	        "LEFT JOIN score s " +
	        "ON p.v_id = s.v_id AND p.video_type = s.video_type " +
	        "GROUP BY p.v_id, p.video_type, s.video_name, s.v_img " +
	        "ORDER BY p.popularity DESC " +
	        "LIMIT 20" +
	        "</script>")
	List<Score> getTop20ByPopularityDesc();

	@Select("SELECT popularity FROM popularity WHERE v_id = #{vId} AND video_type = #{videoType}")
	Double getPopularityByVIdAndType(@Param("vId") int vId, @Param("videoType") String videoType);

	@Insert("INSERT INTO popularity (v_id, video_type, popularity, last_updated) VALUES (#{vId}, #{videoType}, #{popularity}, NOW())")
	int insertPopularity(@Param("vId") int vId, @Param("videoType") String videoType, @Param("popularity") Double popularity);

	@Update("UPDATE popularity SET popularity = #{popularity}, last_updated = NOW() WHERE v_id = #{vId} AND video_type = #{videoType}")
	int updatePopularity(@Param("vId") int vId, @Param("videoType") String videoType, @Param("popularity") Double popularity);

	long count(Score score);

	int insert(Score score);

	int insertReleaseYear(@Param("movieId") int movieId, @Param("releaseYear") int releaseYear);

	int insertGenres(@Param("movieId") int movieId, @Param("genres") List<Integer> genres);

	List<Integer> getCountryIdsByShortNames(@Param("shortNames") List<String> shortNames);

	int insertCountries(@Param("movieId") int movieId, @Param("countryIds") List<Integer> countryIds);

	int insertTvReleaseYear(@Param("tvId") int tvId, @Param("releaseYear") int releaseYear);

	int insertTvGenres(@Param("tvId") int movieId, @Param("genres") List<Integer> genres);

	List<Integer> getTvCountryIdsByShortNames(@Param("shortNames") List<String> shortNames);

	int insertTvCountries(@Param("tvId") int tvId, @Param("countryIds") List<Integer> countryIds);

	int insertBatch(@Param("entities") List<Score> entities);

	int insertOrUpdateBatch(@Param("entities") List<Score> entities);

	int update(Score score);

	int deleteById(Integer id);
}
