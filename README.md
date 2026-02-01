# MoodTrip: An Emotion-Aware Tourism Recommender System

MoodTrip is an intelligent itinerary planner that transforms your **emotional state** into personalized travel plans. Unlike traditional systems that focus only on location and budget, MoodTrip understands how you *feel* and curates a multi-day journey that matches your mood—whether you're stressed and need nature, or energized and seek adventure.

## 🌟 Key Features

*   **🧠 Emotion-Aware**: Uses Large Language Models (DeepSeek) to extract a normalized emotion vector from your natural conversation.
*   **🗺️ Personalized Itineraries**: Generates 3 distinct route strategies to fit your needs:
    *   **Balanced**: A mix of popularity and mood alignment.
    *   **Your Picks**: Heavily biased towards your explicit category interests.
    *   **Discovery**: Optimizes for diversity using MMR (Maximal Marginal Relevance) to help you explore the unknown.
*   **🎵 Atmospheric Enrichment** (need authorization): Integrates with **Spotify** to generate accompanying playlists that match the route's emotional vibe.
*   **📍 Trusted Data**: Built on **OpenStreetMap (OSM)** with strict quality filtering and Wikipedia enrichment.
*   **📈 Continuous Learning**: Uses **Empirical Bayes Shrinkage** to rapidly adapt to your preferences from user feedback (ratings), solving the cold-start problem.

---


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

## 🚀 Quick Start

Get the application running in minutes!

### 1. Environment Setup
Copy the example environment file to both backend and frontend directories:

```bash
cp .env.example moodtrip-backend/.env
cp .env.example moodtrip-frontend/.env
```

> **Important**: Open the `.env` files and fill in your `DEEPSEEK_API_KEY` and Spotify credentials (`SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`). The application requires these to function correctly.

### 2. Start Services

**Terminal 1: Database & Backend**
```bash
cd moodtrip-backend
docker-compose up -d postgres   # Start PostgreSQL
./gradlew bootRun               # Start Backend Server
```

**Terminal 2: Frontend**
```bash
cd moodtrip-frontend
npm install
npm run dev
```

Visit [http://localhost:5173](http://localhost:5173) to start your Moodtrip!

## 📋 Prerequisites
- **Java**: JDK 21+
- **Node.js**: v20+
- **Docker**: Required to run the PostgreSQL database

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
