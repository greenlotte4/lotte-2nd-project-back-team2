package com.backend.util.jwt;

import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class JwtTokenProvider {

    @Value("${token.secret.key}")
    private String secret;

    @Value("${token.secret.issuer}")
    private String issuer;
    // 사용자 이름

    public String createToken(String username, String role, Long id, String company, String type) {
        Date now = new Date();  // 현재 시간
        Date expireDate = null;

        if ("access".equals(type)) {
            expireDate = new Date(now.getTime() + 1000 * 60 * 60);  // 만료 시간 60분
            return Jwts.builder()
                    .setIssuer(issuer)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expireDate)
                    .claim("id", id)
                    .claim("role", role)
                    .claim("name",username )
                    .claim("company", company)
                    .signWith(SignatureAlgorithm.HS256, getSigningKey())
                    .compact();
        }else{
            expireDate = new Date(now.getTime() + Duration.ofDays(7).toMillis());
            return Jwts.builder()
                    .setIssuer(issuer)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expireDate)
                    .claim("id", id)
                    .claim("role", role)
                    .claim("name",username )
                    .claim("company", company)
                    .signWith(SignatureAlgorithm.HS256, getSigningKey())
                    .compact();
        }
    }

    public byte[] getSigningKey() {
        try {
            return secret.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("서명 키를 가져오는 중 오류 발생", e);
            throw e;
        }
    }

    public Claims getClaims(String token) {
        log.info("getClaims 메서드 호출, 토큰: {}", token);
        try {
            // JWT 파싱 및 Claims 추출
            return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException e) {
            log.error("토큰이 만료되었습니다: {}", token, e);  // 만료된 토큰 로그
            throw e; // 예외를 던져서 필터에서 처리
        } catch (MalformedJwtException e) {
            log.error("잘못된 JWT 형식입니다: {}", token, e);  // 잘못된 형식 로그
            throw e;  // 예외를 던져서 필터에서 처리
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("JWT 서명이 올바르지 않습니다: {}", token, e);  // 서명 오류 로그
            throw e;  // 예외를 던져서 필터에서 처리
        } catch (Exception e) {
            log.error("JWT 처리 중 알 수 없는 오류 발생: {}", token, e);  // 기타 오류 로그
            throw e;  // 예외를 던져서 필터에서 처리
        }
    }


    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getSigningKey()) // getSigningKey 재사용
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
//            Jwts.parserBuilder()
//                    .setSigningKey(getSigningKey())
//                    .build()
//                    .parseClaimsJws(token);
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }



}
