package com.mybank.accounts.service;

import com.mybank.accounts.dto.AccountMeResponse;
import com.mybank.accounts.dto.AccountSummaryResponse;
import com.mybank.accounts.dto.AccountUpdateRequest;
import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.OperationStatus;
import com.mybank.accounts.model.UserAccount;
import com.mybank.accounts.repository.AccountOperationRepository;
import com.mybank.accounts.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountsServiceTest {

    @Mock AccountRepository repo;
    @Mock AccountOperationRepository accountOperationRepository;

    @InjectMocks AccountsService accountsService;

    private UserAccount alice() {
        return UserAccount.builder()
                .id(1L)
                .userName("alice")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@mybank.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .balance(new BigDecimal("10000.00"))
                .build();
    }

    // ======================== getMe ========================

    @Test
    @DisplayName("getMe → возвращает данные пользователя")
    void getMe_shouldReturnAccountResponse() {
        when(repo.findByUserName("alice")).thenReturn(Optional.of(alice()));

        AccountMeResponse response = accountsService.getMe("alice");

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.balance()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("getMe → пользователь не найден → IllegalArgumentException")
    void getMe_userNotFound_shouldThrow() {
        when(repo.findByUserName("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountsService.getMe("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    // ======================== updateMe ========================

    @Test
    @DisplayName("updateMe → обновляет профиль и создаёт операцию со статусом UPDATED")
    void updateMe_shouldUpdateProfileAndCreateOperation() {
        UserAccount alice = alice();
        when(repo.findByUserName("alice")).thenReturn(Optional.of(alice));
        when(accountOperationRepository.nextOperationId()).thenReturn(100L);
        when(accountOperationRepository.findById(100L)).thenReturn(Optional.empty());
        when(accountOperationRepository.save(any(AccountOperation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var req = new AccountUpdateRequest(null, "Alice", "Johnson", LocalDate.of(1990, 5, 15));

        accountsService.updateMe("alice", req);

        assertThat(alice.getLastName()).isEqualTo("Johnson");
        assertThat(alice.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        verify(repo).save(alice);

        ArgumentCaptor<AccountOperation> captor = ArgumentCaptor.forClass(AccountOperation.class);
        verify(accountOperationRepository, times(2)).save(captor.capture());
        AccountOperation saved = captor.getAllValues().getLast();
        assertThat(saved.getStatus()).isEqualTo(OperationStatus.UPDATED);
    }

    @Test
    @DisplayName("updateMe → возраст < 18 → IllegalArgumentException")
    void updateMe_underAge_shouldThrow() {
        var req = new AccountUpdateRequest(null, "Kid", "Young", LocalDate.now().minusYears(10));

        assertThatThrownBy(() -> accountsService.updateMe("alice", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("18+");
    }

    // ======================== getAllOthers ========================

    @Test
    @DisplayName("getAllOthers → возвращает список без текущего пользователя")
    void getAllOthers_shouldReturnOtherUsers() {
        var bob = UserAccount.builder()
                .userName("bob").firstName("Bob").lastName("Brown")
                .email("bob@mybank.com").balance(BigDecimal.ZERO).build();
        when(repo.findAllByUserNameNot("alice")).thenReturn(List.of(bob));

        List<AccountSummaryResponse> result = accountsService.getAllOthers("alice");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().username()).isEqualTo("bob");
        assertThat(result.getFirst().fullName()).isEqualTo("Bob Brown");
    }
}
