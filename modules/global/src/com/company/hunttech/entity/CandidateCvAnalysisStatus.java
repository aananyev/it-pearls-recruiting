package com.company.hunttech.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;

/**
 * Перечисление состояний фонового анализа навыков резюме (Candidate Skills Enrichment).
 */
public enum CandidateCvAnalysisStatus implements EnumClass<Integer> {

    /** Новое CV, анализ навыков ещё не выполнялся */
    NOT_ANALYZED(10),

    /** CV находится в процессе обработки воркером прямо сейчас */
    PROCESSING(20),

    /** Анализ успешно выполнен, результат актуален (хеш текста и версия AI совпадают) */
    FRESH(30),

    /** Текст CV изменился или версия системного AI-промпта обновилась — требуется повторный анализ */
    STALE(40),

    /** Произошла временная ошибка провайдера/сети, запланирован повторный запуск (backoff) */
    RETRY(50),

    /** Превышено максимальное число повторов или произошла неустранимая ошибка */
    ERROR(60),

    /** Текст CV пуст или непригоден для смыслового анализа навыков */
    SKIPPED(70);

    private final Integer id;

    CandidateCvAnalysisStatus(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static CandidateCvAnalysisStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (CandidateCvAnalysisStatus value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
