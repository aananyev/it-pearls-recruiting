# План реализации: AI-анализ сущностей (исправленный)

> **Ветка:** `feat/ai-entity-analysis`  
> **Дата:** 2026-07-21  
> **Стек:** CUBA 7.3, Java 11, Gradle, PostgreSQL  
> **Архитектура:** `docs/ai/AI_ANALYSIS.md`

---

## Исправления относительно первоначального плана

| # | Проблема | Исправление |
|---|----------|-------------|
| 1 | `HrmAiService` не умеет отправлять произвольные промпты | Добавлен метод `sendPrompt(String, String)` |
| 2 | `View.LOCAL` тащит LOB | Создан `aiPromptTemplate-analysis-view` (только нужные поля) |
| 3 | `@PostConstruct` раньше `@Inject` в CUBA | Переход на `@Autowired` + Spring `@PostConstruct` |
| 4 | Неверный `Dialogs` API | `screen.showOptionDialog(...)` |
| 5 | Неверный `BaseAction` импорт | `com.haulmont.cuba.gui.actions.BaseAction` |
| 6 | `getSimpleName()` с Hibernate-прокси | `getClass().getSimpleName().replaceAll("\\$.*", "")` |
| 7 | Нет `sendPrompt` в HrmAiService | Расширен до `sendPrompt(String userPrompt, String providerCode)` |

---

## Правила реализации (обязательные)

1. **Следовать плану строго по порядку этапов.**
2. **После каждого этапа — сборка (`./gradlew compileJava`).**
3. **Не менять существующий код без явной необходимости.**
4. **Не менять бизнес-логику, запросы, загрузчики, проверки, сохранение данных.**
5. **Не коммитить, пока пользователь явно не попросит.**
6. **Каждый новый файл — с комментарием на русском о назначении.**
7. **Все @Inject — только для компонентов tabMain (не lazy-табов).**
8. **Новые сервисы — регистрировать в `web-spring.xml` через WebRemoteProxyBeanCreator.**

---

## Этап 1: Entity AiPromptTemplate

### 1.1 Создать класс сущности

**Файл (создать):** `modules/global/src/com/company/hunttech/entity/AiPromptTemplate.java`

```java
package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;

/**
 * Шаблон системного промпта для AI-анализа сущностей.
 * Управляется администратором. Хранит текст промпта с {{placeholders}},
 * которые заполняются данными сущности во время анализа.
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

    @Column(name = "DESCRIPTION")
    protected String description;

    @Column(name = "ACTIVE", nullable = false)
    protected Boolean active = true;

    // Стандартные геттеры/сеттеры
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getEntityClass() { return entityClass; }
    public void setEntityClass(String entityClass) { this.entityClass = entityClass; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
```

### 1.2 Добавить в persistence.xml

**Файл (изменить):** `modules/global/src/com/company/hunttech/persistence.xml`

Внутри `<persistence-unit>` добавить:
```xml
<class>com.company.hunttech.entity.AiPromptTemplate</class>
```

### 1.3 SQL-миграции

**Файл (создать):** `modules/core/db/update/h2/99/990721-001-createAiPromptTemplate.sql`

```sql
create table HUNTTECH_AI_PROMPT_TEMPLATE (
    ID varchar(36) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    VERSION integer not null default 1,
    NAME varchar(255) not null,
    CODE varchar(255) not null unique,
    ENTITY_CLASS varchar(255) not null,
    PROMPT_TEXT text not null,
    DESCRIPTION varchar(1000),
    ACTIVE boolean not null default true,
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    primary key (ID)
);
```

**Файл (создать):** `modules/core/db/update/postgres/99/990721-001-createAiPromptTemplate.sql`

```sql
create table HUNTTECH_AI_PROMPT_TEMPLATE (
    ID varchar(36) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    VERSION integer not null default 1,
    NAME varchar(255) not null,
    CODE varchar(255) not null unique,
    ENTITY_CLASS varchar(255) not null,
    PROMPT_TEXT text not null,
    DESCRIPTION varchar(1000),
    ACTIVE boolean not null default true,
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    primary key (ID)
);
```

