package com.backend.util;

import com.backend.entity.user.SocialAccount;
import com.backend.repository.UserRepository;
import com.backend.repository.user.SocialAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class MyOauth2UserService extends DefaultOAuth2UserService {

    private final HttpServletRequest request;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("Load user... 1 : "+userRequest);

        String accessToken = userRequest.getAccessToken().getTokenValue();
        log.info("Load user... 2 (access token) : " + accessToken);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("Load user... 3 (provider) : " + provider);

        // 소셜 로그인 유저 정보 로딩
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("Load user... 4 (OAuth2User) : " + oAuth2User);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.info("Load user... 5 (attributes) : " + attributes);

        String providerId = null;
        String identifierKey = null;

        if(provider.equals("google")){
            providerId = (String) attributes.get("sub");
            identifierKey = "sub";
            log.info(providerId + " 구글 : " + provider);

        } else if(provider.equals("naver")){
            attributes = (Map<String, Object>) attributes.get("response");
            providerId = (String) attributes.get("id");
            identifierKey = "id";
            log.info(providerId + " 네이버 : " + provider);

        } else if(provider.equals("kakao")) {
            providerId = String.valueOf(attributes.get("id"));
            identifierKey = "id";
            log.info(providerId + " 카카오 : " + provider);
        }

        SocialAccount socialAccount = socialAccountRepository.findByProviderId(providerId).orElse(null);
        if(socialAccount != null){
            return new DefaultOAuth2User(
                    oAuth2User.getAuthorities(),
                    attributes,
                    identifierKey
            );
        }else{
            socialAccount = SocialAccount.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .build();
            socialAccountRepository.save(socialAccount);

            return new DefaultOAuth2User(
                    oAuth2User.getAuthorities(),
                    attributes,
                    identifierKey
            );
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}