package com.doctorappointment.config;

import com.doctorappointment.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmail(
                        email.trim().toLowerCase(Locale.ROOT))
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails
                        .UsernameNotFoundException("Invalid email or password"));
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository =
                new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-origin:http://localhost:5173}") String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> originPatterns = new java.util.ArrayList<>();
        if (frontendOrigin != null && !frontendOrigin.isBlank()) {
            for (String origin : frontendOrigin.split(",")) {
                String trimmed = origin.trim();
                while (trimmed.endsWith("/")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                if (!trimmed.isEmpty()) {
                    originPatterns.add(trimmed);
                }
            }
        }
        if (!originPatterns.contains("http://localhost:5173")) {
            originPatterns.add("http://localhost:5173");
        }
        originPatterns.add("https://*.vercel.app");

        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Content-Type", "X-XSRF-TOKEN", "Accept", "Authorization", "X-Requested-With", "Origin"));
        configuration.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/send-reset-code", "/api/auth/reset-password", "/api/auth/register-admin")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/patients")
                        .permitAll()
                        .requestMatchers("/api/doctors/me")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/doctors", "/api/doctors/*",
                                "/api/specializations",
                                "/api/specializations/*",
                                "/api/availabilities/doctor/*")
                        .permitAll()
                        .requestMatchers("/api/admin/**", "/api/users/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/patients")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/*")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/patients/*/credentials")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/patients/me")
                        .hasRole("PATIENT")
                        .requestMatchers("/api/patients/**")
                        .hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/doctors")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/doctors/*/credentials")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/doctors/*")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/doctors/*")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/doctors/me")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/doctors/**")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/appointments/*/status")
                        .hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/appointments/*")
                        .hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/appointments/*")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/appointments/status/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/appointments/**")
                        .hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/availabilities")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/availabilities/doctor/*")
                        .permitAll()
                        .requestMatchers("/api/availabilities/**")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/specializations",
                                "/api/specializations/*")
                        .permitAll()
                        .requestMatchers("/api/specializations/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/prescriptions/**")
                        .hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers("/api/notifications/**")
                        .authenticated()
                        .requestMatchers("/api/favorites/**")
                        .hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/doctor/*")
                        .permitAll()
                        .requestMatchers("/api/reviews/**")
                        .hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers("/api/admin/analytics")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(401)));

        return http.build();
    }
}
