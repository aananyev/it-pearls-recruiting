# Архитектура единого модуля «Умная загрузка резюме» (`SmartCvIngestService` & `SmartCvUploadScreen`)

## 1. Назначение и бизнес-требования

Модуль **«Умная загрузка резюме»** в HRM HuntTech обеспечивает сквозной автоматизированный процесс приёма, извлечения текста, AI-парсинга, строгой проверки уникальности кандидатов, предотвращения дубликатов и сохранения кандидатов и их резюме в систему.

### Ключевые требования:
1. **Поддержка источников резюме:**
   * **Файлы:** форматы `PDF` (Apache PDFBox), `DOCX` (Apache POI XWPF), `DOC` (Apache POI HWPF), `RTF` (RTFEditorKit), `PAGES` (Apple Pages zip / Preview.pdf), `TXT` (UTF-8);
   * **Текст / RichText:** прямая вставка текста резюме через визуальный редактор (вкладка «Вставить текст»);
   * **Интернет / URL:** импорт и парсинг резюме по прямой web-ссылке через Jsoup (вкладка «Загрузить по ссылке»).
2. **Единая кодовая база (Single Source of Truth):**
   * Вся логика извлечения текста, AI-структурирования, поиска дубликатов и создания сущностей (`JobCandidate`, `CandidateCV`, `JobHistory`, `CandidateSkill`, `Company`, `IteractionList`) вынесена в единый сервисный слой `SmartCvIngestService` (модуль `core`) и универсальный мастер `SmartCvUploadScreen` (модуль `web`).
   * Исключено дублирование логики: форма «Реестр кандидатов» (`JobCandidateReestr`) и форма «Реестр резюме» (`CandidateCVReestrBrowse`) вызывают один и тот же диалоговый мастер по единому контракту.
3. **Строгая политика предотвращения дублирования карточек кандидатов:**
   * **В системе запрещено создание необоснованных дубликатов кандидатов**.
   * Многоуровневый алгоритм поиска дубликатов в БД:
     1. Нормализованный номер телефона (последние 10 цифр) с поддержкой масок форматирования (`+7 (999) 111-22-33`, `8-999-111-2233`, `9991112233`);
     2. Адрес электронной почты (Email) без учёта регистра;
     3. Никнейм Telegram (с префиксом `@` и без него);
     4. ФИО кандидата с проверкой перестановки имени и фамилии.
   * Если найден существующий кандидат:
     - Интерфейс выводит предупреждающий блок `duplicateBox` с подробной информацией о существующей записи (ФИО, телефон, email, автор записи) и явным вопросом рекрутеру.
     - Основное приоритетное действие: **«Не дублировать (привязать резюме к кандидату)»** (`attachDuplicateBtn`). В этом случае новая карточка кандидата **НЕ создаётся**, а создаётся новая версия резюме (`CandidateCV`), дополняются недостающие контакты и сохраняются места работы `JobHistory`.
     - При попытке принудительного дублирования (`createNewAnywayBtn`) отображается модальный диалог подтверждения с предупреждением о запрете дублирования в системе, где по умолчанию фокус установлен на отказ от дублирования.
4. **Безопасность и отказоустойчивость:**
   * Повреждённые, пустые (0 байт) и битые файлы не приводят к падению JVM или ошибкам приложения;
   * Очистка ответов LLM от Markdown-тегов ````json ... ````;
   * Безопасное аварийное восстановление при обрыве токенов генерации.

---

## 2. Архитектурная схема взаимодействия

```mermaid
graph TD
    A[Пользователь / Рекрутер] -->|Клик 'Умная загрузка'| B[SmartCvUploadScreen (DIALOG)]
    
    subgraph UI Layer (modules/web)
        B --> B1[Вкладка 1: Загрузить файл\nPDF, DOCX, DOC, RTF, PAGES, TXT]
        B --> B2[Вкладка 2: Вставить текст\nRichTextArea]
        B --> B3[Вкладка 3: Загрузить по ссылке\nJsoup Web-Scraper]
        B --> B4[Превью распознанных данных\nФИО, Опыт, Навыки, Саммари]
        B --> B5[Блок проверки уникальности duplicateBox]
    end
    
    subgraph Core Layer (modules/core)
        B1 & B2 & B3 -->|extractTextFromFile & parseCvText| C[SmartCvIngestServiceBean]
        C --> D[AiExecutionService\nФункция CV_SMART_PARSE_JSON v3]
        C --> E[Движок дедупликации findDuplicate\nТелефоны с масками, Email, TG, ФИО]
        C --> F[Транзакционный коммит\ncreateNewCandidate / attachCvToExistingCandidate]
    end
    
    subgraph Database Layer
        F --> G[(PostgreSQL / JPA)]
        G --> G1[HUNTTECH_JOB_CANDIDATE]
        G --> G2[HUNTTECH_CANDIDATE_C_V]
        G --> G3[HUNTTECH_JOB_HISTORY]
        G --> G4[HUNTTECH_CANDIDATE_SKILL]
        G --> G5[HUNTTECH_ITERACTION_LIST]
    end
    
    B5 -->|COMMIT| H[Обновление реестра и подсветка строки в таблице]
```

