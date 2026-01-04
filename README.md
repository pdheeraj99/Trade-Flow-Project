# 🚀 TradeFlow - Real-Time Cryptocurrency Trading Platform

# A professional-grade, microservices-based trading platform with **real-time market data**, JWT authentication, and event-driven architecture. #

![Status](https://img.shields.io/badge/status-active-success)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## 📊 Features

### ✅ Core Functionality

- **Real-Time Market Data** - Binance WebSocket integration (sub-second updates)
- **User Authentication** - JWT-based auth with access & refresh tokens
- **Order Management** - Limit & market orders with real-time execution
- **Wallet Management** - Multi-currency balance tracking with faucet
- **Trading Dashboard** - Live charts, order book, and active orders

### 🏗️ Architecture Highlights

- **Microservices** - 5 independent services (API Gateway, Auth, Market Data, OMS, Wallet)
- **Event Streaming** - Kafka for real-time order events
- **Command Messaging** - RabbitMQ for saga orchestration
- **Caching** - Redis for high-performance data access
- **Distributed Tracing** - Zipkin for observability

---

## 🔐 **IMPORTANT: Security Notice**

### ⚠️ Development Credentials

This repository contains **development/demo credentials** for ease of setup.

**🚨 THESE CREDENTIALS ARE NOT SECURE FOR PRODUCTION! 🚨**

### Current Default Credentials

| Service | Username | Password | Location |
|---------|----------|----------|----------|
| PostgreSQL | `tradeflow` | `tradeflow_secret` | `docker-compose.yml:11` |
| MongoDB | `tradeflow` | `tradeflow_secret` | `docker-compose.yml:156` |
| RabbitMQ | `tradeflow` | `tradeflow_secret` | `docker-compose.yml:134` |
| JWT Secret | N/A | `tradeflow-super-secret-key...` | `auth-service/application.yml:37` |
| CoinGecko API | N/A | `CG-JZVbfTVjYHf5aUk9rQqxKp4G` | `market-data-service/application.yml:39` |

### 🔒 Before Production Deployment

**You MUST change these credentials!** Follow the steps in the [Production Deployment](#-production-deployment) section.

---

## 🛠️ Tech Stack

### Backend

- **Java 21** - Modern LTS Java
- **Spring Boot 3.5.9** - Microservices framework
- **Spring Cloud Gateway** - API Gateway & routing
- **Spring Security** - JWT authentication
- **Spring Data JPA** - PostgreSQL persistence
- **Spring Data MongoDB** - Audit logs
- **Spring Kafka** - Event streaming
- **Spring AMQP** - RabbitMQ messaging
- **Spring WebSocket** - Real-time updates

### Frontend

- **React 19** - Modern UI framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool
- **React Router** - Client-side routing
- **Lightweight Charts** - Trading charts
- **React Hot Toast** - Notifications

### Infrastructure

- **PostgreSQL 16** - Primary database (users, orders, wallets)
- **Redis 7.2** - Caching & state store
- **Apache Kafka 3.5** - Event streaming
- **RabbitMQ 3.12** - Command messaging
- **MongoDB 7.0** - Audit logs
- **Zipkin** - Distributed tracing
- **Docker** - Containerization

---

## 🚀 Quick Start

### Prerequisites

- **Docker Desktop** (required)
- **Java 21** (for development)
- **Node.js 18+** (for frontend)
- **Maven 3.9+** (for backend builds)

### 1️⃣ Clone Repository

```bash
git clone https://github.com/yourusername/Trade-Flow-Project.git
cd Trade-Flow-Project
```

### 2️⃣ Start Infrastructure

```bash
docker-compose up -d
```

**Wait ~30 seconds** for all services to be healthy. Verify:

```bash
docker-compose ps
```

### 3️⃣ Start Backend Services

**In separate terminals:**

```bash
# API Gateway (Port 8080)
cd api-gateway
mvn spring-boot:run

# Auth Service (Port 8081)
cd auth-service
mvn spring-boot:run

# Market Data Service (Port 8085) - Real-time Binance WebSocket!
cd market-data-service
mvn spring-boot:run

# OMS Service (Port 8083)
cd oms-service
mvn spring-boot:run

# Wallet Service (Port 8084)
cd wallet-service
mvn spring-boot:run
```

### 4️⃣ Start Frontend

```bash
cd frontend
npm install
npm run dev
```

### 5️⃣ Access Application

- **Frontend**: <http://localhost:5173>
- **API Gateway**: <http://localhost:8080>
- **Kafka UI**: <http://localhost:8090>
- **RabbitMQ**: <http://localhost:15672> (user: `tradeflow`, pass: `tradeflow_secret`)
- **Zipkin**: <http://localhost:9411>

---

## 📖 User Guide

### Registration & Login

1. Navigate to <http://localhost:5173>
2. Click **Register** → Create account
3. Auto-login after registration
4. View Dashboard with real-time BTC/USDT prices!

### Using the Faucet

1. Go to **Wallet** section
2. Click **Use Faucet** to get demo funds
3. Receive 10,000 USDT + 0.1 BTC

### Placing Orders

1. Select **BUY** or **SELL**
2. Choose **LIMIT** or **MARKET**
3. Enter price & quantity
4. Click **Place Order**
5. View in **Active Orders** section

### Real-Time Updates

- **Market Data**: Updates every ~1 second via Binance WebSocket
- **Order Status**: Real-time via backend WebSocket
- **Charts**: Live candlestick data

---

## 🔧 Configuration

### Environment Variables

Create `.env` file in project root:

```bash
# Database
POSTGRES_PASSWORD=your-secure-password-here
MONGO_PASSWORD=your-secure-password-here

# Messaging
RABBITMQ_PASSWORD=your-secure-password-here

# JWT Authentication (CRITICAL!)
JWT_SECRET=your-super-secret-jwt-key-minimum-256-bits

# External APIs (Optional - Fallback only)
COINGECKO_API_KEY=your-api-key-here
```

**Note:** `.env` is already in `.gitignore` and will NOT be committed.

### Application Configuration

All services use `application.yml` with environment variable support:

```yaml
jwt:
  secret: ${JWT_SECRET:dev-default-change-in-production}
```

**Format:** `${ENV_VAR:default-value}`

- Uses environment variable if set
- Falls back to default if not set

---

## 🏭 Production Deployment

### Step 1: Generate Secure Secrets

```bash
# Generate JWT Secret (256+ bits)
openssl rand -base64 64

# Generate Database Passwords
openssl rand -base64 32
```

### Step 2: Set Environment Variables

**Option A: Docker Compose**

Edit `docker-compose.yml`:

```yaml
postgres:
  environment:
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}  # Remove default

mongodb:
  environment:
    MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}

rabbitmq:
  environment:
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
```

**Option B: Kubernetes Secrets**

```bash
kubectl create secret generic tradeflow-secrets \
  --from-literal=jwt-secret=YOUR_JWT_SECRET \
  --from-literal=postgres-password=YOUR_DB_PASSWORD
```

**Option C: Cloud Provider Secrets**

- **AWS**: Secrets Manager / Parameter Store
- **Azure**: Key Vault
- **GCP**: Secret Manager

### Step 3: Update Application Configs

Remove hardcoded defaults from `application.yml`:

```yaml
# ❌ Before (Insecure)
jwt:
  secret: tradeflow-super-secret-key...

# ✅ After (Secure)
jwt:
  secret: ${JWT_SECRET}  # No default!
```

### Step 4: Enable HTTPS

Update API Gateway:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
```

### Step 5: Database Hardening

- [ ] Change all default passwords
- [ ] Enable SSL/TLS connections
- [ ] Configure firewall rules (whitelist only backend services)
- [ ] Enable audit logging
- [ ] Set up automated backups

### Step 6: Rate Limiting & DDoS Protection

- [ ] Configure API Gateway rate limits
- [ ] Add WAF (Web Application Firewall)
- [ ] Enable Cloudflare or similar CDN
- [ ] Set up monitoring alerts

---

## 📊 Architecture

### System Overview

```
┌─────────────────┐
│  Frontend (React)│
└────────┬─────────┘
         │ HTTP + WebSocket
         ↓
┌─────────────────────┐
│  API Gateway :8080  │ ← JWT Validation
└──────────┬──────────┘
           │
    ┌──────┴──────┬──────────┬──────────┐
    ↓             ↓          ↓          ↓
┌─────────┐  ┌─────────┐ ┌─────────┐ ┌─────────┐
│ Auth    │  │ Market  │ │  OMS    │ │ Wallet  │
│ :8081   │  │ Data    │ │ :8083   │ │ :8084   │
│         │  │ :8085   │ │         │ │         │
└────┬────┘  └────┬────┘ └────┬────┘ └────┬────┘
     │            │           │           │
     └────────┬───┴───────┬───┴───────────┘
              ↓           ↓
         ┌─────────┐ ┌─────────┐
         │ Kafka   │ │RabbitMQ │
         │ :9092   │ │ :5672   │
         └─────────┘ └─────────┘
              ↓
    ┌─────────┴─────────┬────────────┐
    ↓                   ↓            ↓
┌──────────┐      ┌─────────┐  ┌─────────┐
│PostgreSQL│      │  Redis  │  │ MongoDB │
│  :5432   │      │  :6379  │  │ :27017  │
└──────────┘      └─────────┘  └─────────┘
```

### Data Flow

**1. User Registration:**

```
Frontend → API Gateway → Auth Service → PostgreSQL
                                      └→ Wallet Service → Create wallets
```

**2. Real-Time Market Data:**

```
Binance WebSocket → Market Data Service → Redis Cache
                                        └→ WebSocket → Frontend
```

**3. Order Placement:**

```
Frontend → API Gateway → OMS Service → Kafka (OrderCreated event)
                                     └→ PostgreSQL (orders table)
                                     └→ WebSocket → Frontend
```

---

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Test WebSocket Connections

```bash
# Install wscat
npm install -g wscat

# Connect to market data
wscat -c ws://localhost:8085/ws/market

# Connect to order updates (requires auth)
wscat -c ws://localhost:8080/ws/orders -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📈 Monitoring & Observability

### Zipkin Tracing

- **URL**: <http://localhost:9411>
- **Features**: Distributed request tracing across microservices

### Kafka UI

- **URL**: <http://localhost:8090>
- **Features**: Topic monitoring, consumer lag, message inspection

### RabbitMQ Management

- **URL**: <http://localhost:15672>
- **Login**: `tradeflow` / `tradeflow_secret`
- **Features**: Queue monitoring, message rates

### Application Metrics

All services expose Spring Boot Actuator endpoints:

```bash
# Health check
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics
```

---

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Kill process on port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Services Won't Start

```bash
# Check Docker containers
docker-compose ps

# View logs
docker-compose logs postgres
docker-compose logs kafka

# Restart services
docker-compose restart
```

### WebSocket Connection Failed

- Ensure API Gateway is running
- Check JWT token is valid
- Verify CORS settings in `application.yml`

### Database Connection Refused

- Verify PostgreSQL container is running
- Check credentials in `application.yml`
- Ensure port 5432 is not blocked

---

## 📚 API Documentation

### Authentication

**Register:**

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Login:**

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response:
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "user": { ... }
}
```

### Market Data

**Get Ticker:**

```http
GET /api/market/ticker/BTCUSDT
Authorization: Bearer <token>

Response:
{
  "symbol": "BTCUSDT",
  "price": 87849.01,
  "change24h": 1234.56,
  "changePercent24h": 1.42
}
```

### Orders

**Create Order:**

```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "symbol": "BTCUSDT",
  "side": "BUY",
  "type": "LIMIT",
  "price": 87000.00,
  "quantity": 0.01
}
```

---

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file.

---

## 🙏 Acknowledgments

- **Binance** - Real-time market data WebSocket API
- **CoinGecko** - Fallback market data API
- **Spring Team** - Amazing microservices framework
- **TradingView** - Lightweight Charts library

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/Trade-Flow-Project/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/Trade-Flow-Project/discussions)

---

**Built with ❤️ by Dheeraj**

⭐ Star this repo if you found it helpful!
