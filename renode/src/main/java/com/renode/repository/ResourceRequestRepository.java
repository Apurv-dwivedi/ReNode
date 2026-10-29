package com.renode.repository;

import com.renode.entity.ResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, Integer> {

    // Fetch requests I have received (Incoming)
    List<ResourceRequest> findByReceiverIdOrderByCreatedAtDesc(Integer receiverId);

    // Fetch requests I have sent (Outgoing)
    List<ResourceRequest> findBySenderIdOrderByCreatedAtDesc(Integer senderId);

    // For KPI Dashboard: Count successful matches
    long countByReceiverIdAndStatus(Integer receiverId, com.renode.entity.RequestStatus status);
    long countBySenderIdAndStatus(Integer senderId, com.renode.entity.RequestStatus status);
}