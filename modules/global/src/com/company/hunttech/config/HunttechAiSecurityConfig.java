package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Серверная конфигурация защиты корпоративных AI credentials.
 *
 * Реальное значение hunttech.ai.encryptionKey не хранится в Git. Оно задаётся
 * администратором окружения в конфигурации приложения; пустое значение блокирует
 * создание/использование корпоративных секретов, но не мешает запуску приложения.
 */
@Source(type = SourceType.APP)
public interface HunttechAiSecurityConfig extends Config {

    @Property("hunttech.ai.encryptionKey")
    @DefaultString("")
    String getEncryptionKey();

    /** Previous key is temporary and exists only during a controlled rotation window. */
    @Property("hunttech.ai.previousEncryptionKey")
    @DefaultString("")
    String getPreviousEncryptionKey();
}
