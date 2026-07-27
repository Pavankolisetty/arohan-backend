package com.arohan.lifearea;

import com.arohan.habit.GrowthHabitRepository;
import com.arohan.habit.GrowthHabitService;
import com.arohan.habit.HabitDtos.ScheduleRequest;
import com.arohan.habit.HabitDtos.UpsertHabitRequest;
import com.arohan.habit.HabitKind;
import com.arohan.habit.ScheduleType;
import com.arohan.habit.TrackingMethod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StarterProvisioningService {
    private static final Set<DayOfWeek> WEEKDAYS = Set.of(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    private static final Map<String, StarterDefinition> STARTERS = Map.of(
        "WELLBEING", new StarterDefinition(
            "Wellbeing", "Move for ten minutes",
            "Support your energy with a little gentle movement.",
            "Stand up and begin with one comfortable movement."),
        "MINDFULNESS", new StarterDefinition(
            "Inner stillness", "Pause and breathe",
            "Create a calm moment before the day carries you forward.",
            "Take three slow breaths."),
        "LEARNING", new StarterDefinition(
            "Learning", "Read for ten minutes",
            "Keep curiosity active through a small, regular practice.",
            "Open something you want to learn from."),
        "FINANCIAL", new StarterDefinition(
            "Financial clarity", "Review today's spending",
            "Build awareness of where your money supported your life.",
            "Look at today's transactions."),
        "RELATIONSHIPS", new StarterDefinition(
            "Meaningful relationships", "Reach out to someone",
            "Keep an important relationship warm through small moments of care.",
            "Choose one person and send a short message."),
        "CREATIVE", new StarterDefinition(
            "Creative expression", "Create for ten minutes",
            "Give your ideas a small, regular place to become visible.",
            "Open your creative tool and make one small mark.")
    );

    private final LifeAreaRepository areas;
    private final GrowthHabitRepository habits;
    private final GrowthHabitService habitService;
    private final ThemeSuggestionService themes;

    public StarterProvisioningService(LifeAreaRepository areas, GrowthHabitRepository habits,
                                      GrowthHabitService habitService,
                                      ThemeSuggestionService themes) {
        this.areas = areas;
        this.habits = habits;
        this.habitService = habitService;
        this.themes = themes;
    }

    @Transactional
    public void provision(UUID userId, Set<String> starterKeys, LocalDate startDate) {
        Map<String, LifeArea> areasByName = new LinkedHashMap<>();
        areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId)
            .forEach(area -> areasByName.put(area.getName().toLowerCase(Locale.ROOT), area));

        int areaPosition = areasByName.size();
        for (String key : starterKeys) {
            StarterDefinition definition = STARTERS.get(key);
            if (definition == null) continue;

            LifeArea area = areasByName.get(definition.areaName().toLowerCase(Locale.ROOT));
            if (area == null) {
                var suggestion = themes.suggest(definition.areaName());
                area = areas.save(new LifeArea(
                    userId, null, definition.areaName(),
                    "A gentle starting direction chosen during your Arohan welcome.",
                    suggestion.colorHex(), suggestion.iconKey(), suggestion.backgroundKey(),
                    null, 3, areaPosition++
                ));
                areasByName.put(definition.areaName().toLowerCase(Locale.ROOT), area);
            }

            boolean alreadyExists = habits.findAllByUserIdAndLifeAreaId(userId, area.getId())
                .stream()
                .anyMatch(habit -> habit.getName().equalsIgnoreCase(definition.habitName()));
            if (alreadyExists) continue;

            int habitPosition = (int) habits.countByUserIdAndLifeAreaId(userId, area.getId());
            habitService.create(userId, new UpsertHabitRequest(
                UUID.randomUUID(), HabitKind.GROWTH_HABIT, area.getId(),
                definition.habitName(), definition.purpose(), TrackingMethod.CHECKBOX,
                null, null,
                definition.smallStart(), definition.smallStart(),
                null, null, null, null,
                "If today becomes busy, continue at the next weekday moment.",
                habitPosition,
                new ScheduleRequest(
                    ScheduleType.SELECTED_WEEKDAYS, startDate, WEEKDAYS,
                    null, null, null, null
                )
            ));
        }
    }

    private record StarterDefinition(
        String areaName,
        String habitName,
        String purpose,
        String smallStart
    ) {}
}
