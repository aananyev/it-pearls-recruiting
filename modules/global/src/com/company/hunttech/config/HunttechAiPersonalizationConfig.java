package com.company.hunttech.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultInt;

/**
 * Конфигурация персонализации AI-ответов данными вкладки «Обо мне» (UserAiProfile).
 * Значения читаются из app.properties через CUBA Config (план персонализации §6.2).
 */
@Source(type = SourceType.APP)
public interface HunttechAiPersonalizationConfig extends Config {

    /**
     * Бюджет пользовательского контекст-блока в system prompt, code points.
     * Диапазон 4000–6000; рост лимита растит promptTokens и стоимость каждого
     * персонализированного вызова. Единый для фактического исполнения и preview.
     */
    @Property("hunttech.ai.userContextLimit")
    @DefaultInt(4000)
    int getUserContextLimit();
}
