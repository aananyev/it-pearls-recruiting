package com.company.hunttech.service;

/**
 * Сервис массовой загрузки гео-данных (страны) через провайдеры.
 * Обеспечивает deduplication по ISO кодам и скачивание флагов в BLOB.
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
}