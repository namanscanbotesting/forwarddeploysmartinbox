package com.pharma.inbox.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Basic security configuration for prototype.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails reviewer = User.builder()
            .username("reviewer")
            .password(passwordEncoder.encode("review123"))
            .roles("REVIEWER")
            .build();
        
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN", "REVIEWER")
            .build();
            
        return new InMemoryUserDetailsManager(reviewer, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // For API testing
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/test/**").permitAll() // Allow test endpoints
                .requestMatchers("/api/classifications/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});
        
        return http.build();
    }
}
