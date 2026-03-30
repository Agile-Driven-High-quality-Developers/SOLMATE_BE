package org.solmate.domain.auth.service;

import java.util.concurrent.TimeUnit;

import org.solmate.common.jwt.JwtProvider;
import org.solmate.domain.auth.dto.response.GoogleInfoResponse;
import org.solmate.domain.auth.dto.response.GoogleTokenResponse;
import org.solmate.domain.auth.dto.response.LoginResponse;
import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.auth.entity.LoginType;
import org.solmate.domain.auth.entity.Token;
import org.solmate.domain.auth.enums.OAuthProvider;
import org.solmate.domain.auth.repository.LoginTypeRepository;
import org.solmate.domain.auth.repository.TokenRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleService {

    private final UserRepository userRepository;
    private final LoginTypeRepository loginTypeRepository;
    private final TokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    private final RestClient restClient = RestClient.create();

    @Value("${social.google.client-id}")
    private String clientId;

    @Value("${social.google.client-secret}")
    private String clientSecret;

    @Value("${social.google.redirect-uri}")
    private String redirectUri;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiration;

    // 구글 인가 URL 생성
    public String getGoogleAuthorizeUri() {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .build()
                .toUriString();
    }


    public LoginResponse loginWithGoogle(String code, HttpServletResponse response) {
        GoogleTokenResponse googleToken = getGoogleToken(code);
        GoogleInfoResponse googleInfo = getGoogleUserInfo(googleToken.accessToken());

        User user = findOrCreateUser(googleInfo, googleToken.accessToken());

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refreshToken,
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshExpiration / 1000)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new LoginResponse(user.getId(), user.getNickname(), accessToken, refreshToken);
    }


    private GoogleTokenResponse getGoogleToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        return restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }


    private GoogleInfoResponse getGoogleUserInfo(String accessToken) {
        return restClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleInfoResponse.class);
    }


    private User findOrCreateUser(GoogleInfoResponse googleInfo, String providerToken) {
        return userRepository.findByEmail(googleInfo.email())
                .map(user -> {
                    if (!loginTypeRepository.existsByUserAndLoginType(user, OAuthProvider.GOOGLE)) {
                        throw new GeneralException(ErrorStatus.EMAIL_REGISTERED_WITH_EMAIL);
                    }
                    return user;
                })
                .orElseGet(() -> createGoogleUser(googleInfo, providerToken));
    }

    private User createGoogleUser(GoogleInfoResponse googleInfo, String providerToken) {
        User user = User.builder()
                .email(googleInfo.email())
                .nickname(googleInfo.name())
                .imageUrl(googleInfo.imageUrl())
                .build();
        userRepository.save(user);

        LoginType loginType = LoginType.builder()
                .user(user)
                .loginType(OAuthProvider.GOOGLE)
                .build();
        loginTypeRepository.save(loginType);

        Token token = Token.builder()
                .loginType(loginType)
                .providerToken(providerToken)
                .build();
        tokenRepository.save(token);

        return user;
    }
}