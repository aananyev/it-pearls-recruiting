package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.haulmont.cuba.core.entity.FileDescriptor;

public interface CompanyRequisitesIngestService {
    String NAME = "hunttech_CompanyRequisitesIngestService";
    String FUNCTION_COMPANY_REQUISITES_PARSE_JSON = "COMPANY_REQUISITES_PARSE_JSON";

    /**
     * Извлечение текста из файла (PDF, DOC, DOCX, RTF, PAGES, TXT и др.)
     */
    String extractTextFromFile(FileDescriptor fileDescriptor);

    /**
     * Извлечение текста по веб-ссылке
     */
    String extractTextFromUrl(String url);

    /**
     * AI-парсинг неструктурированного текста реквизитов или карточки предприятия
     */
    CompanyRequisitesParsedData parseRequisites(String rawText);

    /**
     * Поиск или создание персоны Генерального директора в справочнике «Люди»
     */
    Person resolveOrCreateDirector(CompanyRequisitesParsedData data);

    /**
     * Применение распарсенных реквизитов к объекту компании (включая привязку директора)
     */
    Company applyRequisitesToCompany(Company company, CompanyRequisitesParsedData data);
}
