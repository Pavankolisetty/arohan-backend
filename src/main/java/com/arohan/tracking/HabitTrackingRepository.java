package com.arohan.tracking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitTrackingRepository extends JpaRepository<HabitTrackingEntry, UUID> {
    Optional<HabitTrackingEntry> findByUserIdAndHabitIdAndScheduledDate(
        UUID userId, UUID habitId, LocalDate scheduledDate);
    List<HabitTrackingEntry> findAllByUserIdAndScheduledDateBetweenOrderByScheduledDateAsc(
        UUID userId, LocalDate from, LocalDate to);
    List<HabitTrackingEntry> findAllByUserIdAndHabitIdAndScheduledDateBetween(
        UUID userId, UUID habitId, LocalDate from, LocalDate to);
    long countByUserIdAndHabitIdAndScheduledDateBetweenAndStatusIn(
        UUID userId, UUID habitId, LocalDate from, LocalDate to,
        java.util.Collection<TrackingStatus> statuses);
}
