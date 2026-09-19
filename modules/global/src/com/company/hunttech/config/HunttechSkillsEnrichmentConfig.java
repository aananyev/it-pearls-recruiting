package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Конфигурация службы фонового определения навыков кандидатов (Candidate Skills Enrichment).
 * Значения хранятся в SYS_CONFIG (SourceType.DATABASE) — доступны core и web тирам,
 * обновляются администратором из UI мониторинга на лету.
 */
@Source(type = SourceType.DATABASE)
public interface HunttechSkillsEnrichmentConfig extends Config {

    /**
     * Флаг включения фоновой службы обогащения навыков.
     */
    @Property("hunttech.skillsEnrichment.enabled")
    @DefaultBoolean(false)
    boolean getEnabled();

    void setEnabled(boolean value);

    /**
     * Пауза между AI-запросами в секундах (безопасный троттлинг).
     */
    @Property("hunttech.skillsEnrichment.delayBetweenRequestsSec")
    @DefaultInt(15)
    int getDelayBetweenRequestsSec();

    void setDelayBetweenRequestsSec(int value);

    /**
     * Максимальное количество кандидатов, обрабатываемых в час (защита от перегрузки).
     */
    @Property("hunttech.skillsEnrichment.maxCandidatesPerHour")
    @DefaultInt(120)
    int getMaxCandidatesPerHour();

    void setMaxCandidatesPerHour(int value);

    /**
     * Максимальное количество повторных попыток при ошибке провайдера перед переводом в ERROR.
     */
    @Property("hunttech.skillsEnrichment.maxRetries")
    @DefaultInt(4)
    int getMaxRetries();

    void setMaxRetries(int value);

    /**
     * Размер порции выборки кандидатов из базы при поиске очереди.
     */
    @Property("hunttech.skillsEnrichment.batchFetchSize")
    @DefaultInt(10)
    int getBatchFetchSize();

    void setBatchFetchSize(int value);

    /**
     * Политика работы исключительно с бесплатными моделями (FREE_ONLY).
     */
    @Property("hunttech.skillsEnrichment.freeOnly")
    @DefaultBoolean(true)
    boolean getFreeOnly();

    void setFreeOnly(boolean value);

    /**
     * Стабильный код AI-функции для фонового анализа (по умолчанию SKILLS_EXTRACT_BACKGROUND).
     */
    @Property("hunttech.skillsEnrichment.aiFunctionCode")
    @DefaultString("SKILLS_EXTRACT_BACKGROUND")
    String getAiFunctionCode();

    void setAiFunctionCode(String value);

    /**
     * Таймаут в минутах, после которого задача в статусе PROCESSING считается зависшей.
     */
    @Property("hunttech.skillsEnrichment.stuckTimeoutMinutes")
    @DefaultInt(15)
    int getStuckTimeoutMinutes();

    void setStuckTimeoutMinutes(int value);
}
