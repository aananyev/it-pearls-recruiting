# AI-анализ сущностей: архитектура системных промптов

> **Для реализации:** Использовать skill `plan` + делегирование через subagent.

**Цель:** Единая система AI-анализа для любых сущностей HRM HuntTech. Администратор управляет библиотекой промптов. Пользователь нажимает кнопку «AI-анализ» на экране Browse/Edit — система берёт экземпляр сущности, подставляет данные в связанный промпт и отправляет в AI-провайдера. Результат — в UI.

**Архитектура:** Три слоя.
1. **Entity layer** — `AiPromptTemplate` (хранит промпты) + `AiAnalysisLog` (история анализов).
2. **Service layer** — `AiAnalysisService` (оркестрация: извлечение данных → подстановка → API → результат). Использует существующий `HrmAiService` + `AIProviderRegistry`.
3. **UI layer** — CUBA-действие `AiAnalysisAction`, которое можно добавить на любой экран.

---

## Сущности

### 1. `AiPromptTemplate` (global module)

```java
@NamePattern("%s|name")
@Table(name = "HUNTTECH_AI_PROMPT_TEMPLATE")
@Entity(name = "hunttech_AiPromptTemplate")
public class AiPromptTemplate extends StandardEntity {

    @Column(name = "NAME", nullable = false)
    protected String name;           // "Анализ резюме"

    @Column(name = "CODE", nullable = false, unique = true)
    protected String code;           // "RESUME_ANALYSIS"

    @Column(name = "ENTITY_CLASS", nullable = false)
    protected String entityClass;    // "com.company.hunttech.entity.CandidateCV"

    @Lob
    @Column(name = "PROMPT_TEXT", nullable = false)
    protected String promptText;     // Шаблон с {{placeholders}}

    @Column(name = "DESCRIPTION")
    protected String description;    // Описание для админа

    @Column(name = "ACTIVE", nullable = false)
    protected Boolean active = true;

    // {{resumeText}}, {{vacancyDescription}}, {{candidateName}}, {{positionName}} ...
}
```

**Placeholders** — фиксированный набор для каждой сущности. Например, для `CandidateCV`:
- `{{resumeText}}` — текст резюме
- `{{vacancyDescription}}` — описание вакансии (если привязана)
- `{{candidateName}}` — ФИО кандидата
- `{{positionName}}` — название позиции

Для `OpenPosition`:
- `{{vacancyDescription}}` — описание вакансии
- `{{vacancyRequirements}}` — требования
- `{{companyName}}` — компания

### 2. `AiAnalysisLog` (core module, опционально)

Хранит историю запросов: кто, когда, по какой сущности, какой промпт, ответ AI. Нужно для аудита.

---

## Сервис

### `AiAnalysisService` (core module)

```java
public interface AiAnalysisService {
    String NAME = "hunttech_AiAnalysisService";

    /**
     * Выполняет AI-анализ сущности по коду промпта.
     * @param entity анализируемая сущность
     * @param promptCode код промпта из AiPromptTemplate.code
     * @return текст ответа от AI
     */
    String analyze(Entity entity, String promptCode);
}
```

**Реализация** (`AiAnalysisServiceBean`):
1. Загружает `AiPromptTemplate` по `code`
2. Извлекает данные из `entity` через рефлексию + `MetadataTools.getInstanceName()`
3. Подставляет значения в `promptText` (заменяет `{{placeholders}}`)
4. Определяет активного AI-провайдера текущего пользователя через `HrmAiService.getUserConfig()`
5. Вызывает `provider.generateText(filledPrompt, ...)`
6. Возвращает ответ

**Извлечение данных из сущности** — ключевой момент. Вместо хардкода для каждой сущности, используем карту экстракторов:

```java
Map<String, Function<Entity, String>> extractors = Map.of(
    "resumeText",       e -> ((CandidateCV) e).getTextCV(),
    "candidateName",    e -> metadataTools.getInstanceName(((CandidateCV) e).getCandidate()),
    "vacancyDescription", e -> {
        CandidateCV cv = (CandidateCV) e;
        return cv.getToVacancy() != null ? cv.getToVacancy().getDescription() : "";
    },
    // ...
);
```

Экстракторы регистрируются в Spring-контексте и могут быть расширены плагинами.

