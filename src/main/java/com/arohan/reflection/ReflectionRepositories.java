package com.arohan.reflection;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    Optional<JournalEntry> findByIdAndUserId(UUID id, UUID userId);
    List<JournalEntry> findAllByUserIdAndEntryDateBetweenOrderByPinnedDescEntryDateDescCreatedAtDesc(
        UUID userId, LocalDate from, LocalDate to);
}

interface JournalTagRepository extends JpaRepository<JournalTag, UUID> {
    Optional<JournalTag> findByIdAndUserId(UUID id, UUID userId);
    Optional<JournalTag> findByUserIdAndNameIgnoreCase(UUID userId, String name);
    List<JournalTag> findAllByUserIdOrderByNameAsc(UUID userId);
}
