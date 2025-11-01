# 🎯 MoodTrip: A Conversational Emotion-Aware Tourism Recommender System

---

## 🟩 1️⃣ Introduction

Hello everyone,  
We are Team 5 and our project is called **MoodTrip**.  
MoodTrip is a conversational and emotion-aware tourism recommender system that helps users find travel routes and matching playlists based on their mood.

- Users can interact through **text**, **voice**, or **images**.
- The system analyzes their emotions using **AI-based emotion recognition APIs**.
- It recommends travel routes via **Outdooractive’s API** and music playlists via **Spotify’s API**.

---

## 🟦 2️⃣ Motivation

Traditional tourism recommender systems often rely on filters like **location**, **time**, or **price**, but they usually ignore emotions — which play a major role in travel decisions.

- Someone who feels tired may want a **peaceful, relaxing trip**.  
- Someone who feels excited may look for an **adventurous challenge**.

We believe travel recommendations should reflect how users *feel*, not just what they *search for*.  
That’s why we built **MoodTrip** — to make trip planning more human-centered, interactive, and emotionally adaptive.

---

## 🟧 3️⃣ Problem Statement and Concept

**Problem Statement:**  
> How can we include emotional understanding in tourism recommendation?

### Our Approach
MoodTrip solves this problem in **three main stages:**

1. **Emotion Detection**  
   - Analyze user input (text, image, voice) through AI APIs or neural networks.  
   - Detect emotions such as *relaxed*, *adventurous*, or *romantic*.

2. **Interactive Dialogue**  
   - Start a short conversation with the user to collect travel details.  
   - Ask for group size, location, and timeframe.

3. **Personalized Recommendation**  
   - Query **Outdooractive’s API** for suitable routes and **Spotify’s API** for matching playlists.  
   - Assign each route an *emotion score* indicating how well it fits the user’s current mood.

✅ In short, **MoodTrip doesn’t just recommend trips — it recommends experiences.**

---

## 🟨 4️⃣ Exploratory Data Analysis (EDA)

Before building the system, we explored the data provided by **Outdooractive** and **Spotify** APIs.

### 🌍 Outdooractive Data

The **FlexView API** provides structured information about hiking and travel routes.  
Each route includes:

| Field          | Description                  |
| -------------- | ---------------------------- |
| `name`         | Route name                   |
| `activitytype` | Type (e.g., hiking, cycling) |
| `difficulty`   | Level of challenge           |
| `duration`     | Average time in hours        |
| `distance`     | Length of route              |
| `region`       | Geographical area            |
| `rating`       | User review score            |

**Example JSON record:**
```json
{
  "name": "Alpine Lake Hike",
  "activitytype": "hiking",
  "difficulty": "easy",
  "duration": 3.5,
  "region": "Bavaria",
  "rating": 4.6
}