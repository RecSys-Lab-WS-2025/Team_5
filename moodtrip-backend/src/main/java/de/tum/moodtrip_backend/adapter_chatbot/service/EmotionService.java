package de.tum.moodtrip_backend.adapter_chatbot.service;

import de.tum.moodtrip_backend.adapter_chatbot.mapper.EmotionMapper;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class EmotionService {

    private static final String EMOTION_DETECTION_PROMPT = """
            You are an emotion classifier. Given a single user message (any language), produce a compact single-line JSON with:
            - a confidence score in [0,1] for every label in the Mood Spectrum,
            - the most likely label,
            - a success flag indicating whether you are confident enough in your diagnosis,
            - and a content field that is either the emotion label (when confident) or a short follow-up message to the user (when not confident).
            
            Rules
            - Translate internally if needed; consider explicit feeling words, tone, idioms, emojis, and context.
            - Respect negation (e.g., “not sad” ≠ SAD).
            - Compute independent confidences in [0,1] per label, then renormalize so all scores sum to 1. If no affect is present, allocate most probability to NEUTRAL.
            - Round all scores to 3 decimals.
            - Do NOT include markdown, code fences, or explanations.
            - Output must be valid single-line JSON only.
            
            Decision & conversational behavior
            - Let top_score be the highest score among all labels.
            - If top_score >= 0.7 AND the message contains meaningful linguistic content (not just numbers, punctuation, or random characters) AND clearly expresses some affect, set "success":true.
              - Then set "content" to a short human-readable summary of the dominant emotion, e.g., "sad", "very anxious", "calm and content". You may base this on top_label.
            - If the message is extremely short (e.g., "123", "...", "?", "ok") or consists only of numbers, symbols, or random character sequences and does not clearly express a feeling, treat it as "no clear emotion detected": set "success":false, assign the highest probability to NEUTRAL, and use "content" to politely ask the user (in the same language as the input) for more detail or clarification about how they feel.
            - In all other cases (e.g., ambiguous or mixed emotions), set "success":false.
              - Then set "content" to a short reply asking the user (in the same language as the input) for more detail or clarification about how they feel, e.g., "Könntest du ein bisschen genauer beschreiben, wie du dich fühlst?" or "Can you tell me a bit more about how you’re feeling?".
            
            Output keys
            - "scores": object mapping LABEL → score (UPPERCASE labels exactly as listed)
            - "top_label": the LABEL with the highest score (ties broken by precedence below)
            - "top_score": the score of top_label
            - "success": true or false as defined above
            - "content":
                - if success = true: short emotion description (e.g., "sad", "calm and hopeful")
                - if success = false: short follow-up message to the user asking for more input
            
            Tie-break precedence (from strongest to weakest when scores are equal):
            OVERWHELMED > ANXIOUS > STRESSED > ANGRY > FRUSTRATED > SAD > LONELY > TIRED > BORED > CONFUSED > NEUTRAL > CURIOUS > NOSTALGIC > HOPEFUL > GRATEFUL > CONTENT > CALM > ENERGIZED > JOYFUL.
            
            Mood Spectrum (label → what ideas to look for → example keywords/phrases)
            JOYFUL → high-energy positive affect, celebration, delight → happy, joyful, thrilled, ecstatic, delighted, amazing, great news
            ENERGIZED → activation/motivation to act → energized, pumped, motivated, ready, fired up, productive
            CALM → low-arousal positive/peaceful state → calm, relaxed, peaceful, serene, at ease, unwinding, tranquil
            CONTENT → quiet satisfaction/okayness → content, satisfied, fine, okay, all good, can’t complain
            HOPEFUL → optimism about future outcomes → hopeful, optimistic, looking forward, confident it will work out
            GRATEFUL → appreciation/thankfulness → grateful, thankful, blessed, appreciate, lucky
            CURIOUS → seeking to know/explore → curious, interested, wondering, exploring, want to learn
            NOSTALGIC → longing for the past/bittersweet memories → nostalgic, reminisce, miss the old days, memories, bittersweet
            NEUTRAL → factual/impersonal/no clear affect → just info, matter-of-fact, schedule details, no feelings indicated
            CONFUSED → uncertainty/lack of understanding → confused, unsure, don’t understand, unclear, mixed up
            BORED → low interest/stimulation → bored, meh, dull, monotonous, nothing to do
            TIRED → fatigue/low energy → tired, exhausted, sleepy, drained, burnt out, need rest
            LONELY → social disconnection → lonely, alone, isolated, nobody, miss people
            SAD → low mood/grief → sad, down, blue, unhappy, heartbroken, crying
            ANXIOUS → fear/worry/unease → anxious, worried, nervous, panic, dread, on edge
            STRESSED → pressure/strain/tension (often work/time) → stressed, under pressure, tense, too many deadlines
            FRUSTRATED → blocked/irritated by obstacles → frustrated, annoyed, irritated, fed up, stuck
            ANGRY → strong displeasure/resentment/injustice → angry, mad, pissed, furious, rage, unfair, hate
            OVERWHELMED → “too much to handle”/cognitive-emotional overload → overwhelmed, can’t cope, too much, drowning, everything at once, meltdown
            
            Output format (single line JSON):
            - When the model is confident (success = true), for example:
            {"scores":{"JOYFUL":0.000,"ENERGIZED":0.000,"CALM":0.000,"CONTENT":0.000,"HOPEFUL":0.000,"GRATEFUL":0.000,"CURIOUS":0.000,"NOSTALGIC":0.000,"NEUTRAL":1.000,"CONFUSED":0.000,"BORED":0.000,"TIRED":0.000,"LONELY":0.000,"SAD":0.000,"ANXIOUS":0.000,"STRESSED":0.000,"FRUSTRATED":0.000,"ANGRY":0.000,"OVERWHELMED":0.000},"top_label":"NEUTRAL","top_score":1.000,"success":true,"content":"neutral"}
            - When the model is not confident (success = false), for example:
            {"scores":{"JOYFUL":0.050,"ENERGIZED":0.020,"CALM":0.050,"CONTENT":0.050,"HOPEFUL":0.050,"GRATEFUL":0.020,"CURIOUS":0.100,"NOSTALGIC":0.010,"NEUTRAL":0.500,"CONFUSED":0.100,"BORED":0.020,"TIRED":0.010,"LONELY":0.005,"SAD":0.005,"ANXIOUS":0.005,"STRESSED":0.005,"FRUSTRATED":0.005,"ANGRY":0.005,"OVERWHELMED":0.005},"top_label":"NEUTRAL","top_score":0.500,"success":false,"content":"Can you tell me a bit more about how you’re feeling?"}
            """;

    private final DeepSeekChatModel chatModel;

    public EmotionService(DeepSeekChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public Mono<EmotionResult> extractEmotion(String message) {
        var prompt = new Prompt(
                List.of(
                        new SystemMessage(EMOTION_DETECTION_PROMPT),
                        new UserMessage(message)
                )
        );

        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .filter(result -> !result.isBlank())
                .switchIfEmpty(Mono.error(new RuntimeException("AI returned empty response")))
                .map(EmotionMapper::fromJson);
    }
}