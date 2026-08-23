package com.company.hunttech.service;

import java.util.Date;
import java.util.List;

/**
 * Сервис массовой загрузки гео-данных (страны, регионы) через провайдеры.
 * Обеспечивает deduplication по ISO кодам и скачивание флагов/гербов в BLOB.
 */
public interface GeoBulkLoaderService {

    String NAME = "hunttech_GeoBulkLoaderService";

    /**
     * Массовая загрузка всех стран: restcountries.com (основной) + локальный справочник + провайдеры.
     * Повторные вызовы не создают дубликатов (проверка по ISO-2).
     *
     * @return текстовая сводка результата (для вывода в UI/REST)
     */
    String loadAllCountries();

    /**
     * Массовая загрузка всех регионов России (85 субъектов).
     * Использует GeoDataProvider (Geoapify boundaries API level=1 для RU).
     * Повторные вызовы не создают дубликатов (проверка по ISO code).
     *
     * @return текстовая сводка результата
     */
    String loadAllRegionsForRussia();

    /**
     * Результат массовой загрузки.
     */
    class LoadResult {
        private int created;
        private int updated;
        private int skipped;
        private int emblemsSaved;
        private Date startedAt;
        private Date finishedAt;
        private final List<String> errors = new java.util.ArrayList<>();

        public void incrementCreated() { created++; }
        public void incrementUpdated() { updated++; }
        public void incrementSkipped() { skipped++; }
        public void incrementEmblemsSaved() { emblemsSaved++; }
        public void addError(String entity, String error) { errors.add(entity + ": " + error); }

        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public int getEmblemsSaved() { return emblemsSaved; }
        public Date getStartedAt() { return startedAt; }
        public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
        public Date getFinishedAt() { return finishedAt; }
        public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
        public List<String> getErrors() { return errors; }
        public long getDurationMs() {
            return finishedAt != null && startedAt != null ? finishedAt.getTime() - startedAt.getTime() : 0;
        }
    }
}