---

## 3. Сервисный интерфейс (`SmartCvIngestService`)

Фактический интерфейс сервиса (`modules/global/src/com/company/hunttech/service/SmartCvIngestService.java`):

```java
package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.entity.FileDescriptor;
import java.util.UUID;

public interface SmartCvIngestService {
    String NAME = "hunttech_SmartCvIngestService";

    /**
     * Извлечение чистого текста из файла (PDF, DOCX, DOC, RTF, Pages, TXT).
     */
    String extractTextFromFile(FileDescriptor fileDescriptor, byte[] fileBytes);

    /**
     * AI-парсинг текста резюме в структурированный DTO SmartCvParsedData.
     */
    SmartCvParsedData parseCvText(String rawText);

    /**
     * Проверка уникальности кандидата в БД (по телефону, email, telegram, ФИО).
     */
    JobCandidate findDuplicate(SmartCvParsedData data);

    /**
     * Создание новой уникальной карточки кандидата и версии резюме.
     */
    SmartCvIngestResult createNewCandidate(SmartCvParsedData data, FileDescriptor fileDescriptor, 
                                           FileDescriptor faceImage, ExtUser recruiter);

    /**
     * Прикрепление резюме к найденному существующему кандидату (без дублирования).
     */
    SmartCvIngestResult attachCvToExistingCandidate(UUID existingCandidateId, SmartCvParsedData data, 
                                                    FileDescriptor fileDescriptor, FileDescriptor faceImage, ExtUser recruiter);

    /**
     * Применение распознанных данных к существующей карточке резюме.
     */
    SmartCvIngestResult applyParsedDataToCandidateCv(CandidateCV candidateCv, SmartCvParsedData data, ExtUser recruiter);
}
```

---

## 4. DTO-модель распознанных данных (`SmartCvParsedData`)

Объявлена в `com.company.hunttech.service.SmartCvParsedData`:
- `lastName`, `firstName`, `middleName` (разделение ФИО);
- `birthDate` (дата рождения в формате `YYYY-MM-DD`);
- `phone`, `mobilePhone` (нормализованные телефонные номера);
- `email` (нижний регистр);
- `telegram` (очищенный логин без `@` и без URL);
- `skype`, `whatsapp`;
- `position` (желаемая/текущая должность);
- `city` (город проживания с автопоиском в справочнике `City`);
- `currentCompany` (текущая компания с очисткой форм ООО/ЗАО/ПАО через `(?iU)\b`);
- `salary` (зарплатные ожидания);
- `skills` (список атомарных hard skills);
- `experienceYears` (общий стаж в годах);
- `summary` (профессиональное саммари);
- `workExperience` (список `SmartCvWorkExperienceDto`: компания, должность, даты, обязанности, достижения, признак текущего места работы);
- `education` (список `SmartCvEducationDto`: вуз, факультет, специальность, год окончания, академическая степень);
- `missingPositions` (список должностей, отсутствующих в справочнике `Position`, для нотификации рекрутера).

---

## 5. Универсальный UI-мастер (`SmartCvUploadScreen`)

Контроллер экрана `SmartCvUploadScreen` (`com.company.hunttech.web.screens.jobcandidate.SmartCvUploadScreen`, XML: `smart-cv-upload-screen.xml`) открывается в модальном диалоговом режиме (`OpenMode.DIALOG`).

### Поведение при обнаружении дубликата:
1. Если `currentDuplicateCandidate != null`:
   - Отображается блок `duplicateBox` с подробными сведениями о существующем кандидате;
   - Кнопка «Создать карточку кандидата» скрывается;
   - Кнопка **«Не дублировать (привязать резюме к кандидату)»** активируется как основное действие (`primary`);
   - Кнопка **«Дублировать (создать новую запись)»** запрашивает явное подтверждение через `dialogs.createOptionDialog()` с предупреждением о нарушении правил системы.
