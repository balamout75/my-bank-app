package com.mybank.notifications.repository;

import com.mybank.notifications.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT *
            FROM notifications.outbox_event
            WHERE status IN ('NEW','RETRY')
              AND next_attempt_at <= NOW()
            ORDER BY id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """, nativeQuery = true)
    List<OutboxEvent> lockBatchForProcessing(@Param("limit") int limit);
}
