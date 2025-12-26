# TradeFlow - Run Guide 🚀

## Prerequisites

- **Java 21** (OpenJDK recommended)
- **Maven 3.9+**
- **Node.js 18+** (with npm)
- **Docker Desktop** (with Docker Compose v2)

## Quick Start

### Step 1: Start Infrastructure

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project
docker-compose up -d
```

Wait for all containers to be healthy (about 30-60 seconds):

```bash
docker-compose ps
```

Expected output - all services should show "healthy":

- `tradeflow-postgres` (5432)
- `tradeflow-redis` (6379)
- `tradeflow-zookeeper` (2181)
- `tradeflow-kafka` (9092)
- `tradeflow-rabbitmq` (5672, 15672)
- `tradeflow-mongodb` (27017)
- `tradeflow-zipkin` (9411)
- `tradeflow-kafka-ui` (8090)

### Step 2: Build Common Module

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project
mvnd clean install -DskipTests -pl common
```

### Step 3: Start Backend Services

Open **6 separate terminal windows** and run each service:

**Terminal 1 - Auth Service (Port 8081):**

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\auth-service
mvnd spring-boot:run
```

**Terminal 2 - Wallet Service (Port 8082):**

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\wallet-service
mvnd spring-boot:run
```

**Terminal 3 - OMS Service (Port 8083):**

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\oms-service
mvnd spring-boot:run
```

**Terminal 4 - Matching Engine (Port 8084):**

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\matching-engine
mvnd spring-boot:run
```

**Terminal 5 - Market Data Service (Port 8085):**

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\market-data-service
mvnd spring-boot:run
```

**Terminal 6 - API Gateway (Port 8080):**

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\api-gateway
mvnd spring-boot:run
```

### Step 4: Start Frontend

```bash
cd d:\Antigravity_Projects\Trade-Flow-Project\frontend
npm install
npm run dev
```

Frontend will be available at: **<http://localhost:5173>**

---

## Port Reference

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 5173 | React + Vite |
| API Gateway | 8080 | All traffic entry point |
| Auth Service | 8081 | Authentication/JWT |
| Wallet Service | 8082 | Balance & Faucet |
| OMS Service | 8083 | Order Management |
| Matching Engine | 8084 | Order Matching |
| Market Data | 8085 | Prices & WebSocket |
| Audit Service | 8086 | Event Logging |
| PostgreSQL | 5432 | Primary DB |
| Redis | 6379 | Cache |
| Kafka | 9092 | Event Streaming |
| RabbitMQ | 5672 | Saga Messages |
| RabbitMQ UI | 15672 | Management Console |
| Kafka UI | 8090 | Cluster Monitor |
| Zipkin | 9411 | Distributed Tracing |

---

## E2E Test Checklist

### ✅ Registration Flow

1. Navigate to <http://localhost:5173>
2. Click "Register"
3. Fill in: Username, Email, Password
4. Submit → Should redirect to Dashboard
5. **Verify:** Check RabbitMQ console (localhost:15672) for `user.created` message

### ✅ Login Flow

1. Navigate to <http://localhost:5173/login>
2. Enter credentials
3. Submit → Should redirect to Dashboard with JWT stored

### ✅ Wallet & Faucet

1. On Dashboard, click "Wallet" tab
2. Click "Claim Faucet" button
3. **Verify:** Balance should show $10,000 USD

### ✅ WebSocket (Live Prices)

1. On Dashboard, observe the BTC/USDT price ticker
2. **Verify:** Green/Yellow dot indicates connection status
3. Price should update in real-time

### ✅ Place Order

1. In Order Form (right panel), select BUY
2. Enter Price and Quantity
3. Click "Buy BTC"
4. **Verify:** Order appears in "Active Orders" tab

---

## Troubleshooting

### Docker Issues

```bash
# Reset everything
docker-compose down -v
docker-compose up -d
```

### Database Schema Issues

```bash
# Check if schemas were created
docker exec -it tradeflow-postgres psql -U tradeflow -d tradeflow -c "\dn"
```

### Service Won't Start

1. Ensure PostgreSQL is running: `docker-compose ps`
2. Check port conflicts: `netstat -an | findstr "8080 8081 8082"`
3. View logs: `docker-compose logs kafka`

### CORS Errors in Browser

- Ensure API Gateway is running on port 8080
- Check browser console for specific error
- Verify frontend is running on port 5173

---

## Monitoring URLs

- **Kafka UI:** <http://localhost:8090>
- **RabbitMQ Console:** <http://localhost:15672> (tradeflow/tradeflow_secret)
- **Zipkin Tracing:** <http://localhost:9411>

---

## Shutdown

```bash
# Stop frontend (Ctrl+C in terminal)
# Stop each Spring Boot service (Ctrl+C in each terminal)

# Stop infrastructure
cd d:\Antigravity_Projects\Trade-Flow-Project
docker-compose down
```

To remove all data volumes:

```bash
docker-compose down -v
```
