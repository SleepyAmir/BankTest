package com.springbank.card.service;

import com.springbank.card.dto.CardCreateDto;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.dto.CardUpdateDto;
import com.springbank.card.entity.Card;
import com.springbank.card.mapper.CardMapper;
import com.springbank.card.repository.CardRepository;
import com.springbank.common.annotation.Auditable;
import com.springbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardWriteService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final PasswordEncoder passwordEncoder;

    @Auditable(action = "CREATE_CARD", entity = "Card")
    @CacheEvict(value = "cards", key = "#result.id")
    public CardResponseDto createCard(CardCreateDto dto) {
        if (cardRepository.existsByCardNumber(dto.cardNumber())) {
            throw new IllegalArgumentException("Card number already exists");
        }
        Card card = cardMapper.toEntity(dto);
        // Encrypt CVV2 and PIN
        card.setCvv2(passwordEncoder.encode(dto.cvv2()));
        if (dto.pin() != null) {
            card.setPin(passwordEncoder.encode(dto.pin()));
        }
        Card saved = cardRepository.save(card);
        return cardMapper.toDto(saved);
    }

    @Auditable(action = "UPDATE_CARD", entity = "Card")
    @CacheEvict(value = "cards", key = "#id")
    public CardResponseDto updateCard(Long id, CardUpdateDto dto) {
        Card card = cardRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
        cardMapper.updateFromDto(dto, card);
        return cardMapper.toDto(cardRepository.save(card));
    }

    @Auditable(action = "DELETE_CARD", entity = "Card")
    @CacheEvict(value = "cards", key = "#id")
    public void deleteCard(Long id) {
        cardRepository.softDelete(id);
    }

    @Auditable(action = "BLOCK_CARD", entity = "Card")
    @CacheEvict(value = "cards", key = "#id")
    public CardResponseDto blockCard(Long id) {
        Card card = cardRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card", id));
        card.setStatus(com.springbank.common.enums.CardStatus.BLOCKED);
        return cardMapper.toDto(cardRepository.save(card));
    }
}
