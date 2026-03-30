package org.solmate.domain.watchlist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.solmate.common.base.BaseEntity;
import org.solmate.domain.stock.entity.Stock;
import org.solmate.domain.user.entity.User;

@Entity
@Table(name = "watch_list",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stock_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Builder
    public Watchlist(User user, Stock stock) {
        this.user = user;
        this.stock = stock;
    }
}
