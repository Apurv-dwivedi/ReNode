package com.renode.repository;

import com.renode.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Integer> {
    List<NotificationOutbox> findByStatusAndRetryCountLessThan(String status, int maxRetries);
}