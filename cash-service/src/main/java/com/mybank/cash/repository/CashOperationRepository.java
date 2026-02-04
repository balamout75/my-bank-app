package com.mybank.cash.repository;

import com.mybank.cash.model.CashOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

    Optional<CashOperation> findById(Long operationId);

    // Получить следующее значение sequence
    @Query(value = "SELECT nextval('cash_operation_sequence')", nativeQuery = true)
    Long getNextOperationId();
}