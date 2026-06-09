package com.springbank.internal;

import com.springbank.account.dto.AccountBalanceDto;
import com.springbank.account.dto.InternalTransferDto;
import com.springbank.account.dto.TransactionResultDto;
import com.springbank.account.service.AccountReadService;
import com.springbank.account.service.MoneyMovementService;
import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final AccountReadService accountReadService;
    private final MoneyMovementService moneyMovementService;
    private final UserService userService;

    /**
     * انتقال وجه «اتمیک» سرویس‌به‌سرویس (فراخوانی از transaction-write).
     * کل برداشت+واریز در یک تراکنش DB انجام می‌شود → جایگزین دو فراخوانی جدای قبلی.
     */
    @PostMapping("/accounts/transfer")
    public TransactionResultDto transfer(@Valid @RequestBody InternalTransferDto dto) {
        log.debug("Internal atomic transfer: {} -> {} amount {}", dto.fromAccountId(), dto.toAccountId(), dto.amount());
        return moneyMovementService.transferAtomic(dto);
    }

    @GetMapping("/accounts/{id}/balance")
    public AccountBalanceDto getBalance(@PathVariable Long id) {
        log.debug("Internal balance request for account: {}", id);
        return accountReadService.getBalanceInternal(id);
    }

    @PostMapping("/accounts/{id}/deposit")
    public TransactionResultDto deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.debug("Internal atomic deposit for account: {} amount: {}", id, amount);
        return moneyMovementService.depositAtomic(id, amount);
    }

    @PostMapping("/accounts/{id}/withdraw")
    public TransactionResultDto withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.debug("Internal atomic withdraw for account: {} amount: {}", id, amount);
        return moneyMovementService.withdrawAtomic(id, amount);
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUserSummary(@PathVariable Long id) {
        log.debug("Internal user summary request for: {}", id);
        UserResponseDto user = userService.getUserById(id);
        return Map.of(
                "id", user.id(),
                "username", user.username(),
                "email", user.email(),
                "firstName", user.firstName(),
                "lastName", user.lastName(),
                "enabled", user.enabled()
        );
    }

    @GetMapping("/accounts/{id}")
    public Map<String, Object> getAccountSummary(@PathVariable Long id) {
        log.debug("Internal account summary request for: {}", id);
        var account = accountReadService.getById(id);
        return Map.of(
                "id", account.id(),
                "accountNumber", account.accountNumber(),
                "userId", account.userId(),
                "status", account.status().toString()
        );
    }
}
