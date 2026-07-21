# План реализации: AI-анализ сущностей (строгий)

> **Ветка:** `feat/ai-entity-analysis`  
> **Дата:** 2026-07-21  
> **Стек:** CUBA 7.3, Java 11, Gradle, PostgreSQL  
> **Архитектура:** `docs/ai/AI_ANALYSIS.md`

---

## Правила реализации (обязательные)

1. **Следовать плану строго по порядку этапов.**
2. **После каждого этапа — сборка (`./gradlew compileJava`).**
3. **Не менять существующий код без явной необходимости.**
4. **Не менять бизнес-логику, запросы, загрузчики, проверки, сохранение данных.**
5. **Не коммитить, пока пользователь явно не попросит.**
6. **Каждый новый файл — с комментарием на русском о назначении.**
7. **Все @Inject — только для компонентов tabMain (не lazy-табов).**
8. **Новые сущности — стандартный CUBA-паттерн: global entity → core service → web screen.**

---

## Этап 1: Entity AiPromptTemplate

### 1.1 Создать класс сущности

**Файл:** `modules/global/src/com/company/hunttech/entity/AiPromptTemplate.java`

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

    // getters / setters
}
```

### 1.2 Добавить в persistence.xml

**Файл:** `modules/global/src/com/company/hunttech/persistence.xml`

Добавить строку `<class>com.company.hunttech.entity.AiPromptTemplate</class>` внутрь `<persistence-unit>`.

### 1.3 SQL-миграции

**Файл:** `modules/core/db/update/h2/99/990721-001-createAiPromptTemplate.sql`
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

**Файл:** `modules/core/db/update/postgres/99/990721-001-createAiPromptTemplate.sql`
— тот же DDL, адаптированный для postgres.

### 1.4 Views

**Файл:** `modules/global/src/com/company/hunttech/views.xml`

```xml
<view class="com.company.hunttech.entity.AiPromptTemplate" extends="_local" name="aiPromptTemplate-browse-view">
    <property name="name"/>
    <property name="code"/>
    <property name="entityClass"/>
    <property name="active"/>
</view>

<view class="com.company.hunttech.entity.AiPromptTemplate" extends="_local" name="aiPromptTemplate-edit-view">
    <property name="name"/>
    <property name="code"/>
    <property name="entityClass"/>
    <property name="promptText"/>
    <property name="description"/>
    <property name="active"/>
</view>
```

### 1.5 Сборка и проверка

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-global:compileJava --no-daemon
# должен быть BUILD SUCCESSFUL
```

---

## Этап 2: AiAnalysisService (интерфейс + бин)

### 2.1 Интерфейс

**Файл:** `modules/global/src/com/company/hunttech/service/AiAnalysisService.java`

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

### 2.2 Экстрактор данных

**Файл:** `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractor.java`

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

### 2.3 Реестр экстракторов

**Файл:** `modules/core/src/com/company/hunttech/core/ai/EntityDataExtractors.java`

```java
package com.company.hunttech.core.ai;

import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MetadataTools;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реестр экстракторов данных для заполнения {{placeholders}} в промптах.
 * Ключ: "EntityClass.placeholderName" → функция извлечения.
 */
@Component("hunttech_EntityDataExtractors")
public class EntityDataExtractors {

    @Inject private DataManager dataManager;
    @Inject private MetadataTools metadataTools;

    private final Map<String, EntityDataExtractor> registry = new HashMap<>();

    // Вызывается после инжекта бинов
    @javax.annotation.PostConstruct
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
                ? cv.getToVacancy().getDescription() != null
                    ? cv.getToVacancy().getDescription() : ""
                : "";
        });
        reg("CandidateCV", "positionName", e -> {
            CandidateCV cv = (CandidateCV) e;
            return cv.getToVacancy() != null
                ? cv.getToVacancy().getVacansyName() : "";
        });

        // ── OpenPosition ──
        reg("OpenPosition", "vacancyDescription",
            e -> ((OpenPosition) e).getDescription() != null
                ? ((OpenPosition) e).getDescription() : "");
        reg("OpenPosition", "vacancyRequirements",
            e -> ((OpenPosition) e).getRequirements() != null
                ? ((OpenPosition) e).getRequirements() : "");
        reg("OpenPosition", "companyName", e -> {
            OpenPosition op = (OpenPosition) e;
            return op.getCompany() != null
                ? op.getCompany().getComanyName() : "";
        });
        reg("OpenPosition", "projectName", e -> {
            OpenPosition op = (OpenPosition) e;
            return op.getProject() != null
                ? op.getProject().getProjectName() : "";
        });

        // ── IteractionList ──
        reg("IteractionList", "interactionType",
            e -> ((IteractionList) e).getIteractionType() != null
                ? ((IteractionList) e).getIteractionType().getIterationName() : "");
        reg("IteractionList", "comment",
            e -> ((IteractionList) e).getComment() != null
                ? ((IteractionList) e).getComment() : "");
        reg("IteractionList", "dateIteraction",
            e -> Objects.toString(((IteractionList) e).getDateIteraction(), ""));
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
                .list();
            return history.stream()
                .map(i -> String.format("%s | %s | %s | %s",
                    i.getDateIteraction(),
                    i.getIteractionType() != null ? i.getIteractionType().getIterationName() : "-",
                    i.getRecrutierName(),
                    i.getComment() != null ? i.getComment() : ""))
                .collect(Collectors.joining("\n"));
        });

        // ── JobCandidate ──
        reg("JobCandidate", "fullName",
            e -> metadataTools.getInstanceName((JobCandidate) e));
        reg("JobCandidate", "email",
            e -> ((JobCandidate) e).getEmail() != null
                ? ((JobCandidate) e).getEmail() : "");
        reg("JobCandidate", "phone",
            e -> ((JobCandidate) e).getPhone() != null
                ? ((JobCandidate) e).getPhone() : "");
    }

    private void reg(String entityClass, String placeholder, EntityDataExtractor fn) {
        registry.put(entityClass + "." + placeholder, fn);
    }

    public String extract(Entity entity, String placeholder) {
        String simpleName = entity.getClass().getSimpleName();
        EntityDataExtractor fn = registry.get(simpleName + "." + placeholder);
        return fn != null ? fn.apply(entity) : "{{" + placeholder + "}}";
    }
}
```

