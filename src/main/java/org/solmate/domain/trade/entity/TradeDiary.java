package org.solmate.domain.trade.entity;

import org.solmate.common.base.BaseEntity;
import org.solmate.domain.trade.enums.TradeDiaryStatus;
import org.solmate.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade_diary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeDiary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_history_id", nullable = false)
    private TradeHistory tradeHistory;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeDiaryStatus status;

    public void updateStatus(TradeDiaryStatus status) {
        this.status = status;
    }

    @Builder
    public TradeDiary(User user, TradeHistory tradeHistory, String content, TradeDiaryStatus status) {
        this.user = user;
        this.tradeHistory = tradeHistory;
        this.content = content;
        this.status = status;
    }
}
