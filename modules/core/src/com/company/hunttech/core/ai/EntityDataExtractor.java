package com.company.hunttech.core.ai;

import com.haulmont.cuba.core.entity.Entity;

import java.util.function.Function;

/**
 * Извлекает значение placeholder-а из сущности.
 * Может делать JPQL-запросы для получения связанных данных.
 */
@FunctionalInterface
public interface EntityDataExtractor extends Function<Entity, String> {
}
