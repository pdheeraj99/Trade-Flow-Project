package com.tradeflow.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TradeFlow Order Management System
 * Handles order lifecycle and Saga orchestration for distributed transactions
 */
@SpringBootApplication
public class OmsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmsServiceApplication.class, args);
    }
}
