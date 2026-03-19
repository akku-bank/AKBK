package com.akku.backend.domain.bank.dto;

import com.akku.backend.domain.bank.entity.Card;

import java.util.UUID;

public record CardResponse(
    UUID id,
    UUID cardProductId,
    String cardNo,
    String cardName,
    String cardIssuerName,
    String withdrawalAccountNo,
    String withdrawalDate,
    String cardExpiryDate,
    Boolean isActive
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
            card.getId(),
            card.getCardProductId(),
            card.getCardNo(),
            card.getCardProduct() != null ? card.getCardProduct().getCardName() : null,
            card.getCardProduct() != null ? card.getCardProduct().getCardIssuerName() : null,
            card.getWithdrawalAccountNo(),
            card.getWithdrawalDate(),
            card.getCardExpiryDate(),
            card.getIsActive()
        );
    }
}
