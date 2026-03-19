package org.solmate.external.ls.service;

import java.util.concurrent.TimeUnit;

import org.solmate.external.ls.LsProperties;

import org.solmate.external.ls.dto.response.LsTokenResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LsTokenService {

    private static final String REDIS_TOKEN_KEY = "ls:access_token";
    private static final long TOKEN_TTL_HOURS = 23;

    private final LsProperties lsProperties;
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient = RestClient.create();

    public String getToken() {
        String cached = redisTemplate.opsForValue().get(REDIS_TOKEN_KEY);
        if (cached != null) {
            return cached;
        }
        return issueToken();
    }

    private String issueToken() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("appkey", lsProperties.getAppKey());
        params.add("appsecretkey", lsProperties.getAppSecret());
        params.add("scope", "oob");

        LsTokenResponse response = restClient.post()
                .uri(lsProperties.getBaseUrl() + "/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(LsTokenResponse.class);

        redisTemplate.opsForValue().set(
                REDIS_TOKEN_KEY,
                response.accessToken(),
                TOKEN_TTL_HOURS,
                TimeUnit.HOURS
        );

        log.info("LS 토큰 발급 완료");
        return response.accessToken();
    }
}
