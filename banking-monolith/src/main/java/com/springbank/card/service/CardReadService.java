package com.springbank.card.service;

import com.springbank.account.service.AccountReadService;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.entity.Card;
import com.springbank.card.mapper.CardMapper;
import com.springbank.card.repository.CardRepository;
import com.springbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardReadService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final AccountReadService accountReadService;

    /** شناسه‌ی کاربر صاحب یک حساب (برای کنترل دسترسی به کارت‌های آن حساب). */
    public Long getAccountOwnerUserId(Long accountId) {
        return accountReadService.getById(accountId).userId();
    }

    @Cacheable(value = "cards", key = "#id")
    public CardResponseDto getById(Long id) {
        return cardRepository.findActiveById(id)
                .map(cardMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
    }

    public List<CardResponseDto> getByAccountId(Long accountId) {
        return cardRepository.findByAccountId(accountId).stream()
                .map(cardMapper::toDto)
                .collect(Collectors.toList());
    }

    public Card getEntityById(Long id) {
        return cardRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
    }

    public List<CardResponseDto> getAllActive() {
        return cardRepository.findAllActive().stream()
                .map(cardMapper::toDto)
                .collect(Collectors.toList());
    }
}
