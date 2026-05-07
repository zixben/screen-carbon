package com.lks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .addFilterBefore(sessionAuthenticationFilter(), AnonymousAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(frameOptions -> 
                    frameOptions.sameOrigin() // Allow embedding only from the same origin
                )
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/admin", "/1", "/2-2",
                    "/user/all", "/user/update", "/user/selectByUser", "/user/countUsers",
                    "/score/update", "/score/delete", "/score/getAvgFraction", "/score/getCountFraction",
                    "/score/getTotalRated"
                ).hasRole("ADMIN")
                .requestMatchers(
                    "/user-settings", "/user-ratings", "/user/delete", "/score/getScoreList/**"
                ).authenticated()
                .requestMatchers(
                    "/", "/search-results", "/movies", "/movie", "/tv-shows", "/tv", "/details", "/rate",
                    "/finish-rating", "/about", "/privacy-notice", "/signup", "/login", "/reset-password**",
                    "/update-password", "/logout",
                    "/assets/**", "/css/**", "/js/**", "/plugins/**", "/table/**",
                    "/tmdb/**",
                    "/user/me", "/user/getCode", "/user/login", "/user/save", "/user/check-username", "/user/check-email",
                    "/user/password-recovery", "/user/update-password",
                    "/score/add", "/score/last-submission", "/score/getOrderAvg",
                    "/score/getMovieAvgDesc", "/score/getMovieAvgAsc", "/score/getTVAvgDesc", "/score/getTVAvgAsc",
                    "/score/getMovieScoreCountDesc", "/score/getMovieScoreCountAsc",
                    "/score/getTVScoreCountDesc", "/score/getTVScoreCountAsc", "/score/getScoreAvg/**",
                    "/score/getTop20Popularity"
                ).permitAll()
                .anyRequest().denyAll()
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

    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter() {
        return new SessionAuthenticationFilter();
    }
}
