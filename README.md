# ChronoFlux: Live Ops Engine

ChronoFlux is a robust, microservice-based Live Operations (LiveOps) engine designed for modern game development. It allows game administrators to define dynamic JSON schemas, schedule live events (e.g., Flash Sales, Raid Boss spawns, Double XP weekends), and securely deliver event payloads to game clients and external webhooks.

## 🚀 Architecture

ChronoFlux employs a modern, distributed microservice architecture to handle high throughput and ensure resilience.

```mermaid
graph TD
    %% Clients
    Admin[Admin/Dev UI\n(React/Vite)]
    GameClients[Game Clients\n(Unity/Unreal/Simulator)]
    
    %% Ingress
    NGINX[NGINX Reverse Proxy\nAPI Gateway]
    
    %% Services
    AdminAPI[Admin API\n(Spring Boot)]
    EdgeAPI[Edge API\n(Spring Boot)]
    Scheduler[Scheduler Worker\n(Quartz + ShedLock)]
    Webhook[Webhook Worker\n(Spring Boot + RabbitMQ)]
    
    %% Data Stores
    PostgreSQL[(PostgreSQL\nPrimary DB)]
    Redis[(Redis\nCache)]
    RabbitMQ[[RabbitMQ\nMessage Broker]]
    
    %% External
    ExtServer[Mock Game Server\n/ External Webhooks]

    %% Connections
    Admin -->|/api/admin| NGINX
    GameClients -->|/api/v1| NGINX
    
    NGINX --> AdminAPI
    NGINX --> EdgeAPI
    
    AdminAPI --> PostgreSQL
    AdminAPI -.->|Invalidates Cache| Redis
    
    EdgeAPI --> Redis
    EdgeAPI --> PostgreSQL
    
    Scheduler --> PostgreSQL
    Scheduler -.->|Warms Cache| Redis
    Scheduler -->|Triggers Webhooks| RabbitMQ
    
    RabbitMQ --> Webhook
    Webhook --> PostgreSQL
    Webhook --> ExtServer
```

## 🛠️ Technology Stack

| Component | Technology | Purpose |
| --- | --- | --- |
| **Frontend** | React + Vite, Tailwind CSS, TanStack Query | Admin Control Room & Game Client Simulator |
| **Backend Core** | Java 21, Spring Boot 3.4 | Core microservices |
| **Database** | PostgreSQL | Relational data & JSONB for dynamic payloads |
| **Caching** | Redis | Cache-aside for Edge API to survive thundering herds |
| **Distributed Lock** | ShedLock + JDBC | Ensures only 1 Scheduler triggers an event |
| **Message Broker** | RabbitMQ | Async webhook delivery with Dead Letter Queue (DLQ) |
| **Containerization**| Docker & Docker Compose | Local development and deployment orchestration |
| **Testing** | k6, Testcontainers, JUnit | Load testing and integration testing |

## 📡 API Endpoints

### Admin API (Port 8080 internal, `/api/admin/*` via NGINX)
- `GET /schemas` - List all event schemas
- `POST /schemas` - Create a new dynamic JSON schema
- `GET /events` - List all scheduled and active events
- `POST /events` - Schedule an event with a validated JSONB payload
- `POST /webhooks` - Register a webhook for event triggers

### Edge API (Port 8081 internal, `/api/v1/*` via NGINX)
- `GET /events/active` - High-throughput, heavily cached endpoint for game clients to fetch live events.

> 📚 **API Documentation**: Once running, full Swagger UI documentation is available at `http://localhost/api/admin/swagger-ui.html`

## 🏃 How to Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/swarnendu-sekhar-das/ChronoFlux.git
   cd ChronoFlux
   ```

2. **Boot the stack:**
   The entire system (9 containers including DBs, Broker, and Microservices) is orchestrated via Docker Compose.
   ```bash
   docker compose up -d --build
   ```

3. **Access the Application:**
   - **Admin UI & Simulator**: `http://localhost`
   - **RabbitMQ Management**: `http://localhost:15672` (guest / guest)
   - **Swagger UI**: `http://localhost/api/admin/swagger-ui.html`

## 🎮 Demo Scenarios

### 1. The "Thundering Herd" Proof (Redis Cache-Aside)
When a highly anticipated event starts, millions of clients poll the server simultaneously. ChronoFlux handles this via the Edge API and Redis.
**To verify:**
```bash
# Run the k6 load test simulating 5,000 concurrent players
k6 run k6/thundering-herd.js
```
*You will see the 95th percentile response times remain under 50ms without crashing the PostgreSQL database.*

### 2. Distributed Locking (ShedLock)
The Scheduler worker checks for events every 5 seconds. In a distributed environment with multiple instances, we prevent duplicate event triggers using ShedLock.
**To verify:**
1. Scale the scheduler: `docker compose up -d --scale scheduler-worker=3`
2. Create an event in the Admin UI scheduled for 1 minute in the future.
3. Tail the logs: `docker compose logs -f scheduler-worker`
*Result: Only exactly ONE instance will log "Acquired lock for event activation", while the others skip execution.*

### 3. Webhook Retry & Dead Letter Queue (DLQ)
If a webhook target goes down, ChronoFlux retries with exponential backoff before sending the message to a DLQ.
**To verify:**
1. Stop the mock external server: `docker compose stop mock-game-server`
2. Trigger an event.
3. Open RabbitMQ at `localhost:15672`. Watch the `webhook.retry` queue increment.
4. After 3 failures, the message automatically moves to the `webhook.dlq` for manual inspection.
