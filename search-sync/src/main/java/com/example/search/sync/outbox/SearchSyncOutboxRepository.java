package com.example.search.sync.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchSyncOutboxRepository extends JpaRepository<SearchSyncOutbox, UUID> {
}
