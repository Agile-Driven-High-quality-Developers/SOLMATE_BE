package org.solmate.domain.account.entity;

import java.math.BigDecimal;

import org.solmate.common.base.BaseEntity;
import org.solmate.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cash;

    @Builder
    public Account(User user, BigDecimal cash) {
        this.user = user;
        this.cash = cash;
    }

    public void addCash(BigDecimal amount) {
        this.cash = this.cash.add(amount);
    }

    public void subtractCash(BigDecimal amount) {
        this.cash = this.cash.subtract(amount);
    }
}
