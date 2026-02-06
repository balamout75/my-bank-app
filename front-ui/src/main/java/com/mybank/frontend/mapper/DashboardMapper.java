package com.mybank.frontend.mapper;

import com.mybank.frontend.client.dto.AccountMeResponse;
import com.mybank.frontend.client.dto.AccountSummaryResponse;
import com.mybank.frontend.viewmodel.FrontendDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class DashboardMapper {

    public FrontendDTO.MainPageModel toPageModel(
            AccountMeResponse me,
            List<AccountSummaryResponse> allAccounts,
            String successMessage,
            String errorMessage
    ) {
        var accountVm = toAccountInfo(me);

        var available = toSummaries(allAccounts, accountVm.getUsername());
        boolean accountsAvailable = (me != null);

        return FrontendDTO.MainPageModel.builder()
                .account(accountVm)
                .availableAccounts(accountsAvailable ? available : List.of())
                .accountsAvailable(accountsAvailable)
                // формы: чтобы th:object всегда был не null
                .accountUpdateForm(defaultUpdateForm(accountVm))
                .cashOperationForm(new FrontendDTO.CashOperationForm())
                .transferForm(new FrontendDTO.TransferForm())
                .successMessage(successMessage)
                .errorMessage(errorMessage)
                .build();
    }

    public FrontendDTO.AccountInfo toAccountInfo(AccountMeResponse dto) {
        if (dto == null) {
            // можно бросать исключение — но для фронта часто удобнее пустой объект
            return FrontendDTO.AccountInfo.builder()
                    .username("—")
                    .firstName("—")
                    .lastName("")
                    .balance(null)
                    .build();
        }

        LocalDate dob = dto.dateOfBirth();
        Integer age = (dob != null) ? Period.between(dob, LocalDate.now()).getYears() : null;

        return FrontendDTO.AccountInfo.builder()
                .username(dto.username())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .dateOfBirth(dob)
                .balance(dto.balance() == null ? null : dto.balance())
                .age(age)
                .build();
    }

    public List<FrontendDTO.AccountSummary> toSummaries(List<AccountSummaryResponse> all, String excludeUsername) {
        if (all == null) return List.of();

        return all.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.username() != null)
                .filter(a -> excludeUsername == null || !a.username().equalsIgnoreCase(excludeUsername))
                .sorted(Comparator.comparing(AccountSummaryResponse::username, String.CASE_INSENSITIVE_ORDER))
                .map(a -> new FrontendDTO.AccountSummary(
                        a.username(), a.fullName()

                ))
                .toList();
    }

    public FrontendDTO.AccountUpdateForm defaultUpdateForm(FrontendDTO.AccountInfo accountVm) {
        if (accountVm == null) return new FrontendDTO.AccountUpdateForm();

        var form = new FrontendDTO.AccountUpdateForm();
        form.setFirstName(accountVm.getFirstName());
        form.setLastName(accountVm.getLastName());
        form.setDateOfBirth(accountVm.getDateOfBirth());
        return form;
    }

    private String buildFullName(String firstName, String lastName) {
        String fn = (firstName == null) ? "" : firstName.trim();
        String ln = (lastName == null) ? "" : lastName.trim();
        String full = (fn + " " + ln).trim();
        return full.isBlank() ? "Без имени" : full;
    }
}
