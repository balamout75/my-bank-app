package com.mybank.transfer.repository;

import com.mybank.transfer.dto.OperationStatus;
import com.mybank.transfer.model.TransferOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {

    Optional<TransferOperation> findById(Long operationId);

    // Получить следующее значение sequence
    @Query(value = "SELECT nextval('transfer_operation_sequence')", nativeQuery = true)
    Long getNextOperationId();

    Page <TransferOperation> findByStatus(OperationStatus operationStatus, Pageable pageable);
}