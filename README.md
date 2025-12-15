# Moodtrip 

##  Tech Stack

This project adopts a modern architecture with a separated frontend and backend, prioritizing high performance and a reactive user experience.

###  Backend - `moodtrip-backend`
- **Language**: Java 21
- **Framework**: Spring Boot 3.5.7 (WebFlux Reactive Stack)
- **Database**: PostgreSQL (R2DBC Reactive Driver)
- **AI Integration**: Spring AI (DeepSeek Model)
- **Security**: Spring Security + JWT
- **Build Tool**: Gradle


###  Frontend - `moodtrip-frontend`
- **Framework**: React 19
- **Build Tool**: Vite 7 (TypeScript)
- **Styling**: Tailwind CSS 4, Radix UI
- **Map Components**: Leaflet, React Leaflet
- **State & Routing**: React Router DOM 7, React Hooks
- **AI SDK**: Vercel AI SDK

##  Getting Started

### Prerequisites
- **Java**: JDK 21+
- **Node.js**: v20+
- **Docker**: Required to run the PostgreSQL database

### 1. Start Database
Navigate to the backend directory and use Docker Compose to start the database service:

```bash
cd moodtrip-backend
docker-compose up -d
```
> **Note**: Please ensure the `.env` file in the root directory is correctly configured with your database environment variables (e.g., `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`).

### 2. Start Backend Service
```bash
./gradlew bootRun
```

### 3. Start Frontend Application
Open a new terminal window and navigate to the frontend directory:
```bash
cd ../moodtrip-frontend
npm install
npm run dev
```
Open your browser and visit `http://localhost:5173` to experience the application.

## 📂 Project Structure

```
repo2/
├── moodtrip-backend/      # Spring Boot Backend Source
│   ├── src/main/java/     # Java Source Code
│   ├── src/main/resources/# Configuration & DB Migrations (Liquibase)
│   ├── build.gradle       # Gradle Build Configuration
│   └── docker-compose.yml # Database Container Configuration
├── moodtrip-frontend/     # React Frontend Source
│   ├── src/               # Components, Pages, and Hooks
│   ├── components/        # UI Components (Radix + Tailwind)
│   ├── package.json       # Dependency Management
│   └── vite.config.ts     # Vite Configuration
└── README.md              # Project Documentation
```

---
**RecSys Lab WS 2025 - Team 5**
