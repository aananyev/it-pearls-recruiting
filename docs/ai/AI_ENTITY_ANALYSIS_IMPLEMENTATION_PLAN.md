# План реализации: AI-анализ сущностей (v3 — 10 рисков устранено)

> **Ветка:** `feat/ai-entity-analysis`  
> **Дата:** 2026-07-21  
> **Критерии:** лёгкость реализации · лёгкость использования · не трогать бизнес-логику

---

## Таблица устранённых рисков

| # | Риск | Решение |
|---|------|---------|
| 1 | `HrmAiService` не принимает произвольные промпты | `sendPrompt()` |
| 2 | `View.LOCAL` тащит LOB | `_local` view |
| 3 | `@PostConstruct` раньше `@Inject` | `@Autowired` |
| 4 | Неверный `Dialogs` API | `createOptionDialog` + `withHtml(true)` |
| 5 | Неверный `BaseAction` | Отказ от `BaseAction` → `invoke` + helper |
| 6 | `getSimpleName` с Hibernate-прокси | `replaceAll` |
| 7 | Нет валидации ввода в `sendPrompt` | Проверки + `getUserConfig` |
| 8 | `Dialogs.createMessageDialog` не существует | `createOptionDialog(CONFIRMATION)` |
| 9 | `addAction()` на Screen не работает | Отказ от действий → `invoke` в XML |
| 10 | `getUserConfig` падает без конфигурации у юзера | Fallback: системный админ или ошибка с инструкцией |
| 11 | Placeholder-ы непонятны админу | Поле `availablePlaceholders` + подсказка в edit-форме |
| 12 | Новый placeholder = правка Java | Гибрид: базовые в коде, расширенные через `promptText` самого шаблона |
| 13 | Длинный ответ AI не влезает в dialog | `withWidth("700px")` + `withHeight("500px")` |

---

## Упрощённая архитектура (v3)

```
┌──────────────────────────────────────────────────────┐
│ ФОРМА (3 строки)                                     │
│ XML:  <button invoke="onAiAnalysisClick"/>            │
│ Java: AiAnalysisHelper.analyze(this, entity, "CODE") │
├──────────────────────────────────────────────────────┤
│ AiAnalysisHelper (статический, 40 строк)              │
│ → reload entity → AiAnalysisService.analyze()         │
│ → dialogs.createOptionDialog(результат)               │
├──────────────────────────────────────────────────────┤
│ AiAnalysisServiceBean (core)                          │
│ → загрузить AiPromptTemplate по коду                  │
│ → заполнить {{placeholders}} через extractors        │
│ → HrmAiService.sendPrompt()                          │
├──────────────────────────────────────────────────────┤
│ EntityDataExtractors (базовые: 4 сущности)            │
│ + поле AiPromptTemplate.availablePlaceholders         │
│   (JSON-схема, видимая админу в UI)                   │
└──────────────────────────────────────────────────────┘
```

**Ключевое упрощение:** `BaseAction` заменён на `invoke` + статический helper. Это на 100 строк меньше кода и полностью идиоматично для CUBA 7.3.

---

## Этап 1: Entity AiPromptTemplate

### 1.1 Класс сущности

**Файл (создать):** `modules/global/src/com/company/hunttech/entity/AiPromptTemplate.java`

```java
package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import javax.persistence.*;

/**
 * Шаблон системного промпта AI.
 * {{placeholders}} заполняются данными сущности при анализе.
 */
@NamePattern("%s|name")
@Table(name = "HUNTTECH_AI_PROMPT_TEMPLATE")
@Entity(name = "hunttech_AiPromptTemplate")
public class AiPromptTemplate extends StandardEntity {

    @Column(name = "NAME", nullable = false)
    protected String name;

    @Column(name = "CODE", nullable = false, unique = true)
    protected String code;

    @Column(name = "ENTITY_CLASS", nullable = false)
    protected String entityClass;

    @Lob
    @Column(name = "PROMPT_TEXT", nullable = false)
    protected String promptText;

    @Lob
    @Column(name = "AVAILABLE_PLACEHOLDERS")
    protected String availablePlaceholders;  // JSON: {"placeholder":"описание"}

    @Column(name = "DESCRIPTION")
    protected String description;

    @Column(name = "ACTIVE", nullable = false)
    protected Boolean active = true;

    // геттеры/сеттеры
}
```

### 1.2–1.5 — без изменений относительно v2 плана.

---

## Этап 2: HrmAiService.sendPrompt()

### 2.1 Интерфейс

**Файл (изменить):** `modules/global/src/com/company/hunttech/service/HrmAiService.java`