2. Если кандидат уникален (`currentDuplicateCandidate == null`):
   - Блок `duplicateBox` скрыт;
   - Отображается кнопка **«Создать карточку кандидата»** (`saveNewCandidateBtn`).

### Вызов из реестров и сохранение фокуса таблицы:
```java
// Реестр кандидатов (JobCandidateReestr.java) — BL-2026-024
screen.addAfterCloseListener(closeEvent -> {
    if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
        clearSignIconFilter();
        setCandidateScopeFilter("ALL", "Все кандидаты", "USERS");
        JobCandidate createdCandidate = screen.getCreatedCandidate();
        if (createdCandidate != null) {
            try {
                JobCandidate toSelect = jobCandidatesDc != null
                        ? jobCandidatesDc.getItemOrNull(createdCandidate.getId()) : null;
                if (toSelect == null && jobCandidatesDc != null) {
                    JobCandidate reloadedCandidate = dataManager.load(JobCandidate.class)
                            .id(createdCandidate.getId())
                            .view("jobCandidate-view")
                            .optional()
                            .orElse(null);
                    if (reloadedCandidate != null) {
                        jobCandidatesDc.getMutableItems().add(0, reloadedCandidate);
                        toSelect = reloadedCandidate;
                    }
                }
                if (toSelect == null) {
                    toSelect = createdCandidate;
                }
                candidatesTable.setSelected(toSelect);
                candidatesTable.scrollTo(toSelect);
                candidatesTable.focus();
                populateDetailPane(toSelect);
                updateActionsState(toSelect);
                updateSignIconsState(toSelect);
            } catch (Exception ignored) {
            }
        }
    }
});

// Реестр резюме кандидатов (CandidateCVReestrBrowse.java)
screen.addAfterCloseListener(closeEvent -> {
    if (closeEvent.closedWith(StandardOutcome.COMMIT)) {
        candidateCVsDl.load();
        if (screen.getCreatedCv() != null) {
            try {
                CandidateCV toSelect = candidateCVsDc != null
                        ? candidateCVsDc.getItemOrNull(screen.getCreatedCv().getId()) : null;
                candidateCVsTable.setSelected(toSelect != null ? toSelect : screen.getCreatedCv());
            } catch (Exception ignored) {
            }
        }
    }
});
```

---

## 6. Системный промпт AI-функции `CV_SMART_PARSE_JSON` (Версия v3)

Конфигурация промпта зафиксирована в миграции `modules/core/db/changelog/260913-2-updateSmartCvParsePromptV3.xml` (`CONFIGURATION_VERSION = 3`):
- **Правило NULL**: при отсутствии данных возвращается строго `null` (запрещены слова «Не указано», «Нет», «N/A»);
- **Атомарность навыков**: только технологические hard skills, каждый навык отдельным элементом массива (исключены общие фразы «коммуникабельность»);
- **Нормализация контактов**: приведение телефонов к стандарту `+7...` и Telegram без `@` и без URL;
- **Защита от галлюцинаций**: запрет выдумывания дня и месяца рождения при указании только возраста.

---

## 7. Набор автоматизированных тестов

- `SmartCvIngestComprehensiveTest.java` — 13 комплексных тестов на парсинг PDF, DOCX, RTF, TXT, Apple Pages, устойчивость к битым файлам, парсинг JSON с markdown-обертками, нормализацию кириллических компаний и телефонов;
- `SmartCvIngestServiceContractTest.java` — контрактные тесты DTO, сервисов и регистрации миграций в `db.changelog-master.xml`;
- `JobCandidateReestrSmartUploadContractTest.java` — регрессионный контрактный тест реестра: сброс фильтров, позиционирование, автоматический скролл, фокус и наполнение сайдбара профиля после создания кандидата (BL-2026-024).

---

## 8. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-09-23 | BL-2026-024: В `JobCandidateReestr` после «Умной загрузки» реализован сброс фильтра меток (`clearSignIconFilter()`), сброс области на «Все кандидаты» (`ALL`), догрузка кандидата в `jobCandidatesDc` при выпадении из первых 200 записей выборки, вызовы `setSelected`, `scrollTo`, `focus`, принудительное наполнение сайдбара `populateDetailPane(toSelect)` и обновление кнопок действий. Добавлен тест `JobCandidateReestrSmartUploadContractTest`. |
| 2026-09-13 | Создание модуля «Умная загрузка резюме»: сервис `SmartCvIngestService`, мастер `SmartCvUploadScreen`, системный промпт v3, дедупликация и комплексные автотесты. |
