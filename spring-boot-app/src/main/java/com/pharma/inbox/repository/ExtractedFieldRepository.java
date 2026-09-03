package com.pharma.inbox.repository;

import com.pharma.inbox.entity.ExtractedField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExtractedFieldRepository extends JpaRepository<ExtractedField, Long> {
    List<ExtractedField> findByClassificationResultId(Long classificationResultId);
}
