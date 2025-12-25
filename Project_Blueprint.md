# Technical Implementation Blueprint

# TradeFlow High-Frequency Trading

# Simulation Platform

## Executive Summary

This document constitutes the definitive Technical Implementation Blueprint for **TradeFlow** , a
high-frequency trading (HFT) simulation platform designed to replicate the architectural rigor,
latency sensitivity, and transactional integrity of top-tier financial exchanges. Authored by the
Principal Software Architect, this report is intended for the engineering leads, infrastructure
specialists, and senior developers who will execute the construction of the system.
The financial technology landscape is defined by a dichotomy of requirements: the need for
sub-millisecond execution speeds in order matching and the absolute necessity of ACID
(Atomicity, Consistency, Isolation, Durability) compliance in financial settlement. TradeFlow
addresses these conflicting demands through a **Hybrid Event-Driven Microservices
Architecture**. This architecture segregates the system into a "Fast Path" for market data and
matching, optimized for throughput and Availability/Partition Tolerance (AP), and a "Slow
Path" for ledger management and settlement, optimized for Consistency (CP).
The technology stack is selected based on the bleeding edge of stability as of late 2025. We
utilize **Spring Boot 3.3/3.4** 1 as the backend foundation, leveraging **Java 21 Virtual Threads**
(Project Loom) 3 to solve the "C10K problem" inherent in high-concurrency gateways without
the cognitive complexity of reactive programming. The frontend is built on **React 19** 4 , utilizing
concurrent rendering features to handle massive WebSocket ingress rates without UI freezing.
Distributed transaction management, the most complex aspect of microservices in finance, is
handled via the **Saga Orchestration** pattern.^6 This ensures that operations spanning the
Order Management System (OMS), Wallet Service, and Matching Engine remain eventually
consistent, utilizing **RabbitMQ** for reliable command routing 7 and **Apache Kafka** for
high-throughput event streaming.^8
This blueprint details the architectural patterns, technology selection rationale, database
schema engineering, messaging topology, and implementation strategies required to deliver
TradeFlow as a robust, production-grade simulation platform.

## 1. Architectural Strategy and Design Philosophy

The architectural vision for TradeFlow is predicated on the principle of **Segregation of
Duties**. In a monolithic trading system, a single database transaction might encompass order
placement, balance checks, and matching. In a distributed HFT environment, such coupling is
performance suicide. We must decouple these phases into independent microservices,
communicating primarily through asynchronous messaging, with synchronous calls reserved
strictly for read-heavy operations or immediate feedback loops.

### 1.1 The CAP Theorem in Financial Simulation

We explicitly acknowledge the CAP Theorem (Consistency, Availability, Partition Tolerance)
and apply different trade-offs to different subsystems:
● **Order Matching (AP Focus):** The Matching Engine prioritizes Availability and Partition
Tolerance. It must accept and process orders with minimal latency. We utilize in-memory
data structures and eventual consistency for persistence. If the database is slow, trading
must not stop.
● **Ledger & Settlement (CP Focus):** The Wallet Service prioritizes Consistency above all
else. A user's balance must never be negative, and money must never be created or
destroyed, only transferred. We utilize strict pessimistic locking and ACID-compliant
relational databases here.

### 1.2 Architectural Style: Hybrid Event-Driven Microservices

The system is composed of the following core domains, each serving a distinct role in the
trading lifecycle:

1. **Identity & Access Management (IAM):** Handles authentication (OAuth2/JWT) and user
    session management.
2. **Order Management System (OMS):** The entry point for all trading activity. It handles
    order validation, lifecycle management (Pending -> Open -> Filled/Cancelled), and acts
    as the Saga Orchestrator for trade flows.
3. **Wallet & Ledger Service:** The "source of truth" for user balances. It implements a
    double-entry accounting system to ensure no funds are created or destroyed without a
    tracing transaction.
4. **Matching Engine:** A high-performance, in-memory component responsible for the
    price-time priority algorithm. It matches buy and sell orders and emits "Trade Executed"
    events.
