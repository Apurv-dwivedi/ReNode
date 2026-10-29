package com.renode.repository;

import com.renode.entity.TermsContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsRepository extends JpaRepository<TermsContent, Integer> {
    TermsContent findTopByOrderByIdDesc();
}