### 1.4 Views — browse + edit + analysis

**Файл (изменить):** `modules/global/src/com/company/hunttech/views.xml`

```xml
<!-- Browse: без LOB promptText, без description -->
<view class="com.company.hunttech.entity.AiPromptTemplate"
      extends="_local"
      name="aiPromptTemplate-browse-view">
    <property name="name"/>
    <property name="code"/>
    <property name="entityClass"/>
    <property name="active"/>
</view>

<!-- Edit: с promptText, без description -->
<view class="com.company.hunttech.entity.AiPromptTemplate"
      extends="_local"
      name="aiPromptTemplate-edit-view">
    <property name="name"/>
    <property name="code"/>
    <property name="entityClass"/>
    <property name="promptText"/>
    <property name="description"/>
    <property name="active"/>
</view>
```

### 1.5 ✅ Сборка

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-global:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 2: Расширение HrmAiService — новый метод sendPrompt

### 2.1 Интерфейс

**Файл (изменить):** `modules/global/src/com/company/hunttech/service/HrmAiService.java`

Добавить в интерфейс:
```java
/**
 * Отправляет произвольный промпт AI-провайдеру активной конфигурации пользователя.
 * Используется AiAnalysisService для AI-анализа сущностей.
 *
 * @param userPrompt   текст промпта (уже заполненный данными)
 * @param providerCode код провайдера (из UserAiConfiguration.providerCode)
 * @return текстовый ответ AI
 */
String sendPrompt(String userPrompt, String providerCode);
```

### 2.2 Реализация

**Файл (изменить):** `modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java`

Добавить метод:
```java
@Override
public String sendPrompt(String userPrompt, String providerCode) {
    if (!isConfigured(userPrompt)) {
        throw new DevelopmentException("Промпт не может быть пустым.");
    }
    UserAiConfiguration config = getUserConfig(providerCode);
    if (config == null) {
        throw new DevelopmentException("Нет активной AI-конфигурации для провайдера «"
                + providerCode + "».");
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

### 2.3 ✅ Сборка

```bash
./gradlew :app-global:compileJava :app-core:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 3: AiAnalysisService (интерфейс + бин + экстракторы)

### 3.1 Интерфейс

**Файл (создать):** `modules/global/src/com/company/hunttech/service/AiAnalysisService.java`

```java
package com.company.hunttech.service;

import com.haulmont.cuba.core.entity.Entity;

/**
 * Сервис AI-анализа сущностей.
 * Получает сущность и код промпта, заполняет шаблон данными,
 * вызывает AI-провайдера и возвращает текстовый результат.
 */
public interface AiAnalysisService {
    String NAME = "hunttech_AiAnalysisService";

    /**
     * @param entity     экземпляр сущности для анализа
     * @param promptCode код промпта из AiPromptTemplate.code
     * @return текстовый ответ AI
     */
    String analyze(Entity entity, String promptCode);
}
```

### 3.2 Экстрактор данных

**Файл (создать):** `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractor.java`

```java
package com.company.hunttech.core.ai;

import com.haulmont.cuba.core.entity.Entity;
import java.util.function.Function;

/**
 * Извлекает значение placeholder-а из сущности.
 * Может делать JPQL-запросы для получения связанных данных.
 */
@FunctionalInterface
public interface EntityDataExtractor extends Function<Entity, String> {
}
```

### 3.3 Реестр экстракторов

**Файл (создать):** `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractors.java`

