# ChronoFlux: Live Ops Engine

ChronoFlux is a robust, microservice-based Live Operations (LiveOps) engine designed for modern game development. It allows game administrators to define dynamic JSON schemas, schedule live events (e.g., Flash Sales, Raid Boss spawns, Double XP weekends), and securely deliver event payloads to game clients and external webhooks.

## 🚀 Architecture

ChronoFlux employs a modern, distributed microservice architecture to handle high throughput and ensure resilience.

```mermaid
graph TD
    %% Clients
    Admin["Admin/Dev UI\n(React/Vite)"]
    GameClients["Game Clients\n(Unity/Unreal/Simulator)"]
    
    %% Ingress
    NGINX["NGINX Reverse Proxy\nAPI Gateway"]
    
    %% Services
    AdminAPI["Admin API\n(Spring Boot)"]
    EdgeAPI["Edge API\n(Spring Boot)"]
    Scheduler["Scheduler Worker\n(Spring Boot + Polling)"]
    Webhook["Webhook Worker\n(Spring Boot + RabbitMQ)"]
    
    %% Data Stores
    PostgreSQL[("PostgreSQL\nPrimary DB")]
    Redis[("Redis\nCache")]
    RabbitMQ[["RabbitMQ\nMessage Broker"]]
    
    %% External
    ExtServer["Mock Game Server\n/ External Webhooks"]

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
| **Caching** | Redis & Nginx | Proxy caching and cache-aside for Edge API to survive thundering herds |
| **Message Broker** | RabbitMQ | Async webhook delivery with Database-backed retry mechanism |
| **Containerization**| Docker & Docker Compose | Local development and deployment orchestration |
| **Testing** | k6, Testcontainers, JUnit | Load testing and integration testing |
