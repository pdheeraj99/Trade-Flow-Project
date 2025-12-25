package com.tradeflow.wallet.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for safe BigDecimal operations with proper scale handling.
 * Prevents ArithmeticException from division and ensures consistent precision.
 */
public final class MoneyUtils {

    private MoneyUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Default scale for currency calculations (8 decimals for crypto satoshi
     * precision)
     */
    public static final int DEFAULT_SCALE = 8;

    /**
     * Default rounding mode for financial calculations
     */
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    /**
     * Add two amounts with proper scale
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return add(a, b, DEFAULT_SCALE);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b, int scale) {
        BigDecimal left = a != null ? a : BigDecimal.ZERO;
        BigDecimal right = b != null ? b : BigDecimal.ZERO;
        return left.add(right).setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * Subtract two amounts with proper scale
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return subtract(a, b, DEFAULT_SCALE);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b, int scale) {
        BigDecimal left = a != null ? a : BigDecimal.ZERO;
        BigDecimal right = b != null ? b : BigDecimal.ZERO;
        return left.subtract(right).setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * Multiply two amounts with proper scale
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return multiply(a, b, DEFAULT_SCALE);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b, int scale) {
        BigDecimal left = a != null ? a : BigDecimal.ZERO;
        BigDecimal right = b != null ? b : BigDecimal.ZERO;
        return left.multiply(right).setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * Divide two amounts with proper scale (avoids ArithmeticException)
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return divide(a, b, DEFAULT_SCALE);
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b, int scale) {
        if (b == null || b.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        BigDecimal left = a != null ? a : BigDecimal.ZERO;
        return left.divide(b, scale, DEFAULT_ROUNDING);
    }

    /**
     * Negate with proper scale
     */
    public static BigDecimal negate(BigDecimal a) {
        return negate(a, DEFAULT_SCALE);
    }

    public static BigDecimal negate(BigDecimal a, int scale) {
        BigDecimal value = a != null ? a : BigDecimal.ZERO;
        return value.negate().setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * Normalize to standard scale
     */
    public static BigDecimal normalize(BigDecimal a) {
        return normalize(a, DEFAULT_SCALE);
    }

    public static BigDecimal normalize(BigDecimal a, int scale) {
        BigDecimal value = a != null ? a : BigDecimal.ZERO;
        return value.setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * Check if positive
     */
    public static boolean isPositive(BigDecimal a) {
        return a != null && a.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if negative
     */
    public static boolean isNegative(BigDecimal a) {
        return a != null && a.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Check if zero
     */
    public static boolean isZero(BigDecimal a) {
        return a == null || a.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Get zero with proper scale
     */
    public static BigDecimal zero() {
        return zero(DEFAULT_SCALE);
    }

    public static BigDecimal zero(int scale) {
        return BigDecimal.ZERO.setScale(scale, DEFAULT_ROUNDING);
    }
}