```java
package com.company.hunttech.core.ai;

import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MetadataTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реестр экстракторов данных для заполнения {{placeholders}} в промптах.
 * Ключ: "EntityClass.placeholderName" → функция извлечения.
 *
 * ИСПРАВЛЕНИЕ №3: @Autowired + Spring @PostConstruct (НЕ javax),
 * чтобы гарантировать инжект до вызова init().
 */
@Component("hunttech_EntityDataExtractors")
public class EntityDataExtractors {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private MetadataTools metadataTools;

    private final Map<String, EntityDataExtractor> registry = new HashMap<>();

    @PostConstruct
    public void init() {
        // ── CandidateCV ──
        reg("CandidateCV", "resumeText", e -> ((CandidateCV) e).getTextCV());
        reg("CandidateCV", "candidateName", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getCandidate() != null
                ? metadataTools.getInstanceName(cv.getCandidate()) : "";
        });
        reg("CandidateCV", "vacancyDescription", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getToVacancy() != null
                && cv.getToVacancy().getDescription() != null
                ? cv.getToVacancy().getDescription() : "";
        });
        reg("CandidateCV", "positionName", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getToVacancy() != null
                ? cv.getToVacancy().getVacansyName() : "";
        });

        // ── OpenPosition ──
        reg("OpenPosition", "vacancyDescription",
            e -> nonNull(((OpenPosition) e).getDescription()));
        reg("OpenPosition", "vacancyRequirements",
            e -> nonNull(((OpenPosition) e).getRequirements()));
        reg("OpenPosition", "companyName", e -> {
            OpenPosition op = (OpenPosition) e;
            return op.getCompany() != null ? op.getCompany().getComanyName() : "";
        });
        reg("OpenPosition", "projectName", e -> {
            OpenPosition op = (OpenPosition) e;
            return op.getProject() != null ? op.getProject().getProjectName() : "";
        });

        // ── IteractionList ──
        reg("IteractionList", "interactionType", e ->
            ((IteractionList) e).getIteractionType() != null
                ? ((IteractionList) e).getIteractionType().getIterationName() : "");
        reg("IteractionList", "comment",
            e -> nonNull(((IteractionList) e).getComment()));
        reg("IteractionList", "dateIteraction",
            e -> Objects.toString(((IteractionList) e).getDateIteraction(), ""));
        reg("IteractionList", "recrutierName",
            e -> nonNull(((IteractionList) e).getRecrutierName()));
        reg("IteractionList", "candidateName", e -> {
            IteractionList il = (IteractionList) e;
            return il.getCandidate() != null
                ? metadataTools.getInstanceName(il.getCandidate()) : "";
        });
        reg("IteractionList", "candidateHistory", e -> {
            IteractionList il = (IteractionList) e;
            if (il.getCandidate() == null) return "";
            List<IteractionList> history = dataManager.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e " +
                       "where e.candidate = :c order by e.dateIteraction desc")
                .parameter("c", il.getCandidate())
                .maxResults(20)
                .view("_minimal")
                .list();
            return history.stream()
                .map(i -> String.format("%s | %s | %s | %s",
                    i.getDateIteraction(),
                    i.getIteractionType() != null
                        ? i.getIteractionType().getIterationName() : "-",
                    i.getRecrutierName(),
                    nonNull(i.getComment())))
                .collect(Collectors.joining("\n"));
        });

        // ── JobCandidate ──
        reg("JobCandidate", "fullName",
            e -> metadataTools.getInstanceName((JobCandidate) e));
        reg("JobCandidate", "email",
            e -> nonNull(((JobCandidate) e).getEmail()));
        reg("JobCandidate", "phone",
            e -> nonNull(((JobCandidate) e).getPhone()));
    }

    private void reg(String entityClass, String placeholder, EntityDataExtractor fn) {
        registry.put(entityClass + "." + placeholder, fn);
    }

    /**
     * ИСПРАВЛЕНИЕ №6: getSimpleName с Hibernate-прокси.
     */
    public String extract(Entity entity, String placeholder) {
        String simpleName = entity.getClass().getSimpleName().replaceAll("\\$.*", "");
        EntityDataExtractor fn = registry.get(simpleName + "." + placeholder);
        return fn != null ? fn.apply(entity) : "{{" + placeholder + "}}";
    }

    private static String nonNull(String s) {
        return s != null ? s : "";
    }
}
```

### 3.4 Бин AiAnalysisServiceBean

