package org.solmate.domain.stock.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@Table(
	name = "daily_candle",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_daily_candle_stock_code_candle_time",
			columnNames = {"stock_code", "candle_time"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DailyCandle extends BaseCandle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "candle_time", nullable = false)
	private LocalDateTime candleTime;
}