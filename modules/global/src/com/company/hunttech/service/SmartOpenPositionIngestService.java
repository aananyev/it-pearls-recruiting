package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.OpenPosition;
import com.haulmont.cuba.core.entity.FileDescriptor;

/**
 * Сервис умной загрузки, AI-распознавания и создания открытых вакансий (OpenPosition) из произвольного описания.
 */
public interface SmartOpenPositionIngestService {
    String NAME = "hunttech_SmartOpenPositionIngestService";

    /**
     * Извлечение чистого текста из файла описания вакансии (PDF, DOCX, DOC, RTF, TXT).
     */
    String extractTextFromFile(FileDescriptor fileDescriptor, byte[] fileBytes);

    /**
     * AI-парсинг и структурирование текста вакансии.
     */
    SmartOpenPositionParsedData parseVacancyText(String rawText);

    /**
     * Поиск существующей похожей вакансии в базе данных.
     */
    OpenPosition findDuplicate(SmartOpenPositionParsedData data);

    /**
     * Создание и сохранение новой открытой вакансии, привязка проекта, грейда, навыков и города.
     */
    SmartOpenPositionIngestResult createOpenPosition(SmartOpenPositionParsedData data, ExtUser recruiter);
}
