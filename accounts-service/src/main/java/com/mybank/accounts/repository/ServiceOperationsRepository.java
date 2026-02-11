package com.mybank.accounts.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ServiceOperationsRepository {

    private final JdbcTemplate jdbc;

    /**
     * Записывает операцию для идемпотентности.
     *
     * @param operationId ID операции
     * @param username    пользователь
     * @param clientId    ID клиента (сервиса), от которого пришёл запрос
     * @return true если вставили (операция новая), false если уже была
     */
    public boolean insertIfAbsent(long operationId, String username, String clientId) {
        int updated = jdbc.update("""
            INSERT INTO service_operations(operation_id, username, service)
            VALUES (?, ?, ?)
            ON CONFLICT (service, operation_id) DO NOTHING
        """, operationId, username, clientId);
        return updated == 1;
    }
}