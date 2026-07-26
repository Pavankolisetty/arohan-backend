package com.arohan.lifearea;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ThemeSuggestionService {
    private static final List<ThemeRule> RULES = List.of(
        new ThemeRule(List.of("health", "fitness", "wellbeing", "body", "sleep", "vitality", "movement"),
            "#D87867", "vitality", "sunrise"),
        new ThemeRule(List.of("spiritual", "mindful", "meditation", "prayer", "inner"),
            "#776AA8", "stillness", "dusk"),
        new ThemeRule(List.of("finance", "money", "wealth", "saving"),
            "#B7842C", "finance", "golden-flow"),
        new ThemeRule(List.of("learn", "study", "education", "skill", "read"),
            "#397E9A", "learning", "open-sky"),
        new ThemeRule(List.of("family", "friend", "relationship", "love", "social"),
            "#B55F73", "connection", "warmth"),
        new ThemeRule(List.of("career", "work", "professional", "business"),
            "#4A6F7C", "craft", "horizon"),
        new ThemeRule(List.of("creative", "art", "music", "write", "hobby"),
            "#A65F8E", "creative", "aurora")
    );

    public Suggestion suggest(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return RULES.stream()
            .filter(rule -> rule.keywords().stream().anyMatch(normalized::contains))
            .findFirst()
            .map(rule -> new Suggestion(rule.color(), rule.icon(), rule.background()))
            .orElse(new Suggestion("#4E8669", "growth", "meadow"));
    }

    private record ThemeRule(List<String> keywords, String color, String icon,
                             String background) {}

    public record Suggestion(String colorHex, String iconKey, String backgroundKey) {}
}
