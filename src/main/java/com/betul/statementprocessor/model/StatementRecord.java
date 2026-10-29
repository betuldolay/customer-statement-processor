package com.betul.statementprocessor.model;

import java.math.BigDecimal;

public record StatementRecord(
        long reference,
        String accountNumber,
        String description,
        BigDecimal startBalance,
        BigDecimal mutation,
        BigDecimal endBalance
) {
}