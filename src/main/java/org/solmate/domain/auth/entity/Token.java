package org.solmate.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id", length = 255)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_type_id", nullable = false)
    private LoginType loginType;

    @Column(name = "provider_token")
    private String providerToken;

    @Builder
    public Token(LoginType loginType, String providerToken) {
        this.loginType = loginType;
        this.providerToken = providerToken;
    }
}
