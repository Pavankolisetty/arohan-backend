package com.arohan.lifearea;

import com.arohan.habit.GrowthHabitRepository;
import com.arohan.lifearea.LifeAreaDtos.LifeAreaResponse;
import com.arohan.lifearea.LifeAreaDtos.ThemeSuggestionResponse;
import com.arohan.lifearea.LifeAreaDtos.UpsertLifeAreaRequest;
import com.arohan.shared.ApiException;
import com.arohan.user.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LifeAreaService {
    private final LifeAreaRepository areas;
    private final GrowthHabitRepository habits;
    private final ThemeSuggestionService themes;
    private final UserRepository users;

    public LifeAreaService(LifeAreaRepository areas, GrowthHabitRepository habits,
                           ThemeSuggestionService themes, UserRepository users) {
        this.areas = areas;
        this.habits = habits;
        this.themes = themes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<LifeAreaResponse> list(UUID userId, boolean includeArchived) {
        List<LifeArea> all = areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId);
        Map<UUID, List<LifeArea>> children = new LinkedHashMap<>();
        all.stream().filter(area -> area.getParentId() != null)
            .forEach(area -> children.computeIfAbsent(area.getParentId(), ignored -> new ArrayList<>())
                .add(area));
        return all.stream()
            .filter(area -> area.getParentId() == null)
            .filter(area -> includeArchived || area.getStatus() == LifeAreaStatus.ACTIVE)
            .map(area -> response(area, children, includeArchived))
            .toList();
    }

    @Transactional(readOnly = true)
    public LifeAreaResponse get(UUID userId, UUID id) {
        LifeArea area = require(userId, id);
        Map<UUID, List<LifeArea>> children = new LinkedHashMap<>();
        areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(item -> item.getParentId() != null)
            .forEach(item -> children.computeIfAbsent(item.getParentId(), ignored -> new ArrayList<>())
                .add(item));
        return response(area, children, true);
    }

    @Transactional
    public LifeAreaResponse create(UUID userId, UpsertLifeAreaRequest request) {
        validateParent(userId, null, request.parentId());
        ensureUnique(userId, null, request.parentId(), request.name());
        var suggestion = themes.suggest(request.name());
        LifeArea saved = areas.save(new LifeArea(
            userId,
            request.parentId(),
            request.name().trim(),
            trimToNull(request.description()),
            valueOr(request.colorHex(), suggestion.colorHex()),
            valueOr(request.iconKey(), suggestion.iconKey()),
            valueOr(request.backgroundKey(), suggestion.backgroundKey()),
            trimToNull(request.backgroundImageUrl()),
            request.desiredImportance() == 0 ? 3 : request.desiredImportance(),
            request.positionIndex()
        ));
        return response(saved, Map.of(), true);
    }

    @Transactional
    public LifeAreaResponse update(UUID userId, UUID id, UpsertLifeAreaRequest request) {
        LifeArea area = require(userId, id);
        validateParent(userId, id, request.parentId());
        ensureUnique(userId, id, request.parentId(), request.name());
        var suggestion = themes.suggest(request.name());
        area.update(
            request.parentId(),
            request.name().trim(),
            trimToNull(request.description()),
            valueOr(request.colorHex(), suggestion.colorHex()),
            valueOr(request.iconKey(), suggestion.iconKey()),
            valueOr(request.backgroundKey(), suggestion.backgroundKey()),
            trimToNull(request.backgroundImageUrl()),
            request.desiredImportance() == 0 ? 3 : request.desiredImportance(),
            request.positionIndex()
        );
        return response(area, Map.of(), true);
    }

    @Transactional
    public void archive(UUID userId, UUID id) {
        LifeArea area = require(userId, id);
        area.archive();
        areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(child -> id.equals(child.getParentId()))
            .forEach(LifeArea::archive);
        habits.findAllByUserIdAndLifeAreaId(userId, id).forEach(
            com.arohan.habit.GrowthHabit::pause);
        areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(child -> id.equals(child.getParentId()))
            .forEach(child -> habits.findAllByUserIdAndLifeAreaId(userId, child.getId())
                .forEach(com.arohan.habit.GrowthHabit::pause));
    }

    @Transactional
    public void restore(UUID userId, UUID id) {
        LifeArea area = require(userId, id);
        if (area.getParentId() != null) {
            LifeArea parent = require(userId, area.getParentId());
            if (parent.getStatus() == LifeAreaStatus.ARCHIVED) {
                throw new ApiException(HttpStatus.CONFLICT,
                    "Restore the parent Life Area before restoring this subarea.");
            }
        }
        area.restore();
    }

    @Transactional
    public List<LifeAreaResponse> createFromStarters(UUID userId) {
        var user = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User was not found."));
        Map<String, String> names = Map.of(
            "WELLBEING", "Wellbeing",
            "MINDFULNESS", "Inner stillness",
            "LEARNING", "Learning",
            "FINANCIAL", "Financial clarity",
            "RELATIONSHIPS", "Meaningful relationships",
            "CREATIVE", "Creative expression"
        );
        Set<String> existing = new HashSet<>();
        areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId)
            .forEach(area -> existing.add(area.getName().toLowerCase(Locale.ROOT)));
        int position = (int) areas.countByUserId(userId);
        for (String key : user.getStarterTemplateKeys()) {
            String name = names.get(key);
            if (name == null || existing.contains(name.toLowerCase(Locale.ROOT))) continue;
            var suggestion = themes.suggest(name);
            areas.save(new LifeArea(userId, null, name,
                "A starting point from your Arohan inspiration choices.",
                suggestion.colorHex(), suggestion.iconKey(), suggestion.backgroundKey(),
                null, 3, position++));
        }
        return list(userId, false);
    }

    public ThemeSuggestionResponse suggest(String name) {
        var suggestion = themes.suggest(name);
        return new ThemeSuggestionResponse(suggestion.colorHex(), suggestion.iconKey(),
            suggestion.backgroundKey());
    }

    private LifeAreaResponse response(LifeArea area, Map<UUID, List<LifeArea>> children,
                                      boolean includeArchived) {
        List<LifeAreaResponse> subareas = children.getOrDefault(area.getId(), List.of()).stream()
            .filter(child -> includeArchived || child.getStatus() == LifeAreaStatus.ACTIVE)
            .map(child -> response(child, children, includeArchived))
            .toList();
        return new LifeAreaResponse(
            area.getId(), area.getParentId(), area.getName(), area.getDescription(),
            area.getColorHex(), area.getIconKey(), area.getBackgroundKey(),
            area.getBackgroundImageUrl(), area.getDesiredImportance(), area.getStatus(),
            area.getPositionIndex(), habits.countByUserIdAndLifeAreaId(userId(area), area.getId()),
            subareas, area.getCreatedAt(), area.getUpdatedAt()
        );
    }

    private UUID userId(LifeArea area) {
        return area.getUserId();
    }

    private LifeArea require(UUID userId, UUID id) {
        return areas.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Life Area was not found."));
    }

    private void validateParent(UUID userId, UUID currentId, UUID parentId) {
        if (parentId == null) return;
        if (parentId.equals(currentId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "A Life Area cannot be its own parent.");
        }
        LifeArea parent = require(userId, parentId);
        if (parent.getStatus() == LifeAreaStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Choose an active parent Life Area.");
        }
        UUID cursor = parent.getParentId();
        while (cursor != null) {
            if (cursor.equals(currentId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This parent choice would create a circular Life Area.");
            }
            cursor = require(userId, cursor).getParentId();
        }
    }

    private void ensureUnique(UUID userId, UUID currentId, UUID parentId, String name) {
        boolean duplicate = areas.findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(userId).stream()
            .filter(area -> currentId == null || !area.getId().equals(currentId))
            .filter(area -> java.util.Objects.equals(area.getParentId(), parentId))
            .anyMatch(area -> area.getName().equalsIgnoreCase(name.trim()));
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT,
                "A Life Area with this name already exists at the same level.");
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
