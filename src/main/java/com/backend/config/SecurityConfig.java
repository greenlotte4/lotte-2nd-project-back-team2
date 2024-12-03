package com.backend.config;

import com.backend.dto.request.AuthenticateDto;
import com.backend.util.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .csrf(CsrfConfigurer::disable)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

//                        .requestMatchers("/api/auth/**").permitAll()
//                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    protected CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(List.of("http://localhost:8010", "http://13.124.94.213:90"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.addAllowedHeader("Authorization");
        corsConfiguration.addAllowedHeader("Content-Type");
        corsConfiguration.addAllowedHeader("Accept");
        corsConfiguration.addAllowedHeader("X-Requested-With");
        corsConfiguration.addAllowedHeader("Cache-Control");
        corsConfiguration.addAllowedHeader("X-Custom-Header");
        corsConfiguration.setAllowCredentials(true); // 쿠키 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @RequiredArgsConstructor
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtTokenProvider jwtTokenProvider;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String token = extractTokenFromHeader(request);
            System.out.println("===123123=");

            log.info("헤더에서 토큰을 잘 뽑는디 확인 "+token);
            System.out.println(token);
            try {
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    if (jwtTokenProvider.isTokenExpired(token)) {
                        throw new IllegalArgumentException("Token has expired");
                    }
                    AuthenticateDto auth = authenticateWithToken(token);
                    request.setAttribute("uid",auth.getUid());
                    request.setAttribute("role",auth.getRole());
                }else {
                    log.info("토큰 없따");
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext(); // Clear context on failure
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            filterChain.doFilter(request, response);
        }

        private String extractTokenFromHeader(HttpServletRequest request) {
            String header = request.getHeader("Authorization");
            return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        }

        private AuthenticateDto authenticateWithToken(String token) {
            Claims claims = jwtTokenProvider.getClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            log.info("토큰에서 추출한 사용자명: {}", username);
            log.info("토큰에서 추출한 역할: {}", role);

            if (username == null || role == null) {
                log.error("사용자명 또는 역할이 null입니다. 토큰: {}", token);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority(role)));

            log.info("Authentication 객체 생성 완료: {}", authentication);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            AuthenticateDto auth = AuthenticateDto.builder()
                    .uid(username)
                    .role(role)
                    .build();

            return auth;
        }
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
