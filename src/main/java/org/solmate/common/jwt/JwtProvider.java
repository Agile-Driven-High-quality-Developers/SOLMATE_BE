package org.solmate.common.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;


@Component
public class JwtProvider {

	private final SecretKey key;
	private final long accessExpiration;
	private final long refreshExpiration;

	public JwtProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-expiration}") long accessExpiration,
		@Value("${jwt.refresh-token-expiration}") long refreshExpiration
	) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.accessExpiration = accessExpiration;
		this.refreshExpiration = refreshExpiration;
	}

	// access token 생성 (JWT)
	public String generateAccessToken(Long userId) {
		Date now = new Date();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.issuedAt(now)
			.expiration(new Date(now.getTime() + accessExpiration))
			.signWith(key)
			.compact();
	}

	public String generateRefreshToken(Long userId) {
		Date now = new Date();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.issuedAt(now)
			.expiration(new Date(now.getTime() + refreshExpiration))
			.signWith(key)
			.compact();
	}


	// token 유효성 검증
	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// token에서 userId 추출
	public Long getUserId(String token) {
		return Long.parseLong(
			Jwts.parser().verifyWith(key).build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject()
		);
	}

	// token 남은 만료시간 (ms)
	public long getRemainingExpiration(String token) {
		Date expiration = Jwts.parser().verifyWith(key).build()
				.parseSignedClaims(token)
				.getPayload()
				.getExpiration();
		return expiration.getTime() - new Date().getTime();
	}
}