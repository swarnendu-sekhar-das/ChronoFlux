# ChronoFlux

ChronoFlux is a robust, microservices-based Gaming Live-Ops Engine designed to manage real-time game operations, scheduled events, and webhooks at scale.

## System Architecture

The project is built on a modern microservices architecture comprising several core components:

- **Admin API**: A Spring Boot backend powering the admin dashboard. It handles system configurations, management, and authentication via Google OAuth.
- **Edge API**: A high-performance, player-facing API designed for low latency.
- **Scheduler Worker**: A background service that manages scheduled events, pushing tasks to the message broker for asynchronous processing.
- **Webhook Worker**: A worker that listens for events from the message broker and dispatches reliable webhooks to external services or game servers.
- **Mock Game Server**: A testing utility that simulates external game servers for end-to-end integration testing.
- **Frontend**: A Single Page Application (SPA) providing a rich user interface for administrators to monitor and manage the live-ops engine.

## Tech Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.1
- **Database Migrations**: Flyway
- **Data Mapping**: MapStruct
- **Testing**: Testcontainers (Integration)
- **Build Tool**: Maven

### Infrastructure
- **Relational Database**: PostgreSQL 16
- **Caching & KV Store**: Redis 7
- **Message Broker**: RabbitMQ 3
- **Containerization**: Docker & Docker Compose

### Frontend
- **Environment**: Node 24
- **Build Tool**: Vite
- **Web Server**: Nginx (for serving the production build)

## Getting Started

### Prerequisites
- Docker and Docker Compose installed on your local machine.
- Available ports for the services: 5432 (Postgres), 6379 (Redis), 5672/15672 (RabbitMQ), 8080 (Admin API), 8081 (Edge API), 9090 (Mock Game Server), and 80 (Frontend).

### Deployment
1. Ensure the `.env` file is properly configured with your PostgreSQL credentials and Google OAuth Client ID.
2. Run the following command from the root directory to build and start the entire suite of services:
   ```bash
   docker-compose up -d --build
   ```
3. The services will be accessible at:
   - Frontend: `http://localhost`
   - Admin API Health: `http://localhost:8080/api/admin/actuator/health`
   - Edge API Health: `http://localhost:8081/api/v1/actuator/health`
   - RabbitMQ Management: `http://localhost:15672` (guest/guest)
   - Mock Game Server: `http://localhost:9090`
