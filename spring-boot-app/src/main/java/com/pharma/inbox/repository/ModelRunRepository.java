package com.pharma.inbox.repository;

import com.pharma.inbox.entity.ModelRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRunRepository extends JpaRepository<ModelRun, Long> {
}
