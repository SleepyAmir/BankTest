package com.springbank.card.service;

import com.springbank.account.entity.Account;
import com.springbank.card.dto.CardCreateDto;
import com.springbank.card.dto.CardResponseDto;
import com.springbank.card.dto.CardUpdateDto;
import com.springbank.card.dto.IssuedCardDto;
import com.springbank.card.entity.Card;
import com.springbank.card.mapper.CardMapper;
import com.springbank.card.repository.CardRepository;
import com.springbank.common.annotation.Auditable;
import com.springbank.common.enums.CardStatus;
import com.springbank.common.enums.CardType;
import com.springbank.common.exception.ResourceNotFoundException;
import com.springbank.common.util.CardNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CardWriteService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * صدور خودکار کارت مجازی فعال برای یک حساب (فلوی ۳).
     * <p>
     * شماره کارت ۱۶ رقمی معتبر (Luhn)، CVV2، PIN و تاریخ انقضای ۴ ساله تولید می‌شود.
     * CVV2 و PIN قبل از ذخیره هش می‌شوند؛ مقادیر آشکار فقط در پاسخ برگردانده می‌شوند.
     *
     * @param account حساب مالک کارت
     * @return اطلاعات کارت صادرشده شامل CVV2/PIN آشکار (یک‌بار)
     */
    @Auditable(action = "ISSUE_VIRTUAL_CARD", entity = "Card")
    public IssuedCardDto issueVirtualCardForAccount(Account account) {
        String cardNumber = generateUniqueCardNumber();
        String rawCvv2 = CardNumberGenerator.generateCvv2();
        String rawPin = CardNumberGenerator.generatePin();
        LocalDate expiry = LocalDate.now().plusYears(4);

        Card card = Card.builder()
                .cardNumber(cardNumber)
                .cvv2(passwordEncoder.encode(rawCvv2))
                .pin(passwordEncoder.encode(rawPin))
                .type(CardType.DEBIT)
                .status(CardStatus.ACTIVE)
                .expiryDate(expiry)
                .isContactless(true)
                .dailyLimit(new BigDecimal("10000000"))
                .monthlyLimit(new BigDecimal("50000000"))
                .monthlySpent(BigDecimal.ZERO)
                .account(account)
                .build();

        Card saved = cardRepository.save(card);
        log.info("[CARD-ISSUE] ✅ کارت مجازی برای حساب id={} صادر شد: cardId={}, number=****{}",
                account.getId(), saved.getId(), cardNumber.substring(12));

        return new IssuedCardDto(
                saved.getId(), saved.getCardNumber(), rawCvv2, rawPin,
                saved.getType(), saved.getStatus(), saved.getExpiryDate(),
                saved.getDailyLimit(), saved.getMonthlyLimit(), account.getId());
    }

    private String generateUniqueCardNumber() {
        String number;
        int attempts = 0;
        do {
            number = CardNumberGenerator.generateCardNumber();
            if (++attempts > 10) {
                throw new IllegalStateException("امکان تولید شماره کارت یکتا فراهم نشد");
            }
        } while (cardRepository.existsByCardNumber(number));
        return number;
    }

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
