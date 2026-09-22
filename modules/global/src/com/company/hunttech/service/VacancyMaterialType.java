package com.company.hunttech.service;

import java.io.Serializable;

/**
 * Бизнес-типы материалов вакансии, генерируемых единым vacancy AI-фасадом.
 */
public enum VacancyMaterialType implements Serializable {
    CHECKLIST,
    SEARCH_MAP,
    INTERVIEW_PLAN
}
