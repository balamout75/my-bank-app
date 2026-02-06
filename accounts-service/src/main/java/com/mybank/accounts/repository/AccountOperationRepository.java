package com.mybank.accounts.repository;

import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.OperationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountOperationRepository extends JpaRepository<AccountOperation, Long> {

    @Query(value = "select nextval('accounts.notification_operation_seq')", nativeQuery = true)
    Long nextOperationId();

    Page <AccountOperation> findByStatus(OperationStatus status, Pageable pageable);

}