```java
/**
 * Отправляет произвольный промпт AI-провайдеру.
 * Используется AiAnalysisService для анализа сущностей.
 *
 * @param userPrompt   заполненный текст промпта
 * @param providerCode код провайдера
 * @return ответ AI
 */
String sendPrompt(String userPrompt, String providerCode);
```

### 2.2 Реализация

**Файл (изменить):** `modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java`

```java
@Override
public String sendPrompt(String userPrompt, String providerCode) {
    if (!isConfigured(userPrompt)) {
        throw new DevelopmentException("Промпт не может быть пустым.");
    }

    UserAiConfiguration config = getUserConfig(providerCode);
    if (config == null) {
        throw new DevelopmentException(
            "Нет активной AI-конфигурации для провайдера «" + providerCode + "».\n" +
            "Добавьте ключ в окне «Мониторинг ключей пользователя».");
    }

    AIProvider provider = aiProviderRegistry.getProvider(providerCode);
    return provider.generateText(
        userPrompt,
        "Ты — AI-ассистент рекрутинговой системы HRM HuntTech. Отвечай на русском языке.",
        config.getApiKey(),
        config.getDefaultModelName(),
        Map.of("temperature", 0.3));
}
```

---

## Этап 3: AiAnalysisService + EntityDataExtractors (упрощённые)

### 3.1 EntityDataExtractor (без изменений)

### 3.2 EntityDataExtractors (4 сущности, минимальный набор)

**Файл (создать):** `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractors.java`

Базовый набор: `CandidateCV` (4 поля), `OpenPosition` (4 поля), `IteractionList` (6 полей), `JobCandidate` (3 поля). Общий объём — ~120 строк (вместо 200+ в v2).

### 3.3 AiAnalysisServiceBean

**Файл (создать):** `modules/core/src/com/company/hunttech/core/ai/AiAnalysisServiceBean.java`

```java
@Service(AiAnalysisService.NAME)
public class AiAnalysisServiceBean implements AiAnalysisService {

    @Inject private DataManager dataManager;
    @Inject private HrmAiService hrmAiService;
    @Inject private EntityDataExtractors extractors;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public String analyze(Entity entity, String promptCode) {
        AiPromptTemplate template = dataManager.load(AiPromptTemplate.class)
            .query("select e from hunttech_AiPromptTemplate e " +
                   "where e.code = :code and e.active = true")
            .parameter("code", promptCode)
            .view("_local")
            .optional()
            .orElseThrow(() -> new DevelopmentException(
                "Промпт «" + promptCode + "» не найден или неактивен."));

        String filledPrompt = fillPlaceholders(template.getPromptText(), entity);
        return hrmAiService.sendPrompt(filledPrompt, "openai");  // default provider
    }

    private String fillPlaceholders(String template, Entity entity) {
        String result = template;
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            String placeholder = m.group(1);
            String value = extractors.extract(entity, placeholder);
            result = result.replace("{{" + placeholder + "}}", value);
        }
        return result;
    }
}
```

### 3.4 web-spring.xml — без изменений

---

## Этап 4: AiAnalysisHelper (ВМЕСТО AiAnalysisAction)

**Файл (создать):** `modules/web/src/com/company/hunttech/web/ai/AiAnalysisHelper.java`

```java
package com.company.hunttech.web.ai;

import com.company.hunttech.service.AiAnalysisService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.screen.Screen;

/**
 * Статический helper для вызова AI-анализа из любой формы.
 *
 * Использование (3 строки в форме):
 *   XML:  <button id="aiBtn" caption="AI-анализ" invoke="onAiAnalysisClick"/>
 *   Java: public void onAiAnalysisClick() {
 *             AiAnalysisHelper.analyze(this, getEditedEntity(), "RESUME_ANALYSIS");
 *         }
 */
public final class AiAnalysisHelper {

    private AiAnalysisHelper() {}

    /**
     * @param screen     текущий экран (для показа диалогов)
     * @param entity     сущность для анализа (может быть null — тогда нотификация)
     * @param promptCode код промпта из AiPromptTemplate
     */
    public static void analyze(Screen screen, Entity entity, String promptCode) {
        if (entity == null) {
            AppBeans.get(Notifications.class)
                .create(Notifications.NotificationType.WARNING)
                .withCaption("Нет данных для анализа")
                .show();
            return;
        }

        // Перезагружаем с _local view, чтобы все поля были доступны
        DataManager dm = AppBeans.get(DataManager.class);
        Entity full = dm.load(LoadContext.create(entity.getClass())
            .setId(entity.getId())
            .setView(View.LOCAL));

        AiAnalysisService svc = (AiAnalysisService) AppBeans.get("hunttech_AiAnalysisService");

        try {
            String result = svc.analyze(full, promptCode);

            Dialogs dialogs = AppBeans.get(Dialogs.class);
            dialogs.createOptionDialog(Dialogs.MessageType.CONFIRMATION)
                .withCaption("AI-анализ")
                .withMessage(result)
                .withWidth("700px")
                .withHeight("500px")
                .withHtml(true)
                .show();
        } catch (Exception e) {
            AppBeans.get(Notifications.class)
                .create(Notifications.NotificationType.ERROR)
                .withCaption("Ошибка AI-анализа")
                .withDescription(e.getMessage())
                .show();
        }
    }
}
```

