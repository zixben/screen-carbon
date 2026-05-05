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
                		"/", "/search-results", "/movies", "/movie", "/tv-shows", "/tv", "/details", "/rate", "/finish-rating"
                		, "about", "privacy-notice", "/user/getCode", "/signup", "/user/login", "/user-settings", "/user-ratings", "/user/save", "/user/check-username", "/user/check-email", "/user/delete", "/assets/**", "/css/**", "/js/**", "/plugins/**", "/table/**"
                		, "/score/**", "/user/password-recovery", "/reset-password**", "/user/update-password", "/update-password", "/user/**", "/admin", "/logout", "/1", "/2-2"
                		).permitAll() // Allow access to these endpoints without authentication
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
