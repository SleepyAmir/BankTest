package com.springbank.card.repository;

import com.springbank.card.entity.Card;
import com.springbank.common.repository.BaseEntityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends BaseEntityRepository<Card, Long> {

    Optional<Card> findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);

    List<Card> findByAccountId(Long accountId);
}
