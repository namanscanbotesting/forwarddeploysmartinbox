package com.pharma.inbox.repository;

import com.pharma.inbox.entity.IncomingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface IncomingMessageRepository extends JpaRepository<IncomingMessage, Long> {
    List<IncomingMessage> findByReceivedAtAfter(Instant since);
}
