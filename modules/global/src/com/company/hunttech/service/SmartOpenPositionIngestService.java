package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.entity.FileDescriptor;

import java.util.Collection;

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
     * Канонический алгоритм генерации названия вакансии (в соответствии с алгоритмом кнопки «Генерировать» из OpenPositionEdit).
     * Формат: [Grade] [PositionRu] / [PositionEn] ([Project], [City])
     * Примечание: безопасная реализация сервиса обрабатывает случай отсутствия города (для удаленного формата работы по всей РФ)
     * и отсутствие EN-наименования должности, исключая генерацию пустой строки или "null".
     */
    String generateCanonicalVacancyName(Grade grade, Position positionType, Project project, City city, Collection<City> additionalCities);

    /**
     * Создание и сохранение новой открытой вакансии, привязка проекта, грейда, навыков и города.
     */
    SmartOpenPositionIngestResult createOpenPosition(SmartOpenPositionParsedData data, ExtUser recruiter);
}