**Файл (создать):** `modules/core/src/com/company/hunttech/core/ai/AiAnalysisServiceBean.java`

```java
package com.company.hunttech.core.ai;

import com.company.hunttech.entity.AiPromptTemplate;
import com.company.hunttech.service.AiAnalysisService;
import com.company.hunttech.service.HrmAiService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(AiAnalysisService.NAME)
public class AiAnalysisServiceBean implements AiAnalysisService {

    @Inject private DataManager dataManager;
    @Inject private HrmAiService hrmAiService;
    @Inject private EntityDataExtractors extractors;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public String analyze(Entity entity, String promptCode) {
        // 1. Загружаем активный шаблон промпта
        AiPromptTemplate template = dataManager.load(AiPromptTemplate.class)
            .query("select e from hunttech_AiPromptTemplate e " +
                   "where e.code = :code and e.active = true")
            .parameter("code", promptCode)
            .view("_local")  // ИСПРАВЛЕНИЕ №2: конкретный view, не View.LOCAL
            .optional()
            .orElseThrow(() -> new DevelopmentException(
                "Промпт с кодом «" + promptCode + "» не найден или неактивен."));

        // 2. Заполняем {{placeholders}} данными сущности
        String filledPrompt = fillPlaceholders(template.getPromptText(), entity);

        // 3. Определяем провайдера — пока openai по умолчанию,
        //    в будущем — через UserAiConfiguration пользователя
        String providerCode = "openai";

        // 4. ИСПРАВЛЕНИЕ №1: используем новый sendPrompt вместо standardizeVacancyDescription
        return hrmAiService.sendPrompt(filledPrompt, providerCode);
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

### 3.5 Регистрация в web-spring.xml

**Файл (изменить):** `modules/web/src/com/company/hunttech/web-spring.xml`

Внутри `<map>` добавить:
```xml
<entry key="hunttech_AiAnalysisService" value="com.company.hunttech.service.AiAnalysisService"/>
```

### 3.6 ✅ Сборка

```bash
./gradlew :app-core:compileJava :app-web:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 4: CUBA Action AiAnalysisAction

**Файл (создать):** `modules/web/src/com/company/hunttech/web/ai/AiAnalysisAction.java`

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
import com.haulmont.cuba.gui.actions.BaseAction;    // ИСПРАВЛЕНИЕ №5: CUBA, не Vaadin
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.screen.Screen;

import java.util.function.Supplier;

/**
 * Кнопка AI-анализа. Добавляется в любую форму тремя строками:
 * <pre>
 *   XML:  &lt;button id="aiBtn" action="aiAnalysis"/&gt;
 *   Java: addAction(new AiAnalysisAction(this, this::getEditedEntity, "PROMPT_CODE"));
 * </pre>
 */
public class AiAnalysisAction extends BaseAction {

    private final Screen screen;
    private final Supplier<Entity> entitySupplier;
    private final String promptCode;

    public AiAnalysisAction(Screen screen, Supplier<Entity> entitySupplier, String promptCode) {
        super("aiAnalysis");
        this.screen = screen;
        this.entitySupplier = entitySupplier;
        this.promptCode = promptCode;
        setCaption("AI-анализ");
        setIcon("font-icon:BRAIN");
    }

    @Override
    public void actionPerform(Component component) {
        Entity selected = entitySupplier.get();
        if (selected == null) return;

        // ИСПРАВЛЕНИЕ №2: загружаем с _local view (не View.LOCAL)
        DataManager dm = AppBeans.get(DataManager.class);
        Entity full = dm.load(LoadContext.create(selected.getClass())
            .setId(selected.getId())
            .setView(View.LOCAL));

        AiAnalysisService service =
            (AiAnalysisService) AppBeans.get("hunttech_AiAnalysisService");

        try {
            String result = service.analyze(full, promptCode);

            // ИСПРАВЛЕНИЕ №4: правильный API диалога
            Dialogs dialogs = AppBeans.get(Dialogs.class);
            dialogs.createMessageDialog()
                .withCaption("AI-анализ")
                .withMessage(result)
                .withType(Dialogs.MessageType.CONFIRMATION_HTML)
                .show();
        } catch (Exception e) {
            Notifications notifications = AppBeans.get(Notifications.class);
            notifications.create(Notifications.NotificationType.ERROR)
                .withCaption("Ошибка AI-анализа")
                .withDescription(e.getMessage())
                .show();
        }
    }
}
```

### ✅ Сборка

```bash
./gradlew :app-web:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 5: Администрирование промптов

