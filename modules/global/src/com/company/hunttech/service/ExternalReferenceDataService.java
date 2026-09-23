package com.company.hunttech.service;

import com.company.hunttech.dto.integration.ReferenceFilterDto;
import com.company.hunttech.dto.integration.ReferenceListResponseDto;

/**
 * Сервис предоставления справочных данных для внешних интегрируемых систем (BL-2026-029).
 * <p>
 * Предоставляет нормализованные и защищенные данные справочников HRM HuntTech:
 * города, должности, грейды, дерево навыков, типы взаимодействий и страны.
 */
public interface ExternalReferenceDataService {

    String NAME = "hunttech_ExternalReferenceDataService";

    /**
     * Возвращает список городов с поддержкой поиска и пагинации.
     *
     * @param filter параметры фильтрации (поиск, лимит, оффсет)
     * @return DTO со списком элементов справочника
     */
    ReferenceListResponseDto getCities(ReferenceFilterDto filter);

    /**
     * Возвращает список должностей / позиций.
     *
     * @param filter параметры фильтрации
     * @return DTO со списком элементов справочника
     */
    ReferenceListResponseDto getPositions(ReferenceFilterDto filter);

    /**
     * Возвращает список квалификационных грейдов (Junior, Middle, Senior и др.).
     *
     * @param filter параметры фильтрации
     * @return DTO со списком элементов справочника
     */
    ReferenceListResponseDto getGrades(ReferenceFilterDto filter);

    /**
     * Возвращает список типов взаимодействий с кандидатами (Звонок, Собеседование, Оффер и др.).
     *
     * @param filter параметры фильтрации
     * @return DTO со списком элементов справочника
     */
    ReferenceListResponseDto getInteractionTypes(ReferenceFilterDto filter);

    /**
     * Возвращает список навыков из дерева навыков SkillTree (с поддержкой фильтрации по родителю).
     *
     * @param filter параметры фильтрации (parentId, поиск)
     * @return DTO со списком элементов справочника
     */
    ReferenceListResponseDto getSkills(ReferenceFilterDto filter);

    /**
     * Возвращает список стран.
     *
     * @param filter параметры фильтрации
     * @return DTO со списком элементов справочника
     */
    ReferenceListResponseDto getCountries(ReferenceFilterDto filter);
}
