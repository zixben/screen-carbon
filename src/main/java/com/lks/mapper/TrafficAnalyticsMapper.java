package com.lks.mapper;

import com.lks.dto.TrafficGeoStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TrafficAnalyticsMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS traffic_geo_daily (
                traffic_date DATE NOT NULL,
                country_code CHAR(2) NOT NULL,
                region VARCHAR(80) NOT NULL,
                request_count BIGINT NOT NULL DEFAULT 0,
                last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (traffic_date, country_code, region)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """)
    void ensureTrafficGeoDailyTable();

    @Insert("""
            INSERT INTO traffic_geo_daily (traffic_date, country_code, region, request_count, last_seen)
            VALUES (CURRENT_DATE, #{countryCode}, #{region}, 1, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                request_count = request_count + 1,
                last_seen = CURRENT_TIMESTAMP
            """)
    int incrementDailyTraffic(@Param("countryCode") String countryCode, @Param("region") String region);

    @Select("""
            SELECT country_code AS countryCode,
                   region,
                   SUM(request_count) AS requestCount
            FROM traffic_geo_daily
            WHERE traffic_date >= #{startDate}
            GROUP BY country_code, region
            ORDER BY requestCount DESC, country_code ASC, region ASC
            LIMIT #{limit}
            """)
    List<TrafficGeoStat> findGeoTrafficSince(@Param("startDate") String startDate, @Param("limit") int limit);
}
