package org.solmate.domain.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public class BaseCandle {
	@Column(nullable = false, length = 12)
	protected String stockCode;

	@Column(nullable = false)
	protected Long openPrice;

	@Column(nullable = false)
	protected Long highPrice;

	@Column(nullable = false)
	protected Long lowPrice;

	@Column(nullable = false)
	protected Long closePrice;

	@Column(nullable = false)
	protected Long volume;

	@Column(nullable = true)
	protected Long tradingValue;
}
