package com.company.itpearls;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks entities created during integration tests and soft-deletes them in reverse order.
 * Required because ItpearlsTestContainer uses the same PostgreSQL database as local dev (context.xml).
 */
public class TestEntityTracker {

    private final DataManager dataManager;
    private final List<CleanupEntry> entries = new ArrayList<>();

    public TestEntityTracker(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public <T extends Entity<UUID>> T track(T entity) {
        entries.add(new CleanupEntry(entity.getClass(), entity.getId()));
        return entity;
    }

    public void cleanup() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            CleanupEntry entry = entries.get(i);
            try {
                Entity<UUID> entity = dataManager.load(entry.entityClass)
                        .id(entry.id)
                        .view("_minimal")
                        .one();
                dataManager.remove(entity);
            } catch (Exception ignored) {
                // already soft-deleted or removed by the test
            }
        }
        entries.clear();
    }

    private static class CleanupEntry {
        final Class<? extends Entity> entityClass;
        final UUID id;

        CleanupEntry(Class<? extends Entity> entityClass, UUID id) {
            this.entityClass = entityClass;
            this.id = id;
        }
    }
}
