package com.renode.repository;

import com.renode.entity.TradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeRequestRepository extends JpaRepository<TradeRequest, Integer> {
    List<TradeRequest> findByReceiverId(Integer receiverId);
    List<TradeRequest> findBySenderId(Integer senderId);
}