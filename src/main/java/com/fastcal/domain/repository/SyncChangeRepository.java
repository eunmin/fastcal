package com.fastcal.domain.repository;

import com.fastcal.domain.model.SyncChange;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncChangeRepository extends ReactiveCrudRepository<SyncChange, Long> {
}
