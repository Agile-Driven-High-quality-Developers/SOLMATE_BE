package org.solmate.domain.auth.service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.jwt.JwtProvider;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.auth.dto.request.LoginRequest;
import org.solmate.domain.auth.dto.request.SignUpRequest;
import org.solmate.domain.auth.service.EmailVerificationService;
import org.solmate.domain.auth.dto.response.LoginResponse;
import org.solmate.domain.auth.entity.LoginType;
import org.solmate.domain.auth.enums.OAuthProvider;
import org.solmate.domain.auth.repository.LoginTypeRepository;
import org.solmate.domain.user.entity.User;
import org.solmate.domain.user.repository.UserRepository;
import org.solmate.domain.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final BigDecimal INITIAL_SEED_MONEY = new BigDecimal("10000000");

    private final UserService userService;
    private final UserRepository userRepository;
    private final LoginTypeRepository loginTypeRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final EmailVerificationService emailVerificationService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiration;

    @Value("${jwt.access-token-expiration}")
    private long accessExpiration;

    // 회원가입
    public void signUp(SignUpRequest request) {
        userService.checkEmailNotDuplicated(request.email());
        userService.checkNicknameNotDuplicated(request.nickname());
        emailVerificationService.isEmailVerified(request.email());

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();
        userRepository.save(user);

        LoginType loginType = LoginType.builder()
                .user(user)
                .loginType(OAuthProvider.EMAIL)
                .build();
        loginTypeRepository.save(loginType);

        Account account = Account.builder()
                .user(user)
                .cash(INITIAL_SEED_MONEY)
                .build();
        accountRepository.save(account);
    }

    // 로그인
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userService.getUserByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new GeneralException(ErrorStatus.INVALID_PASSWORD);
        }

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
                .secure(true)  // prod 환경에서는 true로 바꾸어 줄 예정!!
                .path("/")
                .maxAge(refreshExpiration / 1000)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new LoginResponse(user.getId(), user.getNickname(), accessToken, refreshToken);
    }

    // 토큰 재발급
    @Transactional(readOnly = true)
    public String reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String savedToken = redisTemplate.opsForValue().get("refresh:" + userId);

        if (!refreshToken.equals(savedToken)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        return jwtProvider.generateAccessToken(userId);
    }

    // 비밀번호 재설정
    public void resetPassword(String email, String newPassword) {
        emailVerificationService.isEmailVerified(email);
        User user = userService.getUserByEmail(email);
        userService.updatePassword(user, passwordEncoder.encode(newPassword));
    }

    // 로그아웃
    public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
        if (jwtProvider.validateToken(refreshToken)) {
            Long userId = jwtProvider.getUserId(refreshToken);
            redisTemplate.delete("refresh:" + userId);
        }

        if (jwtProvider.validateToken(accessToken)) {
            long remaining = jwtProvider.getRemainingExpiration(accessToken);
            redisTemplate.opsForValue().set(
                    "blacklist:" + accessToken,
                    "logout",
                    remaining,
                    TimeUnit.MILLISECONDS
            );
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}