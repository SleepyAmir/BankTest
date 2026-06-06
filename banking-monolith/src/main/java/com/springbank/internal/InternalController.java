package com.springbank.internal;

import com.springbank.account.dto.AccountBalanceDto;
import com.springbank.account.service.AccountReadService;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final AccountReadService accountReadService;
    private final UserService userService;

    @GetMapping("/accounts/{id}/balance")
    public AccountBalanceDto getBalance(@PathVariable Long id) {
        log.debug("Internal balance request for account: {}", id);
        return accountReadService.getBalanceInternal(id);
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