---

## Этап 5: Администрирование промптов

### 5.1 AiPromptTemplateBrowse — без изменений

### 5.2 AiPromptTemplateEdit

**Файл (создать):** `modules/web/src/.../aiprompttemplate/ai-prompt-template-edit.xml`

В форму добавить информационную панель с подсказкой:
```xml
<label id="placeholdersHelp"
       value="Доступные placeholders загружаются из поля availablePlaceholders"
       stylename="small"
       htmlEnabled="true"/>
```

### 5.3 Меню — без изменений

---

## Этап 6: Интеграция в формы

**Правило:** только в формы, где кнопка реально нужна и НЕ ломает бизнес-логику.

### 6.1 CandidateCVEdit

**XML:** `<button id="aiBtn" caption="AI-анализ резюме" invoke="onAiAnalysisClick"/>`

**Java:**
```java
public void onAiAnalysisClick() {
    AiAnalysisHelper.analyze(this, getEditedEntity(), "RESUME_ANALYSIS");
}
```

### 6.2 OpenPositionEdit

```java
public void onAiAnalysisClick() {
    AiAnalysisHelper.analyze(this, getEditedEntity(), "VACANCY_ANALYSIS");
}
```

### 6.3 IteractionListEdit

```java
public void onAiAnalysisClick() {
    AiAnalysisHelper.analyze(this, getEditedEntity(), "INTERACTION_ANALYSIS");
}
```

### 6.4 ⚠️ JobCandidateEdit — ОТЛОЖИТЬ

Не добавлять в JobCandidateEdit сразу: форма сложная, 4000+ строк, lazy-табы. Высокий риск конфликта. Добавить позже, отдельной задачей.

---

## Этап 7: Базовые промпты (SQL)

Без изменений. Три промпта: RESUME_ANALYSIS, VACANCY_ANALYSIS, INTERACTION_ANALYSIS.

В поле `availablePlaceholders` — JSON с описанием:
```json
{"resumeText":"текст резюме","candidateName":"ФИО кандидата","vacancyDescription":"описание вакансии","positionName":"название позиции"}
```

---

## Этап 8: Тесты

Дополнить `HrmAiServiceTest`:

```java
@Test
public void sendPromptRejectsEmptyPrompt() {
    try {
        aiService.sendPrompt(null, "openai");
        fail("missing exception");
    } catch (DevelopmentException e) { /* ok */ }
}
```

---

## Итоговая таблица: что изменилось vs v2

| Компонент | v2 (старый) | v3 (исправленный) |
|-----------|-------------|-------------------|
| UI-интеграция | `AiAnalysisAction extends BaseAction` + `addAction()` | `AiAnalysisHelper.analyze()` + `invoke` |
| Строк в форме | 2 (XML + Java addAction) | 2 (XML invoke + Java helper) |
| Диалог результата | `createMessageDialog` (не существует) | `createOptionDialog` + `withHtml(true)` + `700×500` |
| Экстракторы | 200+ строк, 25+ полей | 120 строк, 17 полей (4 сущности) |
| Placeholder-ы для админа | Нет подсказок | Поле `availablePlaceholders` (JSON) |
| JobCandidateEdit | Кнопка сразу | Отложено (высокий риск) |
| Fallback для sendPrompt | Нет | `getUserConfig` с понятной ошибкой |

---

## Финальный чеклист

- [ ] Этап 1: Entity + миграции + views + persistence.xml → `./gradlew :app-global:compileJava`
- [ ] Этап 2: `sendPrompt` в HrmAiService → `./gradlew :app-core:compileJava`
- [ ] Этап 3: AiAnalysisService + экстракторы + web-spring → `./gradlew :app-core:compileJava :app-web:compileJava`
- [ ] Этап 4: AiAnalysisHelper → `./gradlew :app-web:compileJava`
- [ ] Этап 5: Admin UI (browse/edit/меню) → `./gradlew :app-web:compileJava`
- [ ] Этап 6: Кнопки в 3 формах (CV, OpenPosition, IteractionList) → `./gradlew :app-web:compileJava`
- [ ] Этап 7: SQL-сиды промптов
- [ ] Этап 8: Автотесты зелёные
- [ ] Финальная сборка: `./gradlew deploy -x test` → HTTP 200
- [ ] Ручная проверка: открыть CV → кнопка → диалог с результатом
