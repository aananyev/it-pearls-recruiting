package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;

import java.util.List;

/**
 * AI-сервис поиска информации о компаниях в открытых источниках интернета,
 * извлечения реквизитов, контактов, описания и автозаполнения карточки компании.
 */
public interface CompanySearchAiService {
    String NAME = "hunttech_CompanySearchAiService";
    String FUNCTION_COMPANY_WEB_SEARCH_PARSE_JSON = "COMPANY_WEB_SEARCH_PARSE_JSON";

    /**
     * Поиск информации о компании в интернете по наименованию и/или ИНН.
     * Возвращает список найденных кандидатов организаций для выбора пользователем.
     *
     * @param companyName наименование компании или бренда
     * @param inn         ИНН организации (опционально)
     * @return список найденных вариантов с полными реквизитами и описанием
     */
    List<CompanyRequisitesParsedData> searchCompanyInWeb(String companyName, String inn);

    /**
     * AI-парсинг произвольного текста реквизитов или карточки предприятия.
     *
     * @param rawText текст реквизитов
     * @return структурированные данные
     */
    CompanyRequisitesParsedData parseCompanyData(String rawText);

    /**
     * Поиск или создание персоны Генерального директора в справочнике «Люди».
     *
     * @param data распарсенные данные
     * @return привязанная или созданная персона
     */
    Person resolveOrCreateDirector(CompanyRequisitesParsedData data);

    /**
     * Применение всех найденных реквизитов, контактов, адресов и описания к сущности Company.
     *
     * @param company целевая компания
     * @param data    выбранный вариант данных
     * @return обновленная компания
     */
    Company applyCompanyData(Company company, CompanyRequisitesParsedData data);
}