5. **Market Data Service:** Consumes raw trade events to generate aggregated tickers
    (OHLCV - Open, High, Low, Close, Volume) and broadcasts them to frontend clients via
    WebSockets.
6. **Audit & Compliance Service:** An asynchronous observer that logs every state change
    to an immutable append-only store (MongoDB) for regulatory simulation and debugging.

### 1.3 Concurrency Model: The Shift to Virtual Threads

A critical architectural decision for TradeFlow is the adoption of **Java 21 Virtual Threads** ,
fully supported in **Spring Boot 3.3**.^3
Traditionally, Java applications relied on Platform Threads (OS threads), which are expensive
to create (approx. 1MB stack size) and costly to context-switch. In high-concurrency
environments like trading gateways, a "thread-per-request" model inevitably hits resource
ceilings. While Reactive Programming (WebFlux) offered a solution, it introduced significant
code complexity ("callback hell") and debugging challenges.
Virtual Threads offer the best of both worlds: the simplicity of the imperative synchronous
programming style with the scalability of non-blocking I/O. For the Wallet Service and OMS,
which spend significant time waiting for Database I/O (MySQL) and Network I/O
(Kafka/RabbitMQ), Virtual Threads allow the application to handle hundreds of thousands of
concurrent requests without blocking OS threads.
**Implementation Directive:** All Spring Boot services (excluding the CPU-bound Matching
Engine) must enable Virtual Threads. This is configured in application.yml:
YAML
spring:
threads:
virtual:
enabled: true
The implication of this change is profound. We can lower the Tomcat max thread count
significantly, as the "threads" handling requests are no longer one-to-one mapped to OS
threads. However, we must be cautious of "pinning," where a virtual thread is stuck to a carrier
thread, typically caused by synchronized blocks in legacy code.^9

## 2. Technology Stack and Versioning Strategy

To ensure long-term maintainability and performance, the stack is curated based on the
stable releases available in late 2024 and 2025. This selection process is driven by the need
for long-term support (LTS) and compatibility between the extensive Spring ecosystem
components.

### 2.1 Backend Core: Spring Ecosystem

We have selected **Spring Boot 3.3.x** (or 3.4.x if stable) as the foundation.^1 This version

provides the necessary baseline for Jakarta EE 10 and Spring Framework 6.1.
Compatibility Matrix:
Adhering to the Spring Cloud Release Train is critical to prevent dependency hell. Based on
the release trajectory 10, the following combinations are mandated:
**Component Version / Release Train Rationale
Spring Boot** 3.3.x / 3.4.x Latest stable generation
supporting Java 21 Virtual
Threads.
**Spring Cloud** 2023.0.x (Leyton) or
2024.0.x (Moorgate)
Strictly coupled to Boot
version; provides Gateway,
Circuit Breaker.
**Java SDK** OpenJDK 21 LTS Mandatory for Project
Loom (Virtual Threads) and
ZGC.
**Build Tool** Maven 3.9+ Required for newer plugin
architectures.
The decision to strictly avoid Spring Boot 2.x is driven by the End-of-Life (EOL) status of 2.x
lines and the requirement for Jakarta namespaces, which are prerequisites for the latest
Hibernate and Tomcat versions.^2

### 2.2 Data Persistence Layer

```
● Relational Database (Ledger/Orders): MySQL 8.0 / 8.4 LTS.
MySQL is chosen for its robust transaction support (InnoDB engine) and specific
DECIMAL handling capabilities required for financial precision.13 While PostgreSQL is a
valid alternative, MySQL's ubiquity in high-read environments and specific optimizations
for clustered indexing (primary keys) make it suitable for the order ledger.
● NoSQL (Audit): MongoDB 7.0+.
Selected for flexible schema design. Audit logs often vary in structure depending on the
event type (e.g., a "Login" event differs structurally from an "Order Placement" event).
MongoDB's BSON storage allows for efficient querying of these polymorphic
structures.
● Cache & State Store: Redis 7.2.
Redis is indispensable for the "Fast Path." It is used for the Order Book snapshotting,
real-time Ticker storage, and idempotent keys. Its single-threaded nature guarantees
```

