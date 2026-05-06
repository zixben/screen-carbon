package com.lks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            //.csrf().disable() // Disable CSRF protection for simplicity, but consider enabling it in production
            //.csrf().and()
            .headers(headers -> headers
                .frameOptions(frameOptions -> 
                    frameOptions.sameOrigin() // Allow embedding only from the same origin
                )
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/", "/search-results", "/movies", "/movie", "/tv-shows", "/tv", "/details", "/rate",
                    "/finish-rating", "/about", "/privacy-notice", "/signup", "/login", "/reset-password**",
                    "/update-password", "/logout", "/admin", "/1", "/2-2", "/user-settings", "/user-ratings",
                    "/assets/**", "/css/**", "/js/**", "/plugins/**", "/table/**",
                    "/user/getCode", "/user/login", "/user/save", "/user/check-username", "/user/check-email",
                    "/user/password-recovery", "/user/update-password", "/user/delete",
                    "/user/all", "/user/update", "/user/selectByUser", "/user/countUsers",
                    "/score/add", "/score/getScoreList/**", "/score/getAvgFraction", "/score/getOrderAvg",
                    "/score/getMovieAvgDesc", "/score/getMovieAvgAsc", "/score/getTVAvgDesc", "/score/getTVAvgAsc",
                    "/score/getMovieScoreCountDesc", "/score/getMovieScoreCountAsc",
                    "/score/getTVScoreCountDesc", "/score/getTVScoreCountAsc", "/score/getScoreAvg/**",
                    "/score/getCountFraction", "/score/getTotalRated", "/score/getTop20Popularity"
                ).permitAll()
                .anyRequest().authenticated() // Require authentication for any other requests
            )
            .formLogin(form -> form
                .loginPage("/login") // Specify custom login page URL
                .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
                )
            .sessionManagement(session -> session
                    .sessionFixation().migrateSession() // Ensures session fixation protection
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
