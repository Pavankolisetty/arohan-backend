package com.arohan.reflection;

import static com.arohan.reflection.ReflectionDtos.*;

import com.arohan.habit.GrowthHabit;
import com.arohan.habit.GrowthHabitRepository;
import com.arohan.lifearea.LifeArea;
import com.arohan.lifearea.LifeAreaRepository;
import com.arohan.shared.ApiException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflectionService {
    private final JournalEntryRepository entries;
    private final JournalTagRepository tags;
    private final LifeAreaRepository areas;
    private final GrowthHabitRepository habits;

    public ReflectionService(JournalEntryRepository entries, JournalTagRepository tags,
                             LifeAreaRepository areas, GrowthHabitRepository habits) {
        this.entries = entries;
        this.tags = tags;
        this.areas = areas;
        this.habits = habits;
    }

    @Transactional(readOnly = true)
    public List<ReflectionResponse> list(UUID userId, LocalDate from, LocalDate to, String query,
                                         ReflectionType type, UUID lifeAreaId, UUID habitId,
                                         UUID tagId, Boolean pinned) {
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(89) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The beginning date must be before the ending date.");
        }
        if (safeFrom.isBefore(safeTo.minusYears(2))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a reflection window of two years or less.");
        }
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return entries.findAllByUserIdAndEntryDateBetweenOrderByPinnedDescEntryDateDescCreatedAtDesc(
                userId, safeFrom, safeTo).stream()
            .filter(entry -> type == null || entry.getEntryType() == type)
            .filter(entry -> lifeAreaId == null || lifeAreaId.equals(entry.getLifeAreaId()))
            .filter(entry -> habitId == null || habitId.equals(entry.getHabitId()))
            .filter(entry -> tagId == null || entry.getTagIds().contains(tagId))
            .filter(entry -> pinned == null || entry.isPinned() == pinned)
            .filter(entry -> needle.isEmpty() || searchable(entry).contains(needle))
            .map(entry -> response(userId, entry))
            .toList();
    }

    @Transactional
    public ReflectionResponse create(UUID userId, ReflectionRequest request) {
        validate(userId, request);
        JournalEntry entry = new JournalEntry(userId);
        entry.update(request);
        return response(userId, entries.save(entry));
    }

    @Transactional
    public ReflectionResponse update(UUID userId, UUID id, ReflectionRequest request) {
        validate(userId, request);
        JournalEntry entry = ownedEntry(userId, id);
        entry.update(request);
        return response(userId, entry);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        entries.delete(ownedEntry(userId, id));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags(UUID userId) {
        return tags.findAllByUserIdOrderByNameAsc(userId).stream().map(this::tagResponse).toList();
    }

    @Transactional
    public TagResponse createTag(UUID userId, TagRequest request) {
        String name = request.name().strip();
        return tags.findByUserIdAndNameIgnoreCase(userId, name)
            .map(this::tagResponse)
            .orElseGet(() -> tagResponse(tags.save(new JournalTag(
                userId, name, request.colorHex().toUpperCase(Locale.ROOT)))));
    }

    @Transactional
    public void deleteTag(UUID userId, UUID id) {
        JournalTag tag = tags.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That tag could not be found."));
        boolean used = entries.findAllByUserIdAndEntryDateBetweenOrderByPinnedDescEntryDateDescCreatedAtDesc(
            userId, LocalDate.of(1970, 1, 1), LocalDate.now().plusYears(1)).stream()
            .anyMatch(entry -> entry.getTagIds().contains(id));
        if (used) {
            throw new ApiException(HttpStatus.CONFLICT, "Remove this tag from its reflections before deleting it.");
        }
        tags.delete(tag);
    }

    private void validate(UUID userId, ReflectionRequest request) {
        boolean hasText = has(request.title()) || has(request.content()) || has(request.wins())
            || has(request.friction()) || has(request.nextAdjustment()) || has(request.smallCommitment());
        if (!hasText && request.moodScore() == null && request.energyScore() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Add a thought, a mood, or an energy check-in before saving.");
        }
        if (request.entryType() == ReflectionType.HABIT_NOTE && request.habitId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose the Growth Habit this reflection belongs to.");
        }
        if (request.entryType() == ReflectionType.LIFE_AREA_NOTE && request.lifeAreaId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose the Life Area this reflection belongs to.");
        }
        if (request.entryType() == ReflectionType.WEEKLY_REVIEW) {
            if (request.periodStart() == null || request.periodEnd() == null
                || request.periodStart().isAfter(request.periodEnd())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid week for this review.");
            }
        }
        if (request.lifeAreaId() != null) ownedArea(userId, request.lifeAreaId());
        if (request.habitId() != null) {
            GrowthHabit habit = ownedHabit(userId, request.habitId());
            if (request.lifeAreaId() != null && !request.lifeAreaId().equals(habit.getLifeAreaId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "That habit belongs to a different Life Area.");
            }
        }
        if (request.tagIds() != null) {
            request.tagIds().forEach(tagId -> tags.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "One selected tag is unavailable.")));
        }
    }

    private ReflectionResponse response(UUID userId, JournalEntry entry) {
        Map<UUID, JournalTag> tagMap = tags.findAllByUserIdOrderByNameAsc(userId).stream()
            .collect(Collectors.toMap(JournalTag::getId, Function.identity()));
        LifeArea area = entry.getLifeAreaId() == null ? null
            : areas.findByIdAndUserId(entry.getLifeAreaId(), userId).orElse(null);
        GrowthHabit habit = entry.getHabitId() == null ? null
            : habits.findByIdAndUserId(entry.getHabitId(), userId).orElse(null);
        List<TagResponse> entryTags = entry.getTagIds().stream()
            .map(tagMap::get).filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(JournalTag::getName))
            .map(this::tagResponse).toList();
        return new ReflectionResponse(entry.getId(), entry.getEntryType(), entry.getTitle(),
            entry.getContent(), entry.getEntryDate(), entry.getLifeAreaId(),
            area == null ? null : area.getName(), entry.getHabitId(),
            habit == null ? null : habit.getName(), entry.getMoodScore(), entry.getEnergyScore(),
            entry.isPinned(), entry.getPeriodStart(), entry.getPeriodEnd(), entry.getWins(),
            entry.getFriction(), entry.getNextAdjustment(), entry.getSmallCommitment(), entryTags,
            entry.getCreatedAt(), entry.getUpdatedAt());
    }

    private JournalEntry ownedEntry(UUID userId, UUID id) {
        return entries.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That reflection could not be found."));
    }
    private LifeArea ownedArea(UUID userId, UUID id) {
        return areas.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "That Life Area is unavailable."));
    }
    private GrowthHabit ownedHabit(UUID userId, UUID id) {
        return habits.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "That Growth Habit is unavailable."));
    }
    private TagResponse tagResponse(JournalTag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColorHex());
    }
    private boolean has(String value) { return value != null && !value.isBlank(); }
    private String searchable(JournalEntry entry) {
        return String.join(" ", List.of(
            safe(entry.getTitle()), safe(entry.getContent()), safe(entry.getWins()),
            safe(entry.getFriction()), safe(entry.getNextAdjustment()), safe(entry.getSmallCommitment())
        )).toLowerCase(Locale.ROOT);
    }
    private String safe(String value) { return value == null ? "" : value; }
}
