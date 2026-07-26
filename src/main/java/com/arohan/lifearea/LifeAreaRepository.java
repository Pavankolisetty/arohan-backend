package com.arohan.lifearea;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeAreaRepository extends JpaRepository<LifeArea, UUID> {
    Optional<LifeArea> findByIdAndUserId(UUID id, UUID userId);
    List<LifeArea> findAllByUserIdOrderByPositionIndexAscCreatedAtAsc(UUID userId);
    long countByUserId(UUID userId);
}

