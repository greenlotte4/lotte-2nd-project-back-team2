package com.backend.controller;

import com.backend.dto.request.LoginDto;
import com.backend.dto.response.UserDto;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.util.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8010")
@RequiredArgsConstructor
@Log4j2
public class AuthController {
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //2024/11/29 박연화 토큰 발급 수정
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginDto dto
            , HttpServletResponse resp
    ){
        Optional<User> user = userRepository.findByUid(dto.getUid());
        if(user.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        if(passwordEncoder.matches(dto.getPwd(), user.get().getPwd())){

            UserDto userDto = user.get().toSliceDto();
            log.info("로그인 컨트롤러!!!!:" +userDto);

            String accessToken = tokenProvider.createToken(dto.getUid(),user.get().getRole().toString(), "access");
            String refreshToken = tokenProvider.createToken(dto.getUid(),user.get().getRole().toString(), "refresh");

            //쿠키에 저장해라
            Cookie cookie = new Cookie("refresh_token", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60*60*24*7);
            resp.addCookie(cookie);

            //리프레시토큰 redis 저장


            Map<String, Object> response = new HashMap<>();
            response.put("token",accessToken);
            response.put("role",user.get().getRole());
            response.put("user",user.get().toSliceDto());
            return ResponseEntity.ok().body(response);
        }

        return ResponseEntity.ok().build();
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
}