```
atomic operations for rate limiting.
```

### 2.3 Messaging & Streaming Infrastructure

```
● Event Streaming: Apache Kafka 3.6+.
Used for high-volume market data (Tickers, Trade Execution Events) and as the durable
"commit log" for the Matching Engine. Kafka's partitioning model allows us to serialize
orders for a specific trading pair (e.g., BTC/USDT) to a specific consumer, guaranteeing
order without global locks.
● Command Messaging: RabbitMQ 3.12+.
Used for transactional messaging between microservices (Saga commands). Unlike
Kafka, RabbitMQ allows for complex routing logic (Direct/Topic exchanges) and individual
message acknowledgment, which is superior for task-based workflows like "Reserve
Funds" where we need to ensure a specific service instance handles a specific
command.
```

### 2.4 Frontend Layer

```
● Library: React 19 (Stable since Dec 2024).
React 19 introduces critical features for HFT dashboards. The useTransition hook allows
us to mark Order Book updates as "non-blocking," ensuring that the UI remains
responsive to user input (e.g., clicking "Buy") even when thousands of market data
updates are flooding in via WebSocket.
● Real-time Transport: WebSocket via SockJS and STOMP.
We layer STOMP (Simple Text Oriented Messaging Protocol) over WebSockets. This
provides a pub/sub semantic (/topic/ticker/BTCUSDT) out of the box, avoiding the need to
invent a custom framing protocol.
```

## 3. Distributed Transaction Management: The Saga

## Pattern

In a monolithic architecture, placing an order and debiting a wallet can be wrapped in a single
database transaction. In Microservices, the Database per Service pattern 6 prevents this.
TradeFlow implements the **Saga Orchestration** pattern to manage this complexity.

### 3.1 Orchestration vs. Choreography

While Choreography (event-based reaction) lowers coupling, it obscures the business
process flow, making debugging "zombie transactions" (where money is reserved but no
order is placed) difficult.^20 Orchestration provides a centralized "Conductor" (The OMS) that
knows the state of every transaction and can explicitly trigger compensating actions. This

reduces the cognitive load on the system and centralizes the logic for failure handling.

### 3.2 The Order Placement Saga Flow

The critical business transaction is the **Limit Buy Order**. The flow is managed by the OMS as
the Orchestrator.

#### Step 1: Initialization

```
● Trigger: User sends POST /orders to OMS.
● Action: OMS creates an Order record in MySQL with status PENDING_VALIDATION. It also
creates a SagaInstance record.^21
```

#### Step 2: Fund Reservation (The Command)

```
● Action: OMS publishes a WalletReserveFundsCommand to RabbitMQ.
● Routing: We use a Direct Exchange. Routing Key: wallet.reserve.
● Payload: Includes sagaId, userId, amount, and currency.^7
```

#### Step 3: Wallet Service Processing

```
● Action: Wallet Service consumes the command.
● Validation: It queries the ledger. If available_balance >= amount:
○ It inserts a ledger_entry of type RESERVE.
○ It publishes a FundsReservedEvent back to the reply queue.
● Failure: If funds are insufficient, it publishes FundsReservationFailedEvent.
```

#### Step 4: Orchestrator Decision

```
● On Success: OMS updates Order status to OPEN and forwards the order to the
Matching Engine (via Kafka topic orders.incoming).
● On Failure: OMS updates Order status to REJECTED. The Saga ends.
```

#### Step 5: Compensation (The Unhappy Path)

```
● Scenario: The Matching Engine rejects the order (e.g., due to a "Market Halted" state or
"Price Band" violation).
● Action: Matching Engine emits OrderRejectedEvent.
● Compensation: OMS (Orchestrator) listens to this event. It recognizes that funds are still
reserved. It creates and sends a WalletReleaseFundsCommand to RabbitMQ.
● Finalization: Wallet Service unlocks the funds. OMS updates Order to CANCELLED.^22
```

### 3.3 State Machine Implementation

The OMS must implement a persisted state machine to track Sagas. This ensures that if the
OMS service crashes mid-transaction, it can resume or roll back upon restart.

