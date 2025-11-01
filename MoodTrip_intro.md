# 🎯 MoodTrip: A Conversational Emotion-Aware Tourism Recommender System

---

## 🟩 1️⃣ Introduction

Hello everyone,  
We are Team 5 and our project is called **MoodTrip**.  
MoodTrip is a **conversational, multimodal, and emotion-aware tourism recommender system** that helps users find travel routes and music playlists that fit their current mood.

Unlike traditional recommender systems that only ask users *“Where do you want to go?”* or *“When are you free?”*, our system goes one step further and asks *“How do you feel today?”*  

The key idea is simple: travel decisions are deeply emotional, and we aim to design a recommender system that understands those emotions.  

Users can communicate with MoodTrip through **text**, **voice**, or **images**.  
Our backend uses **AI emotion recognition APIs** to interpret the user’s feelings and then recommends personalized travel routes through **Outdooractive’s API**, along with **Spotify playlists** that match their emotional tone.


---

## 🟦 2️⃣ Motivation

Tourism recommender systems are now common on platforms such as TripAdvisor, Airbnb, and Google Travel. However, most of these systems depend on explicit search filters like **destination, budget, or time frame**.  

While useful, these filters fail to capture the **emotional motives** behind travel. For example:

- Someone who feels **stressed or overworked** may want a peaceful escape into nature.  
- Someone who feels **energetic and adventurous** might look for hiking or mountain biking routes.  
- Someone who feels **romantic or nostalgic** may prefer scenic or historical places.  

Current systems cannot identify or respond to these psychological cues.  
That’s the motivation for our work:  
to create a **human-centered recommender** that integrates emotional understanding into the travel planning process.

In addition, the rise of **multimodal interaction** — speech, images, and text — allows users to express preferences more naturally.  
For example, a user could upload a picture of a mountain and say “I want something that feels like this.”  
Our system interprets both the visual and linguistic input to recommend similar experiences.

Ultimately, we aim to enhance personalization, user engagement, and satisfaction by letting the recommender system “understand” how the traveler feels before suggesting where to go.


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
### ⚙️ Conceptual Workflow

```text
┌──────────────────────────────┐
│        User Input            │
│ (Text / Voice / Image)       │
└───────────────┬──────────────┘
                ↓
┌──────────────────────────────┐
│   Emotion Recognition         │
│ (AI API / Self-trained Model) │
└───────────────┬──────────────┘
                ↓
┌──────────────────────────────┐
│   Interactive Dialogue        │
│       (Chatbot Layer)         │
└───────────────┬──────────────┘
                ↓
┌──────────────────────────────┐
│   Data Retrieval              │
│ (Outdooractive + Spotify APIs)│
└───────────────┬──────────────┘
                ↓
┌──────────────────────────────┐
│   Emotion Scoring & Ranking   │
└───────────────┬──────────────┘
                ↓
┌──────────────────────────────┐
│ Personalized Recommendation  │
│          Output              │
└──────────────────────────────┘

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