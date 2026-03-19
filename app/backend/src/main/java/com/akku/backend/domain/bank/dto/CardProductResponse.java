package com.akku.backend.domain.bank.dto;

import com.akku.backend.domain.bank.entity.CardProduct;

import java.util.UUID;

public record CardProductResponse(
    UUID id,
    String cardName,
    String cardDescription,
    String cardIssuerName,
    String cardTypeName,
    Long baseLimitPerformance,
    Long maxBenefitLimit
) {
    public static CardProductResponse from(CardProduct cardProduct) {
        return new CardProductResponse(
            cardProduct.getId(),
            cardProduct.getCardName(),
            cardProduct.getCardDescription(),
            cardProduct.getCardIssuerName(),
            cardProduct.getCardTypeName(),
            cardProduct.getBaseLimitPerformance(),
            cardProduct.getMaxBenefitLimit()
        );
    }
}