**Table: saga_instance** (MySQL)
**Column Type Description**
saga_id UUID Unique correlation ID for
the entire flow.
order_id UUID Business key linking to the
Order table.
current_state VARCHAR STARTED,
FUNDS_RESERVED,
MATCHING_SUBMITTED,
COMPLETED, FAILED.
step_history JSON Log of steps taken: ``
payload JSON Snapshot of the initial
request necessary for
compensation generation.
updated_at DATETIME Timestamp for detecting
"stuck" sagas.
This table acts as the "journal" for the Orchestrator. A background background process
(Scheduled Task) periodically scans for Sagas that have been in an intermediate state (e.g.,
STARTED) for longer than 30 seconds and triggers a recovery or compensation workflow.

## 4. Core Microservices Implementation Detail

### 4.1 Wallet Service: The Guardian of Integrity

The Wallet Service is the most critical component for data integrity. It must prevent
double-spending and race conditions.

#### 4.1.1 Database Schema: Double-Entry Ledger

We strictly avoid a simple balance column that gets updated balance = balance + x. This is
prone to "lost update" anomalies. Instead, we use an event-sourcing style transactional ledger

table.
**Table: wallet**
● wallet_id (PK)
● user_id
● currency (e.g., 'USD', 'BTC')
● created_at
**Table: wallet_transaction (The Ledger)**
● transaction_id (PK)
● wallet_id (FK)
● amount (DECIMAL 20, 8) - Positive for credit, negative for debit.^13
● reference_type (ENUM: DEPOSIT, WITHDRAWAL, TRADE, FEE, RESERVE)
● reference_id (UUID) - Links to Order ID or Transfer ID.
● created_at
To determine a user's balance, we theoretically sum all transactions. However, for
performance, we maintain a wallet_balance table acting as a **materialized view** , updated
transactionally within the same ACID transaction as the ledger insert.
Handling Precision:
We utilize MySQL's DECIMAL(M, D) type. Floating point types (FLOAT, DOUBLE) are strictly
prohibited due to IEEE 754 rounding errors which are unacceptable in finance.
● **Configuration:** DECIMAL(20, 8).^13
○ **20:** Total digits.
○ **8:** Fractional digits (Scale). This supports Bitcoin's "Satoshi" unit (0.00000001) while
allowing for large distinct values.
○ **Storage:** This consumes approximately 9-10 bytes per column, a negligible cost for
the accuracy provided.

#### 4.1.2 Locking Strategy

To prevent race conditions (e.g., two concurrent buy orders trying to spend the same
balance), we employ **Pessimistic Locking** on the wallet row during the reservation phase.
Java
// Spring Data JPA Repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.id = :id")

Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
This query issues a SELECT... FOR UPDATE statement in MySQL. This ensures that once a
thread reads the balance to validate funds, no other thread can modify that wallet until the
current transaction commits or rolls back. While this introduces a serialization point, it is
necessary for strict consistency. Virtual Threads help mitigate the impact of this blocking I/O
on the application server's throughput.

### 4.2 Matching Engine: The Speed Layer

The Matching Engine is a CPU-bound service. Unlike the other I/O-bound services, this does
not benefit from Virtual Threads as heavily. It requires single-threaded execution (per trading
pair) to avoid lock contention and context switching overhead.

#### 4.2.1 Data Structure: Limit Order Book (LOB)

We implement the LOB using **Dual TreeMaps** (Red-Black Trees).
● **Bids (Buy side):** TreeMap<BigDecimal, List<Order>> (Sorted Descending - Highest Bid
First).
● **Asks (Sell side):** TreeMap<BigDecimal, List<Order>> (Sorted Ascending - Lowest Ask
First).
This structure allows $O(\log n)$ insertion, deletion, and lookup. Matching is $O(1)$ at the
best price level (the tip of the tree).

#### 4.2.2 Event Loop and Partitioning

