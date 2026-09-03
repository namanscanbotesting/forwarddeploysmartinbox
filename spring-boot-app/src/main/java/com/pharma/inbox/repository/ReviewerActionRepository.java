package com.pharma.inbox.repository;

import com.pharma.inbox.entity.ReviewerAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewerActionRepository extends JpaRepository<ReviewerAction, Long> {
    List<ReviewerAction> findByClassificationResultId(Long classificationResultId);
}
