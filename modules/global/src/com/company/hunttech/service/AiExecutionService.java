package com.company.hunttech.service;

import java.util.Map;

/**
 * Единая точка выполнения AI-функций HRM HuntTech.
 *
 * Потребитель передаёт стабильный functionCode и бизнес-контекст; provider, model,
 * prompt, credential source и fallback выбираются централизованно на middleware.
 */
public interface AiExecutionService {
    String NAME = "hunttech_AiExecutionService";

    String executeText(String functionCode, Map<String, Object> context);
}