---

## UI

### CUBA-действие `AiAnalysisAction`

Переиспользуемая кнопка/действие, добавляемая в XML любого экрана:

```xml
<button id="aiAnalysisBtn"
        caption="AI-анализ"
        icon="BRAIN"
        invoke="onAiAnalysisClick"/>
```

В контроллере — один метод:

```java
public void onAiAnalysisClick() {
    Entity entity = getEditedEntity(); // или getSelected()
    AiAnalysisService service = AppBeans.get(AiAnalysisService.class);
    String promptCode = resolvePromptCode(entity.getClass());
    String result = service.analyze(entity, promptCode);
    showResultDialog(result);
}
```

`resolvePromptCode()` — ищет активный промпт для класса сущности.

### Admin console

CRUD-экран `AiPromptTemplateBrowse` / `AiPromptTemplateEdit`:
- Доступен только роли `ADMINISTRATOR`
- Позволяет создавать/редактировать/удалять промпты
- Поле `promptText` — большой текстовый редактор
- Подсказка с доступными placeholders для выбранной сущности

---

## План реализации

### Этап 1: Entity + DB (1-2 часа)
1. Создать `AiPromptTemplate` entity в глобальном модуле
2. Добавить в `views.xml` browse/edit views
3. Создать Liquibase-миграцию для таблицы `HUNTTECH_AI_PROMPT_TEMPLATE`
4. Обновить `persistence.xml`

### Этап 2: Сервис (1-2 часа)
1. Создать `AiAnalysisService` интерфейс (global) и бин (core)
2. Реализовать `analyze()` — загрузка промпта, извлечение данных, подстановка, вызов AI
3. Реализовать реестр экстракторов данных для базовых сущностей (CandidateCV, OpenPosition, JobCandidate)
4. Зарегистрировать в `web-spring.xml` (WebRemoteProxyBeanCreator)

### Этап 3: Admin UI (1 час)
1. Создать `AiPromptTemplateBrowse` / `AiPromptTemplateEdit`
2. Ограничить доступ ролью `ADMINISTRATOR` через `web-permissions.xml`
3. Добавить пункт меню «Системные промпты AI»

### Этап 4: Интеграция в экраны (2 часа)
1. Создать `AiAnalysisAction` — переиспользуемый компонент
2. Добавить кнопку «AI-анализ» на `CandidateCVEdit` (анализ резюме)
3. Добавить кнопку на `OpenPositionEdit` (анализ вакансии)
4. Добавить кнопку на `JobCandidateEdit` (анализ кандидата)

### Этап 5: Тесты + документация (1 час)
1. `AiAnalysisServiceTest` — автотест подстановки prompt и вызова AI
2. `AiPromptTemplateTest` — CRUD тест
3. Документация в `docs/ai/AI_ANALYSIS.md`

---

## Механика вызова (пример)

**Сценарий: анализ резюме**

1. Пользователь открывает `CandidateCVEdit`
2. Нажимает «AI-анализ»
3. Система ищет активный `AiPromptTemplate` с `entityClass = "CandidateCV"` и `code = "RESUME_ANALYSIS"`
4. Извлекает данные:
   - `{{resumeText}}` → `candidateCV.getTextCV()`
   - `{{candidateName}}` → `metadataTools.getInstanceName(candidateCV.getCandidate())`
   - `{{vacancyDescription}}` → `candidateCV.getToVacancy()?.getDescription()`
5. Заполняет шаблон и отправляет в AI
6. Ответ AI показывается в модальном окне

**Сценарий: анализ вакансии**

1. Пользователь в `OpenPositionEdit` нажимает «AI-анализ»
2. Промпт `code = "VACANCY_ANALYSIS"`
3. Извлекает `{{vacancyDescription}}`, `{{vacancyRequirements}}`, `{{companyName}}`
4. AI выдаёт «расшифровку вакансии человеческим языком»

---

## Преимущества архитектуры

- **Расширяемость:** Новый промпт = новая запись в БД, без кода
- **Переиспользование:** Одно действие на всех экранах
- **Безопасность:** Промпты только для админа, API-ключи — у каждого пользователя свои
- **Простота:** Минимум нового кода, максимум конфигурации
