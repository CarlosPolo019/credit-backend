package com.fya.credits.config;

import com.fya.credits.security.JwtAuthenticationFilter;
import com.fya.credits.security.RateLimitFilter;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RateLimitFilter rateLimitFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
            .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/api/v1/email-jobs/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/v1/users").hasRole("ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint((request, response, ex) -> {
              response.setStatus(401);
              response.setContentType("application/json");
              response.getWriter().write("""
                  {"status":401,"code":"UNAUTHORIZED","message":"Authentication is required"}
                  """);
            })
            .accessDeniedHandler((request, response, ex) -> {
              response.setStatus(403);
              response.setContentType("application/json");
              response.getWriter().write("""
                  {"status":403,"code":"FORBIDDEN","message":"No tienes permiso para acceder a este recurso"}
                  """);
            }))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