### 2.4 Бин AiAnalysisServiceBean

**Файл:** `modules/core/src/com/company/hunttech/core/ai/AiAnalysisServiceBean.java`

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
import java.util.Collections;
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
        // 1. Загружаем шаблон промпта
        AiPromptTemplate template = dataManager.load(AiPromptTemplate.class)
            .query("select e from hunttech_AiPromptTemplate e where e.code = :code and e.active = true")
            .parameter("code", promptCode)
            .optional()
            .orElseThrow(() -> new DevelopmentException(
                "Промпт с кодом «" + promptCode + "» не найден или неактивен."));

        // 2. Заполняем {{placeholders}}
        String filledPrompt = fillTemplate(template.getPromptText(), entity);

        // 3. Получаем активного провайдера текущего пользователя
        //    (используем первый доступный, или yandex по умолчанию)
        String providerCode = resolveProviderCode(entity);

        // 4. Вызываем AI через HrmAiService
        //    (используем встроенный механизм без явной UserAiConfiguration —
        //     HrmAiService сам определит конфигурацию пользователя)
        return hrmAiService.standardizeVacancyDescription(filledPrompt, providerCode);
    }

    private String fillTemplate(String template, Entity entity) {
        String result = template;
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) {
            String placeholder = m.group(1);
            String value = extractors.extract(entity, placeholder);
            result = result.replace("{{" + placeholder + "}}", value);
        }
        return result;
    }

    private String resolveProviderCode(Entity entity) {
        return "openai"; // пока hardcode, потом — через UserAiConfiguration
    }
}
```

### 2.5 Регистрация в web-spring.xml

**Файл:** `modules/web/src/com/company/hunttech/web-spring.xml`

Добавить в `<map>` внутри `<bean id="hunttech_proxyCreator">`:

```xml
<entry key="hunttech_AiAnalysisService" value="com.company.hunttech.service.AiAnalysisService"/>
```

### 2.6 Сборка

```bash
./gradlew :app-core:compileJava :app-web:compileJava --no-daemon
# BUILD SUCCESSFUL
```

---

## Этап 3: CUBA Action AiAnalysisAction

**Файл:** `modules/web/src/com/company/hunttech/web/ai/AiAnalysisAction.java`

```java
package com.company.hunttech.web.ai;

import com.company.hunttech.service.AiAnalysisService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.screen.Screen;

import java.util.function.Supplier;

/**
 * Кнопка AI-анализа. Добавляется в любую форму тремя строками:
 *   XML:  <button id="aiBtn" action="aiAnalysis"/>
 *   Java: addAction(new AiAnalysisAction(this, this::getEditedEntity, "PROMPT_CODE"));
 */
public class AiAnalysisAction extends BaseAction {

    public AiAnalysisAction(Screen screen, Supplier<Entity> entitySupplier, String promptCode) {
        super("aiAnalysis");
        this.screen = screen;
        this.entitySupplier = entitySupplier;
        this.promptCode = promptCode;
    }

