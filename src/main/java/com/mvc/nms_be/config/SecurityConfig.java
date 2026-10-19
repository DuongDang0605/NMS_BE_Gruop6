package com.mvc.nms_be.config;

import com.mvc.nms_be.security.CustomAccessDeniedHandler;
import com.mvc.nms_be.security.CustomUserDetails;
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
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletRequest;

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
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**")
                            .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/categories", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/news", "/api/news/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/news", "/api/news/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/news", "/api/news/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/news", "/api/news/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/notifications", "/api/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications", "/api/notifications/**")
                            .access((authentication, context) -> new AuthorizationDecision(
                                    canAccessRecipient(authentication.get(), context.getRequest())))
                        .requestMatchers(HttpMethod.GET, "/api/tags", "/api/tags/**", "/api/news-tags", "/api/news-tags/**")
                            .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/tags", "/api/tags/**", "/api/news-tags", "/api/news-tags/**").hasRole("ADMIN")
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

    private static boolean canAccessRecipient(Authentication authentication, HttpServletRequest request) {
        if (!authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            return false;
        }
        if (user.getAccountRole() == 1) {
            return true;
        }
        String recipientId = request.getParameter("recipientId");
        if (recipientId == null) {
            return true; // The controller reports its required parameter as HTTP 400.
        }
        try {
            return user.getAccountId().equals(Integer.valueOf(recipientId));
        } catch (NumberFormatException ex) {
            return true; // Let MVC report an invalid parameter type instead of exposing data.
        }
    }
}
