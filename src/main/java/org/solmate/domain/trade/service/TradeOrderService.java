package org.solmate.domain.trade.service;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.solmate.domain.account.entity.Account;
import org.solmate.domain.account.repository.AccountRepository;
import org.solmate.domain.trade.entity.Holdings;
import org.solmate.domain.trade.entity.TradeHistory;
import org.solmate.domain.trade.enums.TradeStatus;
import org.solmate.domain.trade.enums.TradeType;
import org.solmate.domain.trade.repository.HoldingsRepository;
import org.solmate.domain.trade.repository.TradeDiaryRepository;
import org.solmate.domain.trade.repository.TradeHistoryRepository;
import org.solmate.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeOrderService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final TradeDiaryRepository tradeDiaryRepository;
    private final AccountRepository accountRepository;
    private final HoldingsRepository holdingsRepository;

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        // DB 비관적 락(SELECT FOR UPDATE)으로 TradeHistory row를 잠금
        // → 동일 주문에 동시 취소 요청이 들어와도 하나씩 순서대로 처리됨 (중복 취소 방지)
        TradeHistory tradeHistory = tradeHistoryRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        // 본인 주문인지 검증
        if (!tradeHistory.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        // PENDING 상태인 주문만 취소 가능
        // → 이미 체결(FILLED)되거나 취소(CANCELLED)된 주문은 거절
        if (tradeHistory.getTradeStatus() != TradeStatus.PENDING) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        User user = tradeHistory.getUser();

        if (tradeHistory.getTradeType() == TradeType.BUY) {
            // 매수 취소 → 주문 접수 시 선차감했던 현금 복원
            // DB 비관적 락으로 Account row를 잠금 → 동시 복원 시 잔액 덮어쓰기 방지
            Account account = accountRepository.findByUserWithLock(user)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.ACCOUNT_NOT_FOUND));
            account.addCash(tradeHistory.getPrice().multiply(tradeHistory.getQuantity()));
        } else {
            // 매도 취소 → 주문 접수 시 선차감했던 주식 수량 복원
            // DB 비관적 락으로 Holdings row를 잠금 → 동시 복원 시 수량 덮어쓰기 방지
            Holdings holdings = holdingsRepository.findByUserAndTickerCodeWithLock(user, tradeHistory.getStock().getTickerCode())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));
            holdings.addQuantity(tradeHistory.getQuantity());
        }

        // 주문 상태를 CANCELLED로 변경
        tradeHistory.updateStatus(TradeStatus.CANCELLED);

        // 해당 주문에 연결된 매매일지 삭제
        tradeDiaryRepository.deleteByTradeHistoryId(orderId);
    }
}
