# TradeFlow HFT Simulation Platform

[![Build Status](https://github.com/username/Trade-Flow-Project/actions/workflows/build.yml/badge.svg)](https://github.com/username/Trade-Flow-Project/actions)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5.9](https://img.shields.io/badge/Spring%20Boot-3.5.9-green.svg)](https://spring.io/projects/spring-boot)

A high-frequency trading simulation platform with real-time market data from CoinGecko, built using a Hybrid Event-Driven Microservices Architecture.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Frontend (React 19)                          │
│                     TradingView Charts + WebSocket                  │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │       API Gateway         │
                    │   (JWT + Rate Limiting)   │
                    └─────────────┬─────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼───────┐       ┌────────▼────────┐       ┌────────▼────────┐
│  Auth Service │       │  Wallet Service │       │   OMS Service   │
│     (JWT)     │       │  (Ledger + Saga)│       │ (Saga Orchestr) │
└───────────────┘       └────────┬────────┘       └────────┬────────┘
                                 │                         │
                          RabbitMQ (Commands)        Kafka (Events)
                                 │                         │
                    ┌────────────┴─────────────────────────┴───────┐
                    │              Matching Engine                  │
                    │         (Price-Time Priority LOB)             │
                    └────────────────────┬─────────────────────────┘
                                         │
                              ┌──────────▼──────────┐
                              │  Market Data Service │
                              │  (CoinGecko + Redis) │
                              └─────────────────────┘
```

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| **Backend** | Spring Boot 3.5.9, Java 21 (Virtual Threads) |
| **Database** | PostgreSQL 16 (Double-Entry Ledger) |
| **Cache** | Redis 7.2 |
| **Event Streaming** | Apache Kafka 3.5 |
| **Command Messaging** | RabbitMQ 3.12 |
| **Audit Store** | MongoDB 7.0 |
| **Tracing** | Zipkin + Micrometer |
| **Frontend** | React 19 + Vite + lightweight-charts |

## 📦 Modules

| Module | Description | Port |
|--------|-------------|------|
| `api-gateway` | JWT validation, routing, rate limiting | 8080 |
| `auth-service` | User registration, login, JWT tokens | 8081 |
| `wallet-service` | Double-entry ledger, faucet | 8082 |
| `oms-service` | Order lifecycle, Saga orchestration | 8083 |
| `matching-engine` | Price-time priority order matching | 8084 |
| `market-data-service` | CoinGecko API, WebSocket broadcast | 8085 |
| `audit-service` | MongoDB audit logging | 8086 |

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+
- Node.js 20+ (for frontend)

### 1. Start Infrastructure

```bash
docker-compose up -d
```

### 2. Verify Services

| Service | URL |
|---------|-----|
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| Kafka UI | `http://localhost:8090` |
| RabbitMQ | `http://localhost:15672` (tradeflow/tradeflow_secret) |
| MongoDB | `localhost:27017` |
| Zipkin | `http://localhost:9411` |

### 3. Build & Run

```bash
# Build all modules
mvn clean install

# Run services (each in separate terminal)
cd auth-service && mvn spring-boot:run
cd wallet-service && mvn spring-boot:run
cd oms-service && mvn spring-boot:run
cd matching-engine && mvn spring-boot:run
cd market-data-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### 4. Environment Variables

```bash
# CoinGecko API Key (required for market data)
export COINGECKO_API_KEY=your_api_key_here
```

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify -Pintegration-tests
```

## 📊 Trading Pairs

Initial pairs (configurable):

- `BTC/USDT`
- `ETH/USDT`

## 📄 License

Private - All rights reserved.
