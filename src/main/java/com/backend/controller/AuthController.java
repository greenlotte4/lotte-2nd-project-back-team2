package com.backend.controller;

import com.backend.dto.request.LoginDto;
import com.backend.dto.request.user.ReqSocialLinkDTO;
import com.backend.dto.response.UserDto;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.service.UserService;
import com.backend.util.MyUserDetails;
import com.backend.util.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8010", allowCredentials = "true")
@RequiredArgsConstructor
@Log4j2
public class AuthController {
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate redisTemplate;
    private final UserService userService;

    @GetMapping("/test")
    public ResponseEntity<?> testapi (){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    //2024/11/29 박연화 토큰 발급 수정
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(
            @RequestBody LoginDto dto
            , HttpServletResponse resp
    ){
        Optional<User> user = userRepository.findByUid(dto.getUid());
        if(user.isEmpty()){
            return ResponseEntity.notFound().build();
        }else if(user.get().getStatus()==0){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(passwordEncoder.matches(dto.getPwd(), user.get().getPwd())){

            UserDto userDto = user.get().toSliceDto();
            log.info("로그인 컨트롤러!!!!:" +userDto);
            String company = (!userDto.getCompany().isEmpty() || userDto.getCompany() != null) ? userDto.getCompany() : "PersonalUser";
            String accessToken = tokenProvider.createToken(userDto.getUid(),userDto.getRole().toString(),userDto.getId(),company, "access");
            String refreshToken = tokenProvider.createToken(userDto.getUid(),userDto.getRole().toString(),userDto.getId(),company, "refresh");

            log.info("리프레시 토큰 발급 확인 " +refreshToken);
            //쿠키에 저장해라
            Cookie cookie = new Cookie("refresh_token", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60*60*24*7);
            resp.addCookie(cookie);

            long groupId = userService.findGroupByUserUid(userDto.getUid());
            UserDto userDto1 = user.get().toSliceDto();
            if(groupId >0){
                userDto1.setGroupId(groupId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("token",accessToken);
//            response.put("access_token", accessToken); // Access 토큰
            response.put("refresh_token", refreshToken); // Refresh 토큰
            response.put("role",user.get().getRole());
            response.put("user",userDto1);
            response.put("company",company);

            user.get().updateLoginDate(LocalDateTime.now());
            return ResponseEntity.ok().body(response);
        }else{
            return ResponseEntity.status(HttpStatus.CONFLICT).body("비밀번호 안 맞음");
        }
    }

    // Redis에 저장
    public void storeRefreshTokenInRedis(String refreshToken, String userId, String role, long expirationTime) {
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("role", role);

        // 저장 및 만료 시간 설정
        redisTemplate.opsForHash().putAll("refreshToken:" + refreshToken, sessionData);
        redisTemplate.expire("refreshToken:" + refreshToken, Duration.ofMillis(expirationTime));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {

        log.info("로그아웃 컨트롤러 ");
        // 쿠키 무효화
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/"); // 쿠키 경로 설정
        cookie.setMaxAge(0); // 만료 시간 0으로 설정 -> 즉시 삭제
        response.addCookie(cookie);

        // (옵션) 리프레시 토큰을 데이터베이스 또는 캐시에서 삭제
        // tokenService.deleteRefreshToken(userId);

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        System.out.println(refreshToken);
        log.info("리프레시 액세스토큰 컨트롤러 접속... " + refreshToken);
        if (refreshToken == null) {
            log.info("리프레시 토큰 널");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token provided");
        }
        try {
            Claims claims = tokenProvider.getClaims(refreshToken);
            log.info("클레임 : "+claims);
            if (claims == null || tokenProvider.isTokenExpired(refreshToken)) {
                log.info("여긴가?1");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
            }

            // 새 액세스 토큰 생성
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            Long id = claims.get("id",Long.class);
            String company = claims.get("company",String.class);
            String newAccessToken = tokenProvider.createToken(username, role, id, company,"access");
                log.info("여긴가?2"+newAccessToken);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @GetMapping("/social")
    public ResponseEntity<?> socialLogin () {
//        Map<String, Object> attributes = oauth2User.getAttributes();
//        log.info("소셜 정보 처리 후 이쪽으로 들어오는지 확인 "+attributes);
        return ResponseEntity.ok().body("소셜 로그인");
    }

    @PostMapping("/link")
    public ResponseEntity<?> linkSocialAccount(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestBody ReqSocialLinkDTO request
    ) {

        log.info("링크소셜 컨트롤러 ");
        // 1. 현재 인증된 사용자 ID 확인
//        Long userId = Long.parseLong(userDetails.getName());

        // 2. 소셜 계정 연동 로직
//        socialAccountService.linkSocialAccount(userId, request);

        return ResponseEntity.ok().build();
    }
}
