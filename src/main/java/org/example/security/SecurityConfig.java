package org.example.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private TenantFilter tenantFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS Enable Karo
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 🔥 THIS FIXES IT
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/auth/signin", "/auth/signup", "/auth/user", "/auth/refresh").permitAll()
                        .requestMatchers("/index.html", "/signup.html", "/styles.css", "/script.js", "/").permitAll()
                        .requestMatchers("/welcome.html").permitAll()
                        .requestMatchers("/welcome/**").authenticated()
                        .requestMatchers("/projects/**").authenticated()
                        .requestMatchers("/groups/**").authenticated()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers("/api/issues/**").authenticated()
                        .requestMatchers("/comments/**").authenticated()
                        .requestMatchers("/notifications/**").authenticated()
                        .requestMatchers("/api/files/**").authenticated()
                        .requestMatchers("/ai/**").permitAll()
                        .requestMatchers("/health").permitAll()  
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class); // Multi-tenant context

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080","http://localhost:3000","http://localhost:3001","https://45d5-2405-201-5803-9887-ec33-f5d-bb3d-9d69.ngrok-free.app")); // Update frontend URL
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS")); // Allow all necessary methods
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Organization-Id", "X-Requested-With", "Accept", "Origin")); // Allow required headers
        configuration.setAllowCredentials(true); // Enable credentials
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all endpoints
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}