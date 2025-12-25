package com.tradeflow.common.constants;

/**
 * Trading pair constants and configurations
 */
public final class TradingPairs {

    private TradingPairs() {
        // Utility class - prevent instantiation
    }

    // Supported trading pairs
    public static final String BTC_USDT = "BTCUSDT";
    public static final String ETH_USDT = "ETHUSDT";

    // Currencies
    public static final String USD = "USD";
    public static final String USDT = "USDT";
    public static final String BTC = "BTC";
    public static final String ETH = "ETH";

    // CoinGecko coin IDs (for API mapping)
    public static final String COINGECKO_BITCOIN = "bitcoin";
    public static final String COINGECKO_ETHEREUM = "ethereum";

    // Default faucet amount
    public static final String FAUCET_AMOUNT = "10000.00";
    public static final String FAUCET_CURRENCY = USD;

    /**
     * Get base currency from trading pair
     * Example: BTCUSDT -> BTC
     */
    public static String getBaseCurrency(String symbol) {
        if (symbol == null || symbol.length() < 4) {
            throw new IllegalArgumentException("Invalid symbol: " + symbol);
        }
        // Assuming format: BASE + USDT (4 chars for USDT)
        return symbol.substring(0, symbol.length() - 4);
    }

    /**
     * Get quote currency from trading pair
     * Example: BTCUSDT -> USDT
     */
    public static String getQuoteCurrency(String symbol) {
        if (symbol == null || symbol.length() < 4) {
            throw new IllegalArgumentException("Invalid symbol: " + symbol);
        }
        return symbol.substring(symbol.length() - 4);
    }

    /**
     * Map trading pair symbol to CoinGecko coin ID
     */
    public static String toCoinGeckoId(String baseCurrency) {
        return switch (baseCurrency.toUpperCase()) {
            case BTC -> COINGECKO_BITCOIN;
            case ETH -> COINGECKO_ETHEREUM;
            default -> throw new IllegalArgumentException("Unsupported currency: " + baseCurrency);
        };
    }
}
