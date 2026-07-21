# План реализации: AI-анализ сущностей

> 2026-07-21 · `agent/job-candidate-edit-layout-fix`

## Этап 1: Сущность AiPromptTemplate (1 час)

### 1.1 Entity
- `modules/global/src/com/company/hunttech/entity/AiPromptTemplate.java`
  - `name` String, `code` String unique, `entityClass` String, `promptText` LOB, `description` String, `active` Boolean
  - `@NamePattern("%s|name")`, `@Table("HUNTTECH_AI_PROMPT_TEMPLATE")`

### 1.2 Миграция
- `modules/core/db/update/h2/99/990101-001-createAiPromptTemplate.sql`
- `modules/core/db/update/postgres/99/990101-001-createAiPromptTemplate.sql`

### 1.3 Views
- `modules/global/src/com/company/hunttech/views.xml` — добавить `aiPromptTemplate-browse-view`, `aiPromptTemplate-edit-view`

## Этап 2: Сервис AiAnalysisService (1 час)

### 2.1 Интерфейс
- `modules/global/src/com/company/hunttech/service/AiAnalysisService.java`
  ```java
  String NAME = "hunttech_AiAnalysisService";
  String analyze(Entity entity, String promptCode);
  ```

### 2.2 Экстракторы
- `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractor.java` — интерфейс
- `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractors.java` — реестр (Map<String, Map<String, Function<Entity, String>>>)
- На старте регистрируем базовые:
  - `CandidateCV` → `resumeText`, `candidateName`, `vacancyDescription`, `positionName`
  - `OpenPosition` → `vacancyDescription`, `vacancyRequirements`, `companyName`, `projectName`
  - `IteractionList` → `interactionType`, `comment`, `dateIteraction`, `candidateName`, `candidateHistory`
  - `JobCandidate` → `fullName`, `email`, `phone`, `positionList`

### 2.3 Бин
- `modules/core/src/com/company/hunttech/core/ai/AiAnalysisServiceBean.java`
  ```
  @Service(AiAnalysisService.NAME)
  analyze(entity, promptCode):
    1. dataManager.load(AiPromptTemplate.class).query("code = :code").one()
    2. filled = template.getPromptText()
    3. for each {{placeholder}}: filled.replace("{{" + k + "}}", extractors.get(entity.class, k))
    4. config = hrmAiService.getUserConfig(providerCode)
    5. return provider.generateText(filled, "AI-анализ HRM HuntTech", config.getApiKey(), config.getDefaultModelName(), {})
  ```

### 2.4 Регистрация в web
- `modules/web/src/com/company/hunttech/web-spring.xml`
  ```xml
  <entry key="hunttech_AiAnalysisService" value="com.company.hunttech.service.AiAnalysisService"/>
  ```

## Этап 3: CUBA Action (30 мин)

### 3.1 Класс
- `modules/web/src/com/company/hunttech/web/ai/AiAnalysisAction.java`
  ```java
  public class AiAnalysisAction extends BaseAction {
      // constructor(screen, entitySupplier, promptCode)
      // actionPerform: reload entity with View.LOCAL → service.analyze() → dialog
  }
  ```

### 3.2 Диалог результата
- Внутри `AiAnalysisAction.actionPerform()`:
  ```java
  screen.showMessageDialog("AI-анализ", result, MessageType.CONFIRMATION_HTML);
  ```

## Этап 4: Admin UI (1 час)

### 4.1 Экран browse
- `modules/web/src/.../aiprompttemplate/AiPromptTemplateBrowse.java`
- `modules/web/src/.../aiprompttemplate/ai-prompt-template-browse.xml`
  - `StandardLookup<AiPromptTemplate>`, таблица с create/edit/remove

### 4.2 Экран edit
- `modules/web/src/.../aiprompttemplate/AiPromptTemplateEdit.java`
- `modules/web/src/.../aiprompttemplate/ai-prompt-template-edit.xml`
  - Поля: name, code, entityClass (lookup), promptText (textArea), active (checkBox)
  - Подсказка: список доступных {{placeholders}} для entityClass

### 4.3 Меню
- `modules/web/src/com/company/hunttech/web-menu.xml`
  ```xml
  <item screen="hunttech_AiPromptTemplate.browse" 
        caption="Системные промпты AI" 
        roles="systemAdministrator"/>
  ```

### 4.4 Messages
- `modules/web/src/.../aiprompttemplate/messages_ru.properties`
- `modules/web/src/.../aiprompttemplate/messages.properties`

## Этап 5: Интеграция в сущности (1 час)

### 5.1 Добавить кнопку в каждую форму — 3 строки:

**CandidateCVEdit:**
- XML: `<button id="aiBtn" caption="AI-анализ резюме" action="aiAnalysis"/>`
- Java: `addAction(new AiAnalysisAction(this, this::getEditedEntity, "RESUME_ANALYSIS"));`

**OpenPositionEdit:**
- XML + Java: аналогично, promptCode = "VACANCY_ANALYSIS"

**IteractionListEdit:**
- XML + Java: promptCode = "INTERACTION_ANALYSIS"

**JobCandidateEdit:**
- XML + Java: promptCode = "CANDIDATE_ANALYSIS"

## Этап 6: Базовые промпты (15 мин)

SQL-скрипт для первоначального наполнения:
```sql
INSERT INTO HUNTTECH_AI_PROMPT_TEMPLATE (ID, NAME, CODE, ENTITY_CLASS, PROMPT_TEXT, ACTIVE)
VALUES
(uuid(), 'Анализ резюме', 'RESUME_ANALYSIS',
 'com.company.hunttech.entity.CandidateCV',
 'Проанализируй резюме кандидата {{candidateName}}. Текст: {{resumeText}}. Вакансия: {{vacancyDescription}}. Оцени соответствие.', true),
(uuid(), 'Расшифровка вакансии', 'VACANCY_ANALYSIS',
 'com.company.hunttech.entity.OpenPosition',
 'Объясни вакансию простым языком. Название: {{vacancyDescription}}. Требования: {{vacancyRequirements}}. Компания: {{companyName}}.', true),
(uuid(), 'Анализ взаимодействий', 'INTERACTION_ANALYSIS',
 'com.company.hunttech.entity.IteractionList',
 'Проанализируй воронку кандидата. Текущее: {{comment}}. История: {{candidateHistory}}. Вакансия: {{vacancyFullContext}}. Дай прогноз.', true);
```

## Этап 7: Тесты + автотест регистрации (30 мин)

### 7.1 HrmAiServiceTest (дополнить)
- Проверить, что `AiAnalysisService` доступен через `AppBeans`
- Проверить, что `AiPromptTemplate` создаётся через `DataManager`

### 7.2 AiAnalysisWebRegistrationTest
- Проверить наличие `<entry key="hunttech_AiAnalysisService">` в `web-spring.xml`

## Порядок сборки

```bash
./gradlew createDb updateDb      # миграции
./gradlew compileJava deploy -x test
# перезапуск Tomcat
```

## Финальное состояние

| Что | Где | Роль |
|-----|-----|------|
| Управление промптами | Администрирование → Системные промпты AI | Админ |
| Кнопка «AI-анализ» | Любая Edit-форма (3 строки кода) | Все |
| Промпты в БД | 3 базовых, можно добавлять через UI | Админ |
| API-ключи | Мониторинг ключей пользователя | Каждый сам |
