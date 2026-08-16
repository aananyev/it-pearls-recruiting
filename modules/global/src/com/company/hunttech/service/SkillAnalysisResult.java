package com.company.hunttech.service;

import com.company.hunttech.entity.SkillTree;

import java.io.Serializable;
import java.util.List;

/**
 * Результат анализа навыков: найденные навыки справочника + метаданные AI-выполнения.
 *
 * <p>Контракт пользовательской нотификации об AI-операциях
 * (docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md): сервис возвращает
 * не только навыки, но и {@link AiExecutionResult} — модель, провайдера и собственника
 * API, если анализ реально выполнен нейросетью. Если AI недоступен и сработал
 * классический словарный fallback, {@link #getAiExecution()} равен {@code null} —
 * экран не показывает нотификацию «обработано ИИ» (честная семантика).</p>
 */
public class SkillAnalysisResult implements Serializable {

    private final List<SkillTree> skills;
    private final AiExecutionResult aiExecution;

    private SkillAnalysisResult(List<SkillTree> skills, AiExecutionResult aiExecution) {
        this.skills = skills;
        this.aiExecution = aiExecution;
    }

    public static SkillAnalysisResult of(List<SkillTree> skills, AiExecutionResult aiExecution) {
        return new SkillAnalysisResult(skills, aiExecution);
    }

    /** Найденные навыки справочника {@link SkillTree} (без дубликатов, в порядке обнаружения). */
    public List<SkillTree> getSkills() {
        return skills;
    }

    /**
     * Метаданные AI-выполнения (модель, провайдер, собственник API) или {@code null},
     * если анализ выполнен классическим словарным поиском без нейросети.
     */
    public AiExecutionResult getAiExecution() {
        return aiExecution;
    }

    /** {@code true}, если анализ реально выполнен нейросетью через AI Control Plane. */
    public boolean isAiUsed() {
        return aiExecution != null;
    }
}
