package com.mvc.nms_be.config;

import com.mvc.nms_be.security.CustomAccessDeniedHandler;
import com.mvc.nms_be.security.CustomUserDetailsService;
import com.mvc.nms_be.security.JwtAuthenticationEntryPoint;
import com.mvc.nms_be.security.JwtAuthenticationFilter;
import com.mvc.nms_be.security.JwtTokenProvider;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomUserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
            JwtTokenProvider tokens, CustomUserDetailsService users, JwtAuthenticationEntryPoint entryPoint,
            CustomAccessDeniedHandler accessDenied, UrlBasedCorsConfigurationSource corsSource) throws Exception {
        // Construct here so the JWT filter is registered only in Spring Security's chain.
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokens, users, entryPoint);
        return http.cors(cors -> cors.configurationSource(corsSource))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .authenticationManager(authenticationManager)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // Swagger UI và tài liệu OpenAPI mở công khai để demo; API bên trong vẫn cần JWT.
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                            .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**")
                            .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/categories", "/api/v1/categories/**").hasRole("ADMIN")
                        // Cấu hình hệ thống: Admin/Staff được xem, chỉ Admin được thêm/sửa/xóa.
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings", "/api/v1/settings/**")
                            .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/settings", "/api/v1/settings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/news", "/api/v1/news/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/news", "/api/v1/news/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/news", "/api/v1/news/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/news", "/api/v1/news/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications", "/api/v1/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/notifications", "/api/v1/notifications/**")
                            .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/tags/**", "/api/v1/news-tags", "/api/v1/news-tags/**")
                            .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/tags", "/api/v1/tags/**", "/api/v1/news-tags", "/api/v1/news-tags/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