    @Override
    public void actionPerform(Component component) {
        Entity selected = entitySupplier.get();
        if (selected == null) return;

        // Перезагружаем с View.LOCAL — browse-view может не содержать всех полей
        DataManager dm = AppBeans.get(DataManager.class);
        Entity full = dm.load(LoadContext.create(selected.getClass())
            .setId(selected.getId())
            .setView(View.LOCAL));

        AiAnalysisService service = (AiAnalysisService) AppBeans.get("hunttech_AiAnalysisService");
        try {
            String result = service.analyze(full, promptCode);
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

Внимание: уточнить типы в CUBA 7.3 для `BaseAction`, `Dialogs.createMessageDialog()`.

---

## Этап 4: Администрирование промптов

### 4.1 AiPromptTemplateBrowse

**Файл:** `modules/web/src/.../aiprompttemplate/AiPromptTemplateBrowse.java`
```java
@UiController("hunttech_AiPromptTemplate.browse")
@UiDescriptor("ai-prompt-template-browse.xml")
@LoadDataBeforeShow
public class AiPromptTemplateBrowse extends StandardLookup<AiPromptTemplate> {
}
```

**Файл:** `modules/web/src/.../aiprompttemplate/ai-prompt-template-browse.xml`
```xml
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
```

### 4.2 AiPromptTemplateEdit

**Файл:** `modules/web/src/.../aiprompttemplate/AiPromptTemplateEdit.java`
```java
@UiController("hunttech_AiPromptTemplate.edit")
@UiDescriptor("ai-prompt-template-edit.xml")
@EditedEntityContainer("aiPromptTemplateDc")
@LoadDataBeforeShow
public class AiPromptTemplateEdit extends StandardEditor<AiPromptTemplate> {
}
```

**XML:** standard edit form with `providerCode`, `apiKey`, etc.

### 4.3 Меню

**Файл:** `modules/web/src/com/company/hunttech/web-menu.xml`

```xml
<item screen="hunttech_AiPromptTemplate.browse"
      caption="Системные промпты AI"
      roles="systemAdministrator"/>
```

---

## Этап 5: Интеграция в формы — по 3 строки на каждую

### 5.1 CandidateCVEdit

**XML** (`modules/web/src/.../candidatecv/candidate-cv-edit.xml`):
```xml
<button id="aiAnalysisBtn" caption="AI-анализ резюме" action="aiAnalysis"/>
```

**Java** (`CandidateCVEdit.java`, в `onInit()`):
```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "RESUME_ANALYSIS"));
```

### 5.2 OpenPositionEdit

```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "VACANCY_ANALYSIS"));
```

### 5.3 IteractionListEdit

```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "INTERACTION_ANALYSIS"));
```

### 5.4 JobCandidateEdit

```java
addAction(new AiAnalysisAction(this, this::getEditedEntity, "CANDIDATE_ANALYSIS"));
```

---

## Этап 6: Базовые промпты (SQL-заполнение)

Скрипт миграции `990721-002-seedAiPrompts.sql`:

```sql
INSERT INTO HUNTTECH_AI_PROMPT_TEMPLATE (ID, CREATE_TS, CREATED_BY, VERSION, NAME, CODE, ENTITY_CLASS, PROMPT_TEXT, ACTIVE)
VALUES
-- Анализ резюме
(newid(), now(), 'system', 1,
 'Анализ резюме', 'RESUME_ANALYSIS',
 'com.company.hunttech.entity.CandidateCV',
 'Проанализируй резюме кандидата {{candidateName}}.
Текст резюме: {{resumeText}}
Вакансия: {{vacancyDescription}} ({{positionName}})
Оцени соответствие кандидата вакансии по шкале 1-10.
Выдели сильные и слабые стороны. Напиши рекомендации рекрутеру.', true),

-- Расшифровка вакансии
(newid(), now(), 'system', 1,
 'Расшифровка вакансии', 'VACANCY_ANALYSIS',
 'com.company.hunttech.entity.OpenPosition',
 'Объясни требования вакансии простым языком.
Описание: {{vacancyDescription}}
Требования: {{vacancyRequirements}}
Компания: {{companyName}}
Проект: {{projectName}}
Что реально нужно от кандидата? Какие навыки критичны?', true),

-- Анализ взаимодействий
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

---

## Этап 7: Тесты

### 7.1 HrmAiServiceTest (дополнить)

Добавить тест: `AiAnalysisService` доступен через `AppBeans`:

```java
@Test
public void aiAnalysisServiceIsResolvable() {
    Object svc = AppBeans.get("hunttech_AiAnalysisService");
    assertNotNull(svc);
}
```

### 7.2 WebRegistrationTest (дополнить)

Проверить наличие в `web-spring.xml`:
```xml
<entry key="hunttech_AiAnalysisService" value="com.company.hunttech.service.AiAnalysisService"/>
```

---

## Чеклист перед завершением

- [ ] Этап 1: Entity + миграция + views → `./gradlew :app-global:compileJava`
- [ ] Этап 2: Service + extractors + web-spring → `./gradlew :app-core:compileJava :app-web:compileJava`
- [ ] Этап 3: AiAnalysisAction → `./gradlew :app-web:compileJava`
- [ ] Этап 4: Admin UI (browse/edit/меню) → `./gradlew :app-web:compileJava`
- [ ] Этап 5: Кнопки в 4 формах → `./gradlew :app-web:compileJava`
- [ ] Этап 6: SQL-сиды
- [ ] Этап 7: Тесты → `./gradlew :app-core:test --tests "*HrmAiServiceTest*"`
- [ ] Финальная сборка: `./gradlew deploy -x test`
- [ ] Перезапуск Tomcat, HTTP 200
- [ ] Ручная проверка: открыть CandidateCVEdit → кнопка «AI-анализ» → результат
