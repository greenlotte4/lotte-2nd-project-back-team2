package com.backend.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${token.secret.key}")
    private String secret;

    @Value("${token.secret.issuer}")
    private String issuer;
    // 사용자 이름
    private Date now = new Date();  // 현재 시간

    public String createToken(String username, String role, String type) {
        Date expireDate = null;

        if (type == "access") {
            expireDate = new Date(now.getTime() + 1000 * 60 * 1);  // 만료 시간 60분
            return Jwts.builder()
                    .setIssuer(issuer)
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expireDate)
                    .claim("role", role)
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();
        }else{
            expireDate = new Date(now.getTime() + Duration.ofDays(7).toMillis());
            return Jwts.builder()
                    .setExpiration(expireDate)
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();

        // "role" claim에서 역할 정보 추출
        return claims.get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
