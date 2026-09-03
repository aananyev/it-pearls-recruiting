package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultInt;

/**
 * Конфигурация персонализации AI-ответов данными вкладки «Обо мне» (UserAiProfile).
 * Значения хранятся в SYS_CONFIG (SourceType.DATABASE) — единый источник для core
 * и web тиров (план персонализации §6.2, консистентность «факт = preview»).
 */
@Source(type = SourceType.DATABASE)
public interface HunttechAiPersonalizationConfig extends Config {

    /**
     * Бюджет пользовательского контекст-блока в system prompt, code points.
     * Диапазон 4000–6000; рост лимита растит promptTokens и стоимость каждого
     * персонализированного вызова. Единый для фактического исполнения и preview.
     */
    @Property("hunttech.ai.userContextLimit")
    @DefaultInt(4000)
    int getUserContextLimit();

    /**
     * Единая точка резолва лимита для исполнения и preview: некорректное (≤ 0)
     * значение даёт дефолт 4000, нижняя граница 4000 enforced. Единый резолв
     * обязаны использовать и core-исполнение, и web-предпросмотр.
     */
    default int getUserContextLimitOrDefault() {
        int configured = getUserContextLimit();
        return configured > 0 ? Math.max(4000, configured) : 4000;
    }
}
