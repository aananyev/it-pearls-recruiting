# Архитектура единого модуля «Умная загрузка резюме» (`SmartCvIngestService` & `SmartCvUploadScreen`)

## 1. Назначение и бизнес-требования

Модуль **«Умная загрузка резюме»** в HRM HuntTech обеспечивает сквозной автоматизированный процесс приёма, извлечения текста, AI-парсинга, проверки дубликатов и сохранения кандидатов и их резюме в систему.

### Ключевые требования:
1. **Поддержка источников резюме:**
   * **Файлы:** форматы `PDF`, `DOC`, `DOCX`, `RTF`, `PAGES`, `TXT` (до 20 МБ);
   * **Текст / RichText:** прямая вставка неструктурированного текста или форматированного резюме;
   * **Интернет / URL:** импорт и парсинг резюме по прямой web-ссылке (HeadHunter, Habr Career, LinkedIn и др.).
2. **Единая кодовая база (Single Source of Truth):**
   * Вся логика извлечения текста, AI-структурирования, поиска дубликатов и создания сущностей (`JobCandidate`, `CandidateCV`, `PersonContact`, `CandidateSkill`) вынесена в единый сервисный слой `SmartCvIngestService` (core) и универсальный мастер `SmartCvUploadScreen` (web).
   * Исключено дублирование логики между экранами: форма «Реестр кандидатов» (`JobCandidateReestr`) и «Реестр резюме» (`CandidateCVReestrBrowse`) вызывают один и тот же переиспользуемый компонент.
3. **Безопасность и целостность данных:**
   * Недеструктивная обработка текста;
   * Поиск дубликатов по Email, Телеграм, телефону и ФИО с выбором стратегии (создать нового кандидата или прикрепить резюме к существующему);
   * Защита от N+1 и строгий контроль транзакций базы данных.

---

## 2. Архитектурная схема

```mermaid
graph TD
    A[Пользователь / Рекрутер] -->|Клик 'Умная загрузка'| B[SmartCvUploadScreen]
    
    subgraph UI Layer (modules/web)
        B --> B1[Вкладка 1: Загрузка файлов\nPDF, DOCX, DOC, RTF, PAGES]
        B --> B2[Вкладка 2: Вставка текста\nRichTextArea / PlainText]
        B --> B3[Вкладка 3: Ссылка из интернета\nHTTP / Web Ingest]
        B --> B4[Превью, проверка дубликатов & форма валидации]
    end
    
    subgraph Core Layer (modules/core)
        B1 & B2 & B3 -->|Вызов| C[SmartCvIngestService]
        C --> D[TextProcessingService / Apache Tika\nИзвлечение чистого текста]
        C --> E[AI Function / LLM Parser\nСтруктурирование в JSON]
        C --> F[Duplicate Detection Engine\nEmail, Telegram, Phone, FIO]
        C --> G[Transactional Data Commit\nJobCandidate, CandidateCV]
    end
    
    subgraph Database Layer
        G --> H[(PostgreSQL)]
    end
    
    B4 -->|Фокус и открытие| I[CandidateCVEdit / JobCandidateEdit]
```

---

## 3. Сервисный интерфейс (`SmartCvIngestService`)

Интерфейс сервиса объявлен в глобальном модуле:

```java
package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.core.entity.FileDescriptor;
import java.util.List;

public interface SmartCvIngestService {
    String NAME = "hunttech_SmartCvIngestService";

    /**
     * Извлечение чистого текста из файла любого поддерживаемого формата.
     */
    String extractTextFromFile(FileDescriptor fileDescriptor);

    /**
     * AI-парсинг текста резюме в структурированную DTO-модель SmartCvParsedData.
     */
    SmartCvParsedData parseCvText(String rawText);

    /**
     * Проверка на дубликаты кандидатов в БД по контактам и ФИО.
     */
    List<JobCandidate> findDuplicates(SmartCvParsedData parsedData);

    /**
     * Сохранение кандидата и резюме (создание нового или прикрепление к существующему).
     */
    CandidateCV saveCandidateAndCv(SmartCvParsedData data, FileDescriptor rawFile, JobCandidate existingCandidate);
}
```

---

## 4. DTO-модель распознанных данных (`SmartCvParsedData`)

```java
public class SmartCvParsedData implements Serializable {
    private String fullName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String targetPosition;
    private String email;
    private String phone;
    private String telegram;
    private String city;
    private BigDecimal salaryExpected;
    private String currency;
    private String skillsSummary;
    private List<String> extractedSkills;
    private String formattedCvHtml;
    private String rawCvText;
    
    // Геттеры, сеттеры и вспомогательные методы
}
```

---

## 5. Универсальный UI-мастер (`SmartCvUploadScreen`)

Контроллер экрана `SmartCvUploadScreen` (`com.company.hunttech.web.screens.jobcandidate.SmartCvUploadScreen`) зарегистрирован с id `hunttech_SmartCvUploadScreen` и открывается в модальном диалоговом режиме (`OpenMode.DIALOG`).

### Экранные события и возврат созданного резюме:
1. Контроллер сохраняет ссылку на созданное резюме в поле `createdCv` и предоставляет геттер `getCreatedCv()`.
2. После успешного сохранения и закрытия с `WINDOW_COMMIT_AND_CLOSE_ACTION`:
   * Вызывающий экран (например, `CandidateCVReestrBrowse` или `JobCandidateReestr`) перезагружает свой data loader.
   * Выполняется автоматическая фокусировка и выбор созданной строки в таблице.

### Использование из экранов реестров:
```java
@Subscribe("smartUploadBtn")
public void onSmartUploadBtnClick(Button.ClickEvent event) {
    SmartCvUploadScreen screen = screenBuilders.screen(this)
            .withScreenClass(SmartCvUploadScreen.class)
            .withOpenMode(OpenMode.DIALOG)
            .build();
    screen.addAfterCloseListener(afterCloseEvent -> {
        if (afterCloseEvent.closedWith(StandardOutcome.COMMIT)) {
            candidateCvsDl.load();
            CandidateCV created = screen.getCreatedCv();
            if (created != null) {
                candidateCvsTable.setSelected(created);
            }
        }
    });
    screen.show();
}
```

---

## 6. Визуальные стандарты и адаптация под темы

1. **Таблицы реестров (`.candidate-browse-grid`):**
   * Увеличение высоты строк на 20% (`min-height: 38px`, `padding: 6px 8px`).
   * Полная поддержка многострочного переноса текста по словам (`white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;`) в колонках ФИО, должности, рекрутера, вакансии и компании во всех 7 SCSS-темах (`halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-dark`, `hunttech-modern-light`).
2. **Кнопка «Умная загрузка» (`#smartUploadBtn`):**
   * Иконка `font-icon:MAGIC`, основной визуальный акцент (`stylename="primary candidate-btn candidate-smartload-btn"`).
   * Располагается первой в командном тулбаре рядом с кнопкой создания.

---

## 7. План тестирования и контроля целостности

1. **Контрактные тесты тем и верстки:**
   * `JobCandidateEditLayoutContractTest` — проверка 100% идентичности SCSS во всех 7 темах;
   * `GeolocationEditFormsContractTest` & `ProjectEditLayoutContractTest` — валидация общих правил `edit-screen-shared-styles.scss`.
2. **Бизнес-тесты сервисов:**
   * `TextProcessingServiceBeanTest` — корректность извлечения и HTML/Plain форматирования;
   * `SmartCvIngestServiceTest` — тестирование парсинга, дедупликации и сохранения.