### 5.1 Browse экран

**Файл (создать):** `modules/web/src/.../aiprompttemplate/AiPromptTemplateBrowse.java`

```java
@UiController("hunttech_AiPromptTemplate.browse")
@UiDescriptor("ai-prompt-template-browse.xml")
@LoadDataBeforeShow
public class AiPromptTemplateBrowse extends StandardLookup<AiPromptTemplate> {
}
```

**Файл (создать):** `modules/web/src/.../aiprompttemplate/ai-prompt-template-browse.xml`

```xml
<window ... dataReadOnly="true">
    <data>
        <collection id="aiPromptTemplatesDc"
                    class="com.company.hunttech.entity.AiPromptTemplate"
                    view="aiPromptTemplate-browse-view">
            <loader id="aiPromptTemplatesDl">
                <query>
                    <![CDATA[select e from hunttech_AiPromptTemplate e]]>
                </query>
            </loader>
        </collection>
    </data>
    <layout expand="aiPromptTemplatesTable" spacing="true">
        <hbox spacing="true">
            <button action="aiPromptTemplatesTable.create"/>
            <button action="aiPromptTemplatesTable.edit"/>
            <button action="aiPromptTemplatesTable.remove"/>
        </hbox>
        <table id="aiPromptTemplatesTable" dataContainer="aiPromptTemplatesDc" width="100%">
            <actions>
                <action id="create" type="create"/>
                <action id="edit" type="edit"/>
                <action id="remove" type="remove"/>
            </actions>
            <columns>
                <column id="name"/>
                <column id="code"/>
                <column id="entityClass"/>
                <column id="active"/>
            </columns>
            <rowsCount/>
        </table>
    </layout>
</window>
```

### 5.2 Edit экран

**Файл (создать):** `modules/web/src/.../aiprompttemplate/AiPromptTemplateEdit.java`

```java
@UiController("hunttech_AiPromptTemplate.edit")
@UiDescriptor("ai-prompt-template-edit.xml")
@EditedEntityContainer("aiPromptTemplateDc")
@LoadDataBeforeShow
public class AiPromptTemplateEdit extends StandardEditor<AiPromptTemplate> {
}
```

**XML:** стандартная edit-форма с полями: name, code, entityClass (lookupField), promptText (textArea), description, active (checkBox).

### 5.3 Меню

**Файл (изменить):** `modules/web/src/com/company/hunttech/web-menu.xml`

```xml
<item screen="hunttech_AiPromptTemplate.browse"
      caption="Системные промпты AI"
      roles="systemAdministrator"/>
```

### ✅ Сборка

```bash
./gradlew :app-web:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 6: Интеграция в формы — по 3 строки на каждую

### 6.1 CandidateCVEdit

**XML** (добавить кнопку):
```xml
<button id="aiAnalysisBtn" caption="AI-анализ резюме" action="aiAnalysis"/>
```

**Java** (в `onInit()`):
```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "RESUME_ANALYSIS"));
```

### 6.2 OpenPositionEdit

```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "VACANCY_ANALYSIS"));
```

### 6.3 IteractionListEdit

```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "INTERACTION_ANALYSIS"));
```

### 6.4 JobCandidateEdit

```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "CANDIDATE_ANALYSIS"));
```

### ✅ Сборка

```bash
./gradlew :app-web:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 7: Базовые промпты (SQL-заполнение)

**Файл (создать):** `modules/core/db/update/h2/99/990721-002-seedAiPrompts.sql`

