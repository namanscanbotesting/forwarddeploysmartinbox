package com.pharma.inbox.repository;

import com.pharma.inbox.entity.ClassificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassificationResultRepository extends JpaRepository<ClassificationResult, Long> {
    List<ClassificationResult> findByIncomingMessageId(Long messageId);
    
    @Query("SELECT c FROM ClassificationResult c ORDER BY c.createdAt DESC")
    List<ClassificationResult> findAllOrderedByDate();
}
