package com.arohan.habit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthHabitRepository extends JpaRepository<GrowthHabit, UUID> {
    Optional<GrowthHabit> findByIdAndUserId(UUID id, UUID userId);
    Optional<GrowthHabit> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);
    List<GrowthHabit> findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(UUID userId);
    List<GrowthHabit> findAllByUserIdAndLifeAreaId(UUID userId, UUID lifeAreaId);
    long countByUserIdAndLifeAreaId(UUID userId, UUID lifeAreaId);
}
