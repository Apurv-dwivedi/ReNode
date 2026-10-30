package com.renode.repository;

import com.renode.entity.TradeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeMessageRepository extends JpaRepository<TradeMessage, Integer> {
    List<TradeMessage> findByTradeRequestId(Integer tradeRequestId);
}