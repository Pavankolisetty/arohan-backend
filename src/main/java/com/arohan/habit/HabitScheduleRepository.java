package com.arohan.habit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitScheduleRepository extends JpaRepository<HabitSchedule, UUID> {
    Optional<HabitSchedule> findByHabitId(UUID habitId);
}
