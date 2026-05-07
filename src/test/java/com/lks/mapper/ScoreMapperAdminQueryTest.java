package com.lks.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreMapperAdminQueryTest {

    @Test
    void totalRatedUsesStableVideoIdentityNotTitleOnly() throws Exception {
        String query = selectQuery("getTotalRatedMovies");

        assertTrue(query.contains("GROUP BY v_id, video_type"));
        assertFalse(query.contains("DISTINCT video_name"));
    }

    @Test
    void adminAverageAndPopularityQueriesGroupByVideoIdAndType() throws Exception {
        for (String methodName : new String[] { "getAverageFractionX", "getAverageFractionY", "getCountX", "getCountY" }) {
            String query = selectQuery(methodName);

            assertTrue(query.contains("GROUP BY v_id, video_type, video_name"),
                    methodName + " must group by stable video identity");
        }
    }

    @Test
    void averageRatingQueriesIncludeAllRatedMedia() throws Exception {
        assertFalse(selectQuery("getAverageFractionX").contains("LIMIT"));
        assertFalse(selectQuery("getAverageFractionY").contains("LIMIT"));
    }

    @Test
    void popularityQueriesRemainTopTen() throws Exception {
        assertTrue(selectQuery("getCountX").contains("LIMIT 10"));
        assertTrue(selectQuery("getCountY").contains("LIMIT 10"));
    }

    private String selectQuery(String methodName) throws Exception {
        Method method = ScoreMapper.class.getMethod(methodName);
        return String.join(" ", method.getAnnotation(Select.class).value());
    }
}
