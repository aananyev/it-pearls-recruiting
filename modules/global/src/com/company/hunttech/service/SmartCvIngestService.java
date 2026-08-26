package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.entity.FileDescriptor;

import java.util.UUID;

/**
 * Сервис умной загрузки, AI-анализа и создания кандидатов из файлов резюме произвольной формы.
 */
public interface SmartCvIngestService {
    String NAME = "hunttech_SmartCvIngestService";

    /**
     * Извлечение чистого текста из файла резюме (PDF, DOCX, DOC, RTF, Pages).
     */
    String extractTextFromFile(FileDescriptor fileDescriptor, byte[] fileBytes);

    /**
     * AI-парсинг текста резюме в структурированный JSON.
     */
    SmartCvParsedData parseCvText(String rawText);

    /**
     * Поиск существующего кандидата по ключевым реквизитам (телефон, email, telegram, ФИО + дата рождения).
     */
    JobCandidate findDuplicate(SmartCvParsedData data);

    /**
     * Создание нового кандидата, резюме, подчиненных справочников и первичного взаимодействия.
     */
    SmartCvIngestResult createNewCandidate(SmartCvParsedData data, FileDescriptor fileDescriptor, FileDescriptor faceImage, ExtUser recruiter);

    /**
     * Привязка новой версии резюме к уже существующему кандидату (без затирания старых данных).
     */
    SmartCvIngestResult attachCvToExistingCandidate(UUID existingCandidateId, SmartCvParsedData data, FileDescriptor fileDescriptor, FileDescriptor faceImage, ExtUser recruiter);

    /**
     * Применение распознанных данных к существующей карточке резюме CandidateCV (заполнение полей, мест работы JobHistory, образования).
     */
    SmartCvIngestResult applyParsedDataToCandidateCv(CandidateCV candidateCv, SmartCvParsedData data, ExtUser recruiter);
}
