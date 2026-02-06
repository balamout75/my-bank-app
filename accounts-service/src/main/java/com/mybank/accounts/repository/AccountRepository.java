package com.mybank.accounts.repository;

import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUserName(String username);
    List<UserAccount> findAllByUserNameNot(String username);

    interface AccountOperationRepository extends JpaRepository<AccountOperation, Long> {
    }
}
