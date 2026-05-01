package com.election.evm.config;

import com.election.evm.security.JwtAuthenticationFilter;
import com.election.evm.security.GoogleOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleOAuth2SuccessHandler = googleOAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/login/").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/register/").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/otp/send", "/api/auth/otp/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/election-results").hasAnyRole("ADMIN", "CITIZEN", "OBSERVER", "ANALYST")
                        .requestMatchers("/api/users/**", "/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/incidents/**").hasAnyRole("ADMIN", "OBSERVER")
                        .requestMatchers(HttpMethod.GET, "/api/fraud-reports").hasAnyRole("ADMIN", "CITIZEN", "OBSERVER")
                        .requestMatchers(HttpMethod.POST, "/api/fraud-reports").hasAnyRole("ADMIN", "CITIZEN")
                        .requestMatchers(HttpMethod.PUT, "/api/fraud-reports/**").hasAnyRole("ADMIN", "CITIZEN")
                        .requestMatchers(HttpMethod.DELETE, "/api/fraud-reports/**").hasAnyRole("ADMIN", "CITIZEN")
                        .requestMatchers("/api/analyst-reports/**").hasAnyRole("ADMIN", "ANALYST", "OBSERVER")
                        .requestMatchers(HttpMethod.POST, "/api/election-results").hasAnyRole("ADMIN", "ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/election-results/bulk-upload").hasRole("ANALYST")
                        .requestMatchers(HttpMethod.PUT, "/api/election-results/**").hasAnyRole("ADMIN", "ANALYST")
                        .requestMatchers(HttpMethod.DELETE, "/api/election-results/**").hasAnyRole("ADMIN", "ANALYST")
                        .anyRequest().authenticated()
                )
                    .oauth2Login(oauth2 -> oauth2.successHandler(googleOAuth2SuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
