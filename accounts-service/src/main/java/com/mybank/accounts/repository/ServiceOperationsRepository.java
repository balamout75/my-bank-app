package com.mybank.accounts.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ServiceOperationsRepository {
    private final JdbcTemplate jdbc;
    /** @return true если вставили (операция новая), false если уже была */
    public boolean insertIfAbsent(long operationId, String username) {
        int updated = jdbc.update("""
            INSERT INTO applied_operations(operation_id, username)
            VALUES (?, ?)
            ON CONFLICT (operation_id) DO NOTHING
        """, operationId, username);
        return updated == 1;
    }
}
