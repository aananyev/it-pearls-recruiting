package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Конфигурация службы фонового определения контактов кандидатов (Candidate Contact Enrichment).
 * Значения хранятся в SYS_CONFIG (SourceType.DATABASE) — доступны core и web тирам,
 * обновляются администратором из UI мониторинга на лету.
 */
@Source(type = SourceType.DATABASE)
public interface HunttechContactEnrichmentConfig extends Config {

    /**
     * Флаг включения фоновой службы обогащения контактов.
     */
    @Property("hunttech.contactEnrichment.enabled")
    @DefaultBoolean(true)
    boolean getEnabled();

    void setEnabled(boolean value);

    /**
     * Пауза между AI-запросами в секундах (безопасный троттлинг).
     */
    @Property("hunttech.contactEnrichment.delayBetweenRequestsSec")
    @DefaultInt(15)
    int getDelayBetweenRequestsSec();

    void setDelayBetweenRequestsSec(int value);

    /**
     * Максимальное количество кандидатов, обрабатываемых в час (защита от перегрузки).
     */
    @Property("hunttech.contactEnrichment.maxCandidatesPerHour")
    @DefaultInt(120)
    int getMaxCandidatesPerHour();

    void setMaxCandidatesPerHour(int value);

    /**
     * Максимальное количество повторных попыток при ошибке провайдера перед переводом в ERROR.
     */
    @Property("hunttech.contactEnrichment.maxRetries")
    @DefaultInt(4)
    int getMaxRetries();

    void setMaxRetries(int value);

    /**
     * Размер порции выборки кандидатов из базы при поиске очереди.
     */
    @Property("hunttech.contactEnrichment.batchFetchSize")
    @DefaultInt(10)
    int getBatchFetchSize();

    void setBatchFetchSize(int value);

    /**
     * Политика работы исключительно с бесплатными моделями (FREE_ONLY) или разрешение платных.
     */
    @Property("hunttech.contactEnrichment.freeOnly")
    @DefaultBoolean(true)
    boolean getFreeOnly();

    void setFreeOnly(boolean value);

    /**
     * Стабильный код AI-функции для фонового анализа контактов (по умолчанию CONTACTS_EXTRACT_BACKGROUND).
     */
    @Property("hunttech.contactEnrichment.aiFunctionCode")
    @DefaultString("CONTACTS_EXTRACT_BACKGROUND")
    String getAiFunctionCode();

    void setAiFunctionCode(String value);

    /**
     * Таймаут в минутах, после которого задача в статусе PROCESSING считается зависшей.
     */
    @Property("hunttech.contactEnrichment.stuckTimeoutMinutes")
    @DefaultInt(15)
    int getStuckTimeoutMinutes();

    void setStuckTimeoutMinutes(int value);
}
