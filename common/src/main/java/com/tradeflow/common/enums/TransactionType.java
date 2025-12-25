package com.tradeflow.common.enums;

/**
 * Transaction type enumeration for wallet ledger entries
 */
public enum TransactionType {
    DEPOSIT, // External deposit (faucet, admin deposit)
    WITHDRAWAL, // External withdrawal
    RESERVE, // Funds reserved for pending order
    RELEASE, // Reserved funds released (order cancelled/rejected)
    TRADE_DEBIT, // Debit from completed trade
    TRADE_CREDIT, // Credit from completed trade
    FEE // Trading fee deduction
}