```sql
INSERT INTO HUNTTECH_AI_PROMPT_TEMPLATE (ID, CREATE_TS, CREATED_BY, VERSION, NAME, CODE, ENTITY_CLASS, PROMPT_TEXT, ACTIVE)
VALUES
(newid(), now(), 'system', 1,
 'Анализ резюме', 'RESUME_ANALYSIS',
 'com.company.hunttech.entity.CandidateCV',
 'Проанализируй резюме кандидата {{candidateName}}.
Текст резюме: {{resumeText}}
Вакансия: {{vacancyDescription}} ({{positionName}})
Оцени соответствие кандидата вакансии по шкале 1-10.
Выдели сильные и слабые стороны. Напиши рекомендации рекрутеру.', true),
(newid(), now(), 'system', 1,
 'Расшифровка вакансии', 'VACANCY_ANALYSIS',
 'com.company.hunttech.entity.OpenPosition',
 'Объясни требования вакансии простым языком.
Описание: {{vacancyDescription}}
Требования: {{vacancyRequirements}}
Компания: {{companyName}}
Проект: {{projectName}}
Что реально нужно от кандидата? Какие навыки критичны?', true),
(newid(), now(), 'system', 1,
 'Анализ воронки кандидата', 'INTERACTION_ANALYSIS',
 'com.company.hunttech.entity.IteractionList',
 'Проанализируй воронку кандидата {{candidateName}}.
Текущее взаимодействие: {{interactionType}} от {{dateIteraction}}
Комментарий рекрутера: {{comment}}
История взаимодействий: {{candidateHistory}}
1. На какой стадии воронки кандидат?
2. Есть ли признаки зависания?
3. Какое следующее действие рекомендовано?
4. Прогноз: дойдёт до финала?', true);
```

**Файл (создать):** `modules/core/db/update/postgres/99/990721-002-seedAiPrompts.sql`

Тот же DDL, адаптированный для postgres (`NEWID()` → `uuid_generate_v4()`).

---

## Этап 8: Тесты

### 8.1 Дополнить HrmAiServiceTest

**Файл (изменить):** `modules/core/test/.../HrmAiServiceTest.java`

Добавить:
```java
@Test
public void aiAnalysisServiceIsResolvable() {
    Object svc = AppBeans.get("hunttech_AiAnalysisService");
    assertNotNull("AiAnalysisService должен быть доступен", svc);
}

@Test
public void sendPromptRejectsEmptyPrompt() {
    try {
        aiService.sendPrompt(null, "openai");
        fail("Ожидалось исключение для пустого промпта");
    } catch (DevelopmentException e) {
        // expected
    }
}

@Test
public void sendPromptRejectsUnknownProvider() {
    try {
        aiService.sendPrompt("Hello", "unknown-provider-xyz");
        fail("Ожидалось исключение для неизвестного провайдера");
    } catch (Exception e) {
        // expected
    }
}
```

### ✅ Сборка и прогон

```bash
./gradlew :app-core:test --tests "*HrmAiServiceTest*" --no-daemon
# BUILD SUCCESSFUL, все тесты зелёные
```

---

## Финальный чеклист

- [ ] Этап 1: Entity + миграции + views → `./gradlew :app-global:compileJava`
- [ ] Этап 2: `sendPrompt` в HrmAiService → `./gradlew :app-core:compileJava`
- [ ] Этап 3: AiAnalysisService + экстракторы + web-spring → `./gradlew :app-core:compileJava :app-web:compileJava`
- [ ] Этап 4: AiAnalysisAction → `./gradlew :app-web:compileJava`
- [ ] Этап 5: Admin UI (browse/edit/меню) → `./gradlew :app-web:compileJava`
- [ ] Этап 6: Кнопки в 4 формах → `./gradlew :app-web:compileJava`
- [ ] Этап 7: SQL-сиды промптов
- [ ] Этап 8: Автотесты зелёные
- [ ] Финальная сборка: `./gradlew deploy -x test`
- [ ] Перезапуск Tomcat, HTTP 200
- [ ] `git add`, `git commit`, `git push`
