# Noise Robustness Evaluation (Empirical Bayes vs. Over-Confident)

## Experiment Setup
- **Scenario**: User gives consistently high ratings (5.0) for **10 rounds**, establishing a "High Preference" profile.
- **Noise Injection**: In **Round 11**, the user suddenly gives a **1.0 (Low)** rating. This simulates a one-off anomaly (e.g., bad mood, accidental click).
- **Comparison**:
    - **Empirical Bayes (EB)**: Adaptive $\alpha$ (starts low, grows slow).
    - **Over-Confident**: Fixed $\alpha = 0.9$ (always trusts local feedback).

## Results: Reaction to Noise

| Round | Event | EB Prediction | Over-Confident Prediction |
|-------|-------|---------------|---------------------------|
| 10    | Rating 5.0 | 1.74          | 3.30                      |
| 11    | **NOISE 1.0** | 1.87          | 3.45                      |
| 12    | Rating 5.0 | **1.86**      | **3.22**                  |
| 13    | Rating 5.0 | 2.00          | 3.38                      |

## Analysis
1.  **Impact of Noise (Round 11 $\to$ 12)**:
    - **EB**: The prediction dropped from 1.87 to 1.86 (-0.01).
    - **Over-Confident**: The prediction dropped from 3.45 to 3.22 (-0.23).
    - **Conclusion**: The Over-Confident model reacted **23x more violently** to the single noise point. It "panicked" and significantly downgraded the user's preference.

2.  **Recovery (Round 12 $\to$ 13)**:
    - **EB**: Immediately resumed its upward trend (1.86 $\to$ 2.00). The noise was effectively filtered out as a minor outlier.
    - **Over-Confident**: Had to spend the next round "repairing" the damage it caused itself.

## Summary
While a high fixed $\alpha$ (0.9) appears to learn faster initially (reaching 3.30 vs 1.74 at Round 10), it pays a heavy price in **stability**. Empirical Bayes provides a "Shock Absorber" effect: as long as the user's history supports a high preference, a single low rating is treated with healthy skepticism.