The Matching Engine consumes the orders.incoming Kafka topic.
● **Partitioning Strategy:** The Kafka topic is partitioned by symbol.
○ Hash(BTCUSDT) % 10 partitions = Partition 3.
○ This guarantees that all orders for BTCUSDT arrive at the same consumer instance in
strict order.
○ This eliminates the need for global locks across matching engines; each engine
effectively owns the "world" for the symbols assigned to its partition.^9
**Processing Logic:**

1. **Ingest:** Read Order from Kafka.
2. **Match:** Check against opposite side of the book.
    ○ If match found: Create Trade object, adjust remaining quantities.
    ○ Repeat until order filled or no match possible.
3. **Resting:** If the order is not fully filled, insert the remainder into the LOB.
4. **Emit:** Publish TradeExecutedEvent and OrderBookUpdateEvent to Kafka output topics.

### 4.3 Market Data Service: Ticker Generation

This service consumes TradeExecutedEvent from Kafka and generates BookTicker and OHLCV
(Candlestick) data.

#### 4.3.1 Redis TimeSeries Implementation

For historical charts, we use **RedisTimeSeries**.^25 It is optimized for high-ingestion rates of
timestamped values and provides built-in aggregation functions (e.g., "Give me the average
price over the last 5 minutes").
Key Naming Convention:
We follow the strict convention object-type:id:field.
● Ticker TimeSeries: ticker:BTCUSDT:1m
● Last Price String: price:BTCUSDT
● Order Book Snapshot: orderbook:BTCUSDT
**Optimization:** For the Order Book snapshot in Redis, we use a **Redis Hash**.
● Key: orderbook:BTCUSDT
● Field: Price (Stringified)
● Value: Quantity
● **Why?** Hashes allow us to update a single price level (e.g., "Ask at 50000 changed from
1.0 to 0.5") using HSET without serializing/deserializing the entire book. This saves
significant CPU and bandwidth compared to storing the whole book as a JSON string.^27

#### 4.3.2 WebSocket Broadcast

Using **Spring Websocket** with **STOMP** , this service pushes updates to frontend clients.
● Topic: /topic/ticker/{symbol}
● Payload: JSON adhering to standard industry formats (e.g., Binance API) 29 for developer
familiarity.

## 5. High-Throughput Messaging Configuration

### 5.1 RabbitMQ (Transactional Messaging)

Used for the Saga Orchestration and reliable delivery of commands.
● **Exchange Type:** Direct Exchange (amq.direct) for targeted commands.^18
● **Routing Keys:** wallet.reserve, wallet.release, order.finalize.
● **Durability:** Queues must be durable (durable=true). Messages must be persistent
(deliveryMode=2).

Virtual Thread Listener Configuration:
To maximize throughput on consumers, the SimpleAsyncTaskExecutor with Virtual Threads
enabled must be injected into the RabbitListenerContainerFactory. This is a specific pattern
required for Spring Boot 3.3.
Java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory
connectionFactory) {
SimpleRabbitListenerContainerFactory factory = new
SimpleRabbitListenerContainerFactory();
factory.setConnectionFactory(connectionFactory);
// Enable Virtual Threads for Consumers
SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rabbit-vt-");
executor.setVirtualThreads(true);
factory.setTaskExecutor(executor);
return factory;
}

### 5.2 Apache Kafka Tuning

For the Matching Engine, throughput and backpressure management are paramount. If the
engine falls behind, latency spikes.
Consumer Configuration (application.yml) 31 :
YAML
spring:
kafka:
consumer:

# Max records per poll. Low value prevents processing timeouts

max-poll-records: 500

# Minimum bytes to fetch. Increases batching efficiency to reduce network calls

fetch-min-bytes: 10240

# Max wait if min-bytes not met

fetch-max-wait: 500ms

properties:

# Heartbeat to broker

session.timeout.ms: 45000

# Disable auto-commit to ensure at-least-once processing

enable-auto-commit: false
Backpressure Logic:
The max-poll-records setting is the primary lever for backpressure. If the Matching Engine
takes 1ms to process an order, a batch of 500 takes 500ms. If we set this to 50,000,
processing would take 50 seconds, likely triggering a session.timeout.ms (default 45s) and
causing the broker to think the consumer is dead, triggering a "Rebalance Storm".
Virtual Threads in Kafka:
Similar to RabbitMQ, the ConcurrentKafkaListenerContainerFactory can be configured with a
virtual-thread-enabled executor. However, extreme caution is needed. Some underlying Kafka
client libraries still use synchronized blocks for thread coordination. When a virtual thread hits
a synchronized block, it "pins" the carrier OS thread, potentially negating the benefits. We
recommend profiling the Matching Engine; if pinning is observed, revert to standard Platform
Threads for the Kafka consumer, as the Matching Engine is CPU-bound anyway.

## 6. Frontend Implementation (React 19)

The TradeFlow frontend is a Single Page Application (SPA) built with React 19. It focuses on
rendering high-frequency updates without UI freezing.

### 6.1 WebSocket Integration with React 19

We use @stomp/stompjs 19 wrapped in a custom React Hook. React 19's concurrent rendering
features allow us to process incoming WebSocket messages without blocking user interaction.
Hook: useTradeWebSocket
This hook manages the connection lifecycle, subscription to topics, and automatic
reconnection.
JavaScript
// Conceptual Implementation
import { Client } from '@stomp/stompjs';
import { useState, useEffect, useRef } from 'react';
export const useTradeWebSocket = (url, topic) => {

const = useState(null);
const clientRef = useRef(null);
useEffect(() => {
clientRef.current = new Client({
brokerURL: url,
reconnectDelay: 5000 ,
onConnect: () => {
clientRef.current.subscribe(topic, (message) => {
// React 19 batching handles high frequency updates
setData(JSON.parse(message.body));
});
}
});
clientRef.current.activate();
return () => clientRef.current.deactivate();
}, [url, topic]);
return data;
};

### 6.2 Order Book Rendering & Optimization

Rendering a live Order Book (20+ rows updating 10-50 times/second) is expensive. If every
update forces a DOM re-paint, the browser will freeze.
**React 19 Strategy:**

1. **useTransition:** We wrap state updates from the WebSocket in startTransition. This marks
    the Order Book update as "low priority." If the user types in an input field (high priority),
    React will interrupt the Order Book render to process the keystroke, ensuring the UI feels
    responsive.^5
2. **Virtualization:** We use libraries like react-window to only render the DOM nodes
    currently visible in the viewport, regardless of how deep the order book is.
3. **Throttling:** We implement a throttle in the WebSocket hook. Even if the backend sends
    100 updates/sec, we only commit state updates to React at 20fps (every 50ms). The
    human eye cannot perceive jitter above this rate, and it saves significant CPU.

## 7. Resilience and Fault Tolerance

### 7.1 Circuit Breaker (Resilience4j)

Applied to all synchronous Feign Clients (e.g., OMS querying Wallet balance for display). We

do not want a slow Wallet database to cascade and crash the OMS.
Configuration (application.yml):
We use a count-based sliding window. If 50% of the last 10 requests fail, the circuit opens,
failing fast without waiting for timeouts.
YAML
resilience4j:
circuitbreaker:
instances:
walletService:
registerHealthIndicator: true
slidingWindowSize: 10
minimumNumberOfCalls: 5
permittedNumberOfCallsInHalfOpenState: 3
automaticTransitionFromOpenToHalfOpenEnabled: true
waitDurationInOpenState: 5s
failureRateThreshold: 50

### 7.2 Idempotency Implementation

In a distributed environment, "exactly-once" delivery is impossible; we have "at-least-once"
delivery. Duplicate messages are inevitable (e.g., network ACK failure).
**Strategy:**
● **Consumer Idempotency:** The Wallet Service must track processed message_ids (Saga
IDs).
● **Implementation:** Before processing a ReserveFunds command, the service checks a
processed_messages table (or Redis set).
● **Redis Key:** SETNX processed:saga:{uuid} "1" EX 86400.
○ If SETNX returns 1 (True): Process the command.
○ If SETNX returns 0 (False): The command was already processed. Ack the message
and discard.

## 8. Infrastructure and Deployment (Docker Compose)

For the simulation environment, we use Docker Compose to orchestrate the dependency
graph. Note the usage of specific versions to ensure stability.^14

**docker-compose.yml (Comprehensive Snippet):**
YAML
services:
zookeeper:
image: confluentinc/cp-zookeeper:7.5.
environment:
ZOOKEEPER_CLIENT_PORT: 2181
kafka:
image: confluentinc/cp-kafka:7.5.
depends_on: [zookeeper]
ports: ["9092:9092"]
environment:
KAFKA_ZOOKEEPER_CONNECT: zookeeper:
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:

# Critical for partitioning logic in dev

KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
mysql:
image: mysql:8.
command: --default-authentication-plugin=mysql_native_password
environment:
MYSQL_ROOT_PASSWORD: root
volumes:
-./data/mysql:/var/lib/mysql
redis:
image: redis:7.
ports: ["6379:6379"]
rabbitmq:
image: rabbitmq:3.12-management
ports: ["5672:5672", "15672:15672"]
environment:
RABBITMQ_DEFAULT_USER: "admin"
RABBITMQ_DEFAULT_PASS: "password"

Note on Docker Compose Versions:
The version: '3.8' tag is deprecated in newer Docker Compose V2 specifications. We simply
use the top-level services: key as per modern standards.

## 9. Audit and Compliance (MongoDB)

In regulated trading, every action must be auditable. The **Audit Service** listens to all
exchanges (Kafka & RabbitMQ) via a "Wire Tap" pattern. It consumes copies of all messages
without interfering with the main flow.

### 9.1 Schema Design

We use the OCSF (Open Cybersecurity Schema Framework) or a custom JSON schema in
MongoDB.^15
**Collection: audit_logs**
JSON
{
"_id": "ObjectId",
"timestamp": "ISODate",
"actor": { "user_id": "123", "ip": "10.0.0.1", "session_id": "abc" },
"action": "ORDER_PLACED",
"resource": { "type": "ORDER", "id": "uuid" },
"payload": {... }, // Full content of the command/event
"outcome": "SUCCESS",
"metadata": { "latency_ms": 45 , "node_id": "oms-01" }
}
This allows compliance officers to query: "Show all actions by User X between Time A and
Time B," or "Show all failed trades for symbol BTCUSDT."

### 9.2 Change Streams

We can utilize **MongoDB Change Streams** 39 to build reactive monitoring. For example, a
security monitoring service can watch the audit_logs collection. If it detects more than 5
LOGIN_FAILED events for a single user_id within 1 minute, it can automatically trigger a "Lock
Account" command to the IAM service.

## 10. Conclusion and Operational Roadmap

This blueprint outlines a production-grade architecture for the TradeFlow simulation. By
leveraging **Spring Boot 3.3 with Virtual Threads** , we achieve the I/O throughput required for
massive order ingestion. The **Saga Orchestration pattern** ensures that despite the
distributed nature of the system, financial integrity remains intact. **Kafka** and **Redis** provide
the low-latency data pipelines necessary for the **Matching Engine** and **React 19** frontend.
**Immediate Next Steps for the Engineering Team:**

1. **Skeleton Phase:** Initialize the Spring Boot monorepo with module definitions (OMS,
    Wallet, Matching). Configure Maven BOMs for Spring Cloud 2024.0.
2. **Infrastructure:** Stand up the Docker Compose environment and verify connectivity
    between Kafka, Zookeeper, and MySQL. Verify that Virtual Threads are active by
    inspecting the Thread Dump (looking for VirtualThread instead of http-nio-8080-exec).
3. **Prototype:** Implement the "Wallet Reserve" flow using RabbitMQ and verify the Saga
    State Machine persistence in MySQL.
4. **Performance Test:** create a simple load generator (using JMeter or Gatling) to flood the
    Kafka topics and tune the max.poll.records settings before building complex business
    logic.
This document serves as the primary reference for the construction of TradeFlow. Deviations
from these patterns must be approved by the Architecture Review Board.
