package org.solmate.domain.returnrate.entity;

import java.math.BigDecimal;

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
@Table(name = "total_return_rate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TotalReturnRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "key_id", length = 255)
    private String keyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_return_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalReturnRate;

    @Builder
    public TotalReturnRate(User user, BigDecimal totalReturnRate) {
        this.user = user;
        this.totalReturnRate = totalReturnRate;
    }

    public void updateReturnRate(BigDecimal totalReturnRate) {
        this.totalReturnRate = totalReturnRate;
    }
}
