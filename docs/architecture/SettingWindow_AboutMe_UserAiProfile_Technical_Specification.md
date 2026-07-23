# Техническое задание: вкладка «Обо мне» и профиль персонализации ИИ

> Проект: **HRM HuntTech**  
> Платформа: **CUBA Platform 7.3-SNAPSHOT**, Java, PostgreSQL  
> Экран: `ExtSettingsWindow` / `ext-settings-window.xml`  
> Статус документа: утверждённое техническое задание на проектирование и реализацию первого этапа  
> Базовый `master` на момент подготовки: `a455911ee26fb913a02493fc727f59cc2596b2bb`  
> Дата: `2026-07-22`

---

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

Вкладка «Обо мне» должна формировать управляемый профессиональный профиль пользователя HRM HuntTech, который ИИ-сервисы используют для адаптации формы, глубины и терминологии ответов.

Профиль нужен, чтобы ответы учитывали:

- должность и функциональную роль пользователя;
- образование и профессиональный опыт;
- рекрутинговую специализацию;
- отраслевую и техническую экспертизу;
- текущие профессиональные цели;
- предпочитаемый язык, стиль и степень детализации;
- индивидуальные требования к структуре ответа.

Профиль **не является источником объективных фактов о кандидатах, вакансиях, клиентах и проектах**. Он не должен менять права доступа, бизнес-правила, критерии соответствия или решения по кандидатам.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Путь пользователя:

```text
Главное окно HRM HuntTech
→ меню пользователя
→ Настройки
→ вкладка «Обо мне»
```

Экран использует:

- `ExtUser` — имя, аватар и системную учётную запись;
- `UserAiProfile` — профессиональный контекст и предпочтения ответа;
- `UserAiConfiguration` — отдельные настройки провайдера, модели и API-ключа;
- `UserSettings` — существующие общие и почтовые настройки.

Вкладка «Обо мне» не должна отображать и тем более передавать:

- API-ключи;
- пароли почтовых серверов;
- системные роли как текстовые инструкции модели;
- сведения, не разрешённые пользователем к внешней обработке.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

```text
Открытие вкладки
→ профиль существует
→ поля заполняются сохранёнными значениями.

Открытие вкладки
→ профиль отсутствует
→ создаётся несохранённый профиль с безопасными значениями по умолчанию.

Включение персонализации
→ согласие на внешнюю обработку не подтверждено
→ профиль не активируется, пользователь получает понятное уведомление.

Включение персонализации
→ согласие подтверждено
→ профиль становится доступен сервису формирования ИИ-контекста.

Нажатие «Предпросмотр контекста»
→ система формирует очищенный контекст без внешнего HTTP-запроса
→ пользователь видит точный состав данных, разрешённых к передаче.

Сохранение
→ профиль валиден
→ UserAiProfile сохраняется вместе с остальными настройками пользователя.

Отключение персонализации
→ данные профиля сохраняются
→ ИИ-сервисы перестают использовать профиль.
```

---

# 1. Цель первого этапа

Реализовать в `ExtSettingsWindow` полноценную вкладку «Обо мне» и отдельную сущность `UserAiProfile`.

Первый этап включает:

1. модель данных;
2. Liquibase-миграцию;
3. CUBA views;
4. загрузку и сохранение профиля;
5. визуальную форму;
6. локальный SCSS;
7. предпросмотр очищенного ИИ-контекста;
8. тесты;
9. living-документацию.

Первый этап **не включает** вызовы OpenAI или других внешних LLM-провайдеров.

---

# 2. Обязательная предварительная проверка

Перед изменениями исполнитель обязан:

1. Получить актуальный `master`.
2. Зафиксировать точный HEAD.
3. Проверить наличие новых коммитов Hermes.
4. Проверить последний отчёт Hermes.
5. Проверить отсутствие незакоммиченных изменений.
6. Создать отдельную ветку:

```text
agent/setting-window-about-me-ai-profile
```

7. Не продолжать от SHA, сохранённого в этом документе, если фактический `master` уже изменился.

Источник истины — фактический HEAD GitHub на момент начала реализации.

---

# 3. Разрешённая область изменений

## 3.1. Global-модуль

Разрешено создать и изменить:

```text
modules/global/src/com/company/itpearls/entity/UserAiProfile.java
modules/global/src/com/company/itpearls/entity/AiFunctionalRole.java
modules/global/src/com/company/itpearls/entity/AiSeniorityLevel.java
modules/global/src/com/company/itpearls/entity/AiPreferredLanguage.java
modules/global/src/com/company/itpearls/entity/AiResponseDetailLevel.java
modules/global/src/com/company/itpearls/entity/AiCommunicationStyle.java
modules/global/src/com/company/itpearls/entity/AiTerminologyLevel.java
modules/global/src/com/company/itpearls/entity/AiAnswerStructure.java
modules/global/src/com/company/itpearls/persistence.xml
modules/global/src/com/company/itpearls/views.xml
modules/global/src/com/company/itpearls/entity/messages.properties
modules/global/src/com/company/itpearls/entity/messages_ru.properties
```

## 3.2. Core-модуль

Разрешено создать и изменить:

```text
modules/core/db/changelog/260722-1-addUserAiProfile.xml
modules/core/db/changelog/db.changelog-master.xml
modules/core/src/com/company/itpearls/service/UserAiContextServiceBean.java
modules/core/test/com/company/itpearls/service/UserAiContextServiceBeanTest.java
```

При необходимости путь теста привести к фактической структуре `modules/core/test/`, не создавая параллельный тестовый каталог.

## 3.3. Global service contract

Разрешено создать:

```text
modules/global/src/com/company/itpearls/service/UserAiContextService.java
modules/global/src/com/company/itpearls/service/dto/AiUserContext.java
```

DTO может быть размещён в другом уже существующем пакете DTO, если в проекте есть установленная структура. Создавать новый архитектурный слой без необходимости запрещено.

## 3.4. Web-модуль

Разрешено изменить:

```text
modules/web/src/com/company/itpearls/web/screens/extsettingswindow/ext-settings-window.xml
modules/web/src/com/company/itpearls/web/screens/extsettingswindow/ExtSettingsWindow.java
modules/web/src/com/company/itpearls/web/screens/extsettingswindow/messages.properties
modules/web/src/com/company/itpearls/web/screens/extsettingswindow/messages_ru.properties
modules/web/themes/hover/com.company.itpearls/hover-ext.scss
```

## 3.5. Документация

Обязательно создать или обновить:

```text
docs/entities/UserAiProfile.md
docs/ui/ExtSettingsWindow_Spec.md
docs/services/UserAiContextService.md
docs/README.md
docs/ui/README.md
```

Если фактическое каноническое имя UI Spec в репозитории отличается, использовать существующее имя и не создавать дубликат.

---

# 4. Запрещённая область

Без отдельного разрешения Алексея запрещено:

- менять `JobCandidateEdit.java` и `job-candidate-edit.xml`;
- менять бизнес-логику вакансий, кандидатов и взаимодействий;
- менять существующие поля `ExtUser`, `UserSettings` и `UserAiConfiguration`;
- переносить API-ключи между сущностями;
- менять существующие component ID вкладок «Интерфейс» и почтовых настроек;
- менять существующие действия базового `SettingsWindow`;
- менять глобальные стили `.v-*` без локального корневого селектора;
- подключать внешний LLM;
- выполнять скрытое профилирование пользователя;
- автоматически изменять профиль на основании истории запросов;
- использовать профиль для ранжирования кандидатов;
- добавлять поля чувствительных персональных данных;
- менять production;
- запускать Liquibase на production;
- переименовывать legacy-идентификаторы `itpearls_*`, `ITPEARLS_*`, `com.company.itpearls`.

---

# 5. Модель данных `UserAiProfile`

## 5.1. Сущность

```java
@Table(
    name = "ITPEARLS_USER_AI_PROFILE",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "IDX_ITPEARLS_USER_AI_PROFILE_UNQ_USER",
            columnNames = "USER_ID"
        )
    }
)
@Entity(name = "itpearls_UserAiProfile")
@NamePattern("%s|user")
public class UserAiProfile extends StandardEntity {
}
```

Связь:

```java
@NotNull
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "USER_ID", nullable = false, unique = true)
private ExtUser user;
```

Один пользователь может иметь не более одного профиля.

## 5.2. Поля управления

| Java-поле | Колонка БД | Тип | Default | Обязательность |
|---|---|---|---|---|
| `user` | `USER_ID` | UUID FK | — | да |
| `profileEnabled` | `PROFILE_ENABLED` | Boolean | `false` | да |
| `externalProcessingAllowed` | `EXTERNAL_PROCESSING_ALLOWED` | Boolean | `false` | да |
| `consentVersion` | `CONSENT_VERSION` | varchar(32) | `null` | нет |
| `consentAcceptedAt` | `CONSENT_ACCEPTED_AT` | timestamp | `null` | нет |
| `profileConfirmedAt` | `PROFILE_CONFIRMED_AT` | timestamp | `null` | нет |

## 5.3. Профессиональный профиль

| Java-поле | Колонка БД | JPA-тип | UI-лимит |
|---|---|---|---|
| `aboutMe` | `ABOUT_ME` | `@Lob String` | 2000 |
| `currentPosition` | `CURRENT_POSITION` | String(255) | 255 |
| `functionalRole` | `FUNCTIONAL_ROLE` | Integer enum | — |
| `seniorityLevel` | `SENIORITY_LEVEL` | Integer enum | — |
| `professionalExperienceYears` | `PROFESSIONAL_EXPERIENCE_YEARS` | Integer | 0–70 |
| `recruitingExperienceYears` | `RECRUITING_EXPERIENCE_YEARS` | Integer | 0–70 |
| `currentResponsibilities` | `CURRENT_RESPONSIBILITIES` | `@Lob String` | 4000 |
| `education` | `EDUCATION` | `@Lob String` | 4000 |
| `certifications` | `CERTIFICATIONS` | `@Lob String` | 4000 |
| `domainExpertise` | `DOMAIN_EXPERTISE` | `@Lob String` | 4000 |
| `industries` | `INDUSTRIES` | `@Lob String` | 4000 |

## 5.4. Рекрутинговая специализация

| Java-поле | Колонка БД | JPA-тип | UI-лимит |
|---|---|---|---|
| `recruitingSpecializations` | `RECRUITING_SPECIALIZATIONS` | `@Lob String` | 4000 |
| `targetRoles` | `TARGET_ROLES` | `@Lob String` | 4000 |
| `candidateLevels` | `CANDIDATE_LEVELS` | String(255) | 255 |
| `hiringGeographies` | `HIRING_GEOGRAPHIES` | `@Lob String` | 2000 |
| `decisionPriorities` | `DECISION_PRIORITIES` | `@Lob String` | 4000 |
| `clientAndProjectContext` | `CLIENT_PROJECT_CONTEXT` | `@Lob String` | 4000 |

## 5.5. Цели и интересы

| Java-поле | Колонка БД | JPA-тип | UI-лимит |
|---|---|---|---|
| `professionalGoals` | `PROFESSIONAL_GOALS` | `@Lob String` | 4000 |
| `professionalInterests` | `PROFESSIONAL_INTERESTS` | `@Lob String` | 4000 |
| `developmentAreas` | `DEVELOPMENT_AREAS` | `@Lob String` | 4000 |
| `currentPriorities` | `CURRENT_PRIORITIES` | `@Lob String` | 4000 |

## 5.6. Настройки ответа

| Java-поле | Колонка БД | JPA-тип | UI-лимит |
|---|---|---|---|
| `preferredLanguage` | `PREFERRED_LANGUAGE` | Integer enum | — |
| `responseDetailLevel` | `RESPONSE_DETAIL_LEVEL` | Integer enum | — |
| `communicationStyle` | `COMMUNICATION_STYLE` | Integer enum | — |
| `terminologyLevel` | `TERMINOLOGY_LEVEL` | Integer enum | — |
| `preferredAnswerStructure` | `PREFERRED_ANSWER_STRUCTURE` | Integer enum | — |
| `customAiInstructions` | `CUSTOM_AI_INSTRUCTIONS` | `@Lob String` | 4000 |
| `communicationConstraints` | `COMMUNICATION_CONSTRAINTS` | `@Lob String` | 2000 |

## 5.7. Enum

### `AiFunctionalRole`

```text
RECRUITER
RESEARCHER
RECRUITMENT_LEAD
ACCOUNT_MANAGER
HR_MANAGER
TECHNICAL_EXPERT
PROJECT_MANAGER
EXECUTIVE
OTHER
```

### `AiSeniorityLevel`

```text
JUNIOR
MIDDLE
SENIOR
LEAD
HEAD
EXECUTIVE
```

### `AiPreferredLanguage`

```text
AUTO
RUSSIAN
ENGLISH
```

### `AiResponseDetailLevel`

```text
BRIEF
BALANCED
DETAILED
```

### `AiCommunicationStyle`

```text
DIRECT
NEUTRAL
COACHING
```

### `AiTerminologyLevel`

```text
PLAIN
PROFESSIONAL
EXPERT
```

### `AiAnswerStructure`

```text
AUTO
EXECUTIVE_SUMMARY
ACTION_PLAN
STEP_BY_STEP
CHECKLIST
TABLE
```

Enum реализовать в принятом для CUBA 7.3 стиле проекта: `EnumClass<Integer>`, стабильные ID, локализованные captions.

---

# 6. Liquibase

Создать:

```text
modules/core/db/changelog/260722-1-addUserAiProfile.xml
```

Требования:

1. Таблица `ITPEARLS_USER_AI_PROFILE`.
2. Стандартные системные поля CUBA `StandardEntity`.
3. FK на `SEC_USER`/фактическую таблицу наследуемого `ExtUser`.
4. Уникальность `USER_ID`.
5. Индекс на `USER_ID`, если уникальное ограничение не создаёт пригодный индекс в целевой PostgreSQL.
6. Все Boolean default задать явно.
7. ChangeSet должен быть идемпотентным в рамках стандартного Liquibase.
8. Включить файл в `db.changelog-master.xml`.
9. Не изменять старые changeSet.
10. Не добавлять данные профиля seed-скриптом.

---

# 7. CUBA views и persistence

## 7.1. Persistence

Зарегистрировать `UserAiProfile` в:

```text
modules/global/src/com/company/itpearls/persistence.xml
```

## 7.2. Views

Создать:

```xml
<view entity="itpearls_UserAiProfile"
      name="userAiProfile-view"
      extends="_local">
    <property name="user" view="extUser-picker-view"/>
</view>
```

При необходимости создать отдельный узкий `userAiProfile-context-view`, который исключает поля, не нужные сервису предпросмотра.

Требования:

- не использовать `_local` на `ExtUser`;
- не загружать `userRoles`, почтовые пароли и AI API-ключи;
- все getters контроллера и сервиса должны входить в используемый view;
- проверить Data View Integrity.

---

# 8. Экран `ExtSettingsWindow`

## 8.1. Сохранение существующего контракта

Сохранить без переименования:

```text
settingsTabSheet
msgMyInfo
msgInterface
mailAccessTab
okBtn
cancelBtn
userAvatarUpload
userPic
defaultPic
extUserDs
userSettingsDs
```

Существующая логика аватара должна продолжить работать.

## 8.2. Новый datasource

Добавить legacy datasource, совместимый с текущим экраном:

```xml
<datasource id="userAiProfileDs"
            class="com.company.itpearls.entity.UserAiProfile"
            view="userAiProfile-view"
            allowCommit="true"/>
```

Не выполнять частичную миграцию только одной вкладки на новый Data API, если весь экран остаётся на `dsContext`.

## 8.3. Layout

Вкладка `msgMyInfo` должна быть перестроена по утверждённому направлению дизайна `JobCandidateEdit`:

```text
┌──────────────────────────┬────────────────────────────────────────────┐
│ Левая профильная панель  │ Правая рабочая область                    │
│ 260–280 px               │ toolbar + вертикальные секции             │
└──────────────────────────┴────────────────────────────────────────────┘
```

### Левая панель

Содержит:

- аватар;
- имя пользователя;
- текущую должность;
- системную роль только для отображения;
- статус профиля;
- процент заполнения;
- дату подтверждения;
- вертикальную навигацию по секциям;
- предупреждение о запрещённых данных.

### Правая область

Содержит:

1. заголовок и пояснение;
2. toolbar;
3. секцию «Профессиональный профиль»;
4. секцию «Рекрутинговая специализация»;
5. секцию «Как мне отвечать»;
6. секцию «Цели и профессиональные интересы»;
7. секцию «Конфиденциальность и границы»;
8. секцию «Предпросмотр контекста».

Секции должны быть визуально оформлены как локальные карточки/аккордеоны. Первая секция раскрыта по умолчанию.

Не копировать component ID из `JobCandidateEdit`. Использовать собственный префикс `userAiProfile`.

## 8.4. Компоненты ввода

### Профессиональный профиль

```text
aboutMeField
currentPositionField
functionalRoleField
seniorityLevelField
professionalExperienceYearsField
recruitingExperienceYearsField
currentResponsibilitiesField
educationField
certificationsField
domainExpertiseField
industriesField
```

### Рекрутинговая специализация

```text
recruitingSpecializationsField
targetRolesField
candidateLevelsField
hiringGeographiesField
decisionPrioritiesField
clientAndProjectContextField
```

### Предпочтения ответа

```text
preferredLanguageField
responseDetailLevelField
communicationStyleField
terminologyLevelField
preferredAnswerStructureField
customAiInstructionsField
communicationConstraintsField
```

### Цели и интересы

```text
professionalGoalsField
professionalInterestsField
developmentAreasField
currentPrioritiesField
```

### Конфиденциальность

```text
profileEnabledField
externalProcessingAllowedField
consentText
consentAcceptedAtLabel
profileConfirmedAtLabel
previewAiContextBtn
clearAiProfileBtn
```

## 8.5. Валидация

1. Опыт — диапазон 0–70.
2. Длины полей — согласно разделу 5.
3. `profileEnabled = true` допускается только при `externalProcessingAllowed = true`.
4. При подтверждении согласия:
   - записать актуальную версию согласия;
   - записать `consentAcceptedAt`;
   - не менять дату повторно, если пользователь не отзывал согласие.
5. При изменении значимых полей:
   - сбросить `profileConfirmedAt`;
   - предложить пользователю подтвердить актуальность.
6. Пустой профиль можно сохранить.
7. Кнопка очистки требует отдельного подтверждения.

---

# 9. `ExtSettingsWindow.java`

## 9.1. Загрузка

Добавить константу JPQL:

```java
private static final String QUERY_GET_USER_AI_PROFILE =
        "select e from itpearls_UserAiProfile e where e.user = :currentUser";
```

Рекомендуемый метод:

```java
private void loadOrCreateUserAiProfile()
```

Алгоритм:

1. загрузить профиль текущего пользователя узким view;
2. если нет — создать через `Metadata`;
3. установить `user`;
4. задать безопасные defaults;
5. установить item в datasource;
6. не выполнять commit при открытии формы.

## 9.2. Сохранение

В `commit()`:

1. выполнить UI-валидацию;
2. сохранить `UserSettings`;
3. сохранить `ExtUser`;
4. сохранить `UserAiProfile`;
5. вызвать `super.commit()` только после успешных commit;
6. не допустить частичного сохранения при ошибке профиля.

Предпочтительно использовать единый `CommitContext`, если это безопасно для текущего экрана и не меняет контракт базового `SettingsWindow`.

## 9.3. Предпросмотр

Метод:

```java
public void previewAiContext()
```

Требования:

- вызывает `UserAiContextService.buildCurrentUserContextPreview()`;
- не обращается к внешнему API;
- показывает modal dialog или read-only `textArea`;
- явно подписывает источник:
  - данные пользователя;
  - системные значения;
  - вычисленные предпочтения;
- не показывает скрытые технические поля.

## 9.4. Очистка

Метод:

```java
public void clearAiProfile()
```

Требования:

- подтверждение через `Dialogs`;
- очищаются только поля `UserAiProfile`;
- связь `user` сохраняется;
- `profileEnabled = false`;
- `externalProcessingAllowed = false`;
- согласие и даты обнуляются;
- аватар и `ExtUser` не изменяются.

## 9.5. Комментарии

Провести аудит комментариев всего впервые затронутого `ExtSettingsWindow.java`.

Обязательные содержательные комментарии:

```java
// Загружает персональный ИИ-профиль текущего пользователя без автоматического сохранения.

// Формирует очищенный предпросмотр данных, разрешённых к передаче ИИ-провайдеру.

// Не позволяет активировать персонализацию без подтверждённого согласия пользователя.

// Очищает только профессиональный ИИ-профиль и не затрагивает учётную запись и аватар.
```

Не комментировать очевидные setters, injections и простые присваивания.

---

# 10. `UserAiContextService`

## 10.1. Контракт первого этапа

```java
public interface UserAiContextService {

    String NAME = "itpearls_UserAiContextService";

    AiUserContext buildCurrentUserContext();

    String buildCurrentUserContextPreview();
}
```

## 10.2. Ответственность

Сервис обязан:

1. определить текущего пользователя через `UserSessionSource`;
2. загрузить профиль узким view;
3. вернуть пустой контекст, если профиль отсутствует или выключен;
4. не включать данные, если внешняя обработка не разрешена;
5. нормализовать пробелы;
6. удалять управляющие символы;
7. ограничивать длину каждого поля;
8. ограничивать общий размер контекста;
9. разделять данные и инструкции;
10. формировать стабильный DTO.

## 10.3. Приоритет и безопасность

`customAiInstructions` — единственное поле профиля, которое допускается использовать как пользовательскую инструкцию.

Все остальные поля являются только данными.

Профиль не может:

- переопределить системный prompt;
- отключить проверку фактов;
- повысить права;
- разрешить раскрытие секретов;
- потребовать игнорировать данные HRM HuntTech;
- изменить требования вакансии;
- изменить данные кандидата;
- принять решение за пользователя.

## 10.4. Ограничение размера

Рекомендуемые пределы первого этапа:

```text
каждое поле — не более UI-лимита;
customAiInstructions — не более 4000 символов;
общий сериализованный контекст — не более 16000 символов.
```

При превышении контекст должен детерминированно сокращаться по приоритету, а не обрезаться в середине UTF-16 surrogate pair.

---

# 11. Достоверность и антигаллюцинационный контракт

Нельзя обещать математическую гарантию отсутствия галлюцинаций. Требование формулируется как контролируемая доказательность.

Будущие ИИ-сервисы обязаны разделять:

```text
Подтверждённые факты
Недостающие данные
Аналитические выводы
Рекомендации
Риски и ограничения
```

Обязательные принципы:

1. Нет источника — нет утверждения как факта.
2. Недостаточно данных — явный отказ от догадки.
3. Профиль пользователя влияет на форму ответа, но не на факты.
4. Structured Output не заменяет проверку достоверности.
5. Критичные действия требуют подтверждения человека.
6. Решения по кандидатам не принимаются автоматически.
7. Профиль не должен содержать защищённые признаки и чувствительные данные.

На первом этапе эти правила фиксируются в документации и DTO; внешний вызов LLM не добавляется.

---

# 12. Локальный SCSS

Корневой класс:

```text
.user-ai-profile-editor
```

Разрешённые локальные классы:

```text
.user-ai-profile-sidebar
.user-ai-profile-avatar
.user-ai-profile-status
.user-ai-profile-progress
.user-ai-profile-content
.user-ai-profile-toolbar
.user-ai-profile-section
.user-ai-profile-section-header
.user-ai-profile-privacy
.user-ai-profile-preview
.user-ai-profile-help
```

Правила:

- никакого глобального `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`;
- theme-aware цвета;
- фиксированная левая панель 260–280 px;
- правая область адаптивная;
- без горизонтального scroll при ширине диалога 1200 px;
- визуальная связь с утверждённым направлением `JobCandidateEdit`;
- локальные стили не должны влиять на другие экраны.

---

# 13. Тесты

## 13.1. Core unit tests

Минимум:

1. профиль отсутствует → пустой контекст;
2. профиль выключен → пустой контекст;
3. внешняя обработка запрещена → контекст не формируется;
4. включённый профиль → разрешённые поля входят;
5. API-ключ и почтовые пароли не входят;
6. системные роли не сериализуются как инструкции;
7. управляющие символы удаляются;
8. пробелы нормализуются;
9. лимит отдельного поля соблюдается;
10. общий лимит контекста соблюдается;
11. `customAiInstructions` отделены от данных;
12. удаление профиля не затрагивает `ExtUser`.

## 13.2. Web tests

Добавить тесты, соответствующие существующей инфраструктуре web-модуля:

1. экран открывается с существующим профилем;
2. экран открывается без профиля;
3. пустой профиль сохраняется;
4. включение без согласия блокируется;
5. очистка сбрасывает только `UserAiProfile`;
6. аватар продолжает сохраняться;
7. почтовые настройки продолжают сохраняться;
8. предпросмотр не вызывает внешний HTTP.

## 13.3. Integrity

Обязательно:

```bash
./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace
```

Ожидаемый результат:

```text
8/8 PASS
```

Также проверить:

```text
все getters ExtSettingsWindow ⊆ extUser-view / userSettings-view / userAiProfile-view
```

---

# 14. Документация

## 14.1. `docs/entities/UserAiProfile.md`

Обязательные разделы:

1. Business & Context Intro;
2. обзор сущности;
3. связи;
4. поля и enums;
5. views;
6. безопасность и конфиденциальность;
7. база данных;
8. история изменений.

## 14.2. `docs/ui/ExtSettingsWindow_Spec.md`

Обязательные разделы:

1. Business & Context Intro;
2. Invocation & Context;
3. Data & Entity Binding;
4. Form Hierarchy;
5. Business Behavior;
6. Actions & Methods;
7. Layout & Components;
8. история изменений.

## 14.3. `docs/services/UserAiContextService.md`

Описать:

- контракт;
- источники данных;
- выбор полей;
- sanitization;
- лимиты;
- разграничение данных и инструкций;
- запрет секретов;
- антигаллюцинационный контракт;
- тесты;
- история изменений.

Новая запись истории изменений — первой строкой:

```text
2026-07-22
```

---

# 15. Обязательные проверки Hermes

После реализации Hermes должен работать на точном итоговом HEAD ветки.

```bash
git diff --check

./gradlew :app-global:compileJava \
          :app-core:compileJava \
          :app-core:compileTestJava \
          :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests '*UserAiContextServiceBeanTest*' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

После deploy:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200.

Smoke-сценарии:

1. открыть «Настройки»;
2. открыть «Обо мне»;
3. сохранить пустой профиль;
4. заполнить профиль;
5. проверить согласие;
6. открыть предпросмотр;
7. убедиться, что секреты отсутствуют;
8. отключить профиль;
9. очистить профиль;
10. повторно открыть экран;
11. изменить аватар;
12. изменить почтовую настройку;
13. проверить runtime-логи.

Искать:

```text
IllegalStateException
Cannot get unfetched attribute
detached
NullPointerException
DevelopmentException
FileStorageException
OutOfMemoryError
```

---

# 16. Отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-22/setting-window-about-me-ai-profile/
```

Файлы:

```text
implementation-report.md
test-report.md
functional-smoke.md
runtime-errors.log
```

В отчёте указать:

- ветку;
- точный SHA;
- полный diff;
- Liquibase changeSet;
- результаты unit tests;
- `ScreenViewIntegrityTest` 8/8;
- `buildScssThemes`;
- `clean assemble`;
- HTTP 200;
- smoke-сценарии;
- ограничения;
- подтверждение отсутствия внешних LLM-вызовов.

---

# 17. Формат коммита реализации

```text
feat(ai-profile): добавить вкладку «Обо мне» и профиль персонализации ИИ

- добавлена сущность UserAiProfile и Liquibase-миграция
- реализована вкладка «Обо мне» в ExtSettingsWindow
- добавлен безопасный предпросмотр ИИ-контекста
- добавлены unit- и integrity-тесты
- обновлена документация сущности, экрана и сервиса

[branch: agent/setting-window-about-me-ai-profile]
[deploy-local]
[prompt: собери и разверни точный HEAD; запусти UserAiContextServiceBeanTest, ScreenViewIntegrityTest 8/8, buildScssThemes, clean assemble; проверь HTTP 200, сохранение профиля, согласие, предпросмотр без секретов, аватар и почтовые настройки; сохрани отчёт в docs/performance-archive/2026-07-22/setting-window-about-me-ai-profile/]
```

---

# 18. Критерии приёмки

```text
[ ] Создан UserAiProfile 1:1 с ExtUser
[ ] USER_ID уникален
[ ] Персонализация по умолчанию выключена
[ ] Внешняя обработка по умолчанию запрещена
[ ] Пустой профиль сохраняется
[ ] Согласие фиксируется версией и датой
[ ] API-ключи и пароли не входят в контекст
[ ] Предпросмотр работает без внешнего HTTP
[ ] Профиль не влияет на права и бизнес-правила
[ ] Профиль не используется для ранжирования кандидатов
[ ] Layout соответствует направлению JobCandidateEdit
[ ] Стили локальны
[ ] Existing component ID сохранены
[ ] Аватар работает
[ ] Почтовые настройки работают
[ ] Комментарии к нетривиальной логике добавлены
[ ] UserAiContextServiceBeanTest зелёный
[ ] ScreenViewIntegrityTest 8/8
[ ] buildScssThemes успешен
[ ] BUILD SUCCESSFUL
[ ] HTTP 200
[ ] Runtime-ошибки отсутствуют
[ ] Документация синхронизирована
[ ] История изменений обновлена
[ ] Отчёт Hermes сохранён
```

До отчёта Hermes статус:

```text
Этап реализован и передан Hermes на проверку.
```

После отчёта допускается только один из выводов:

```text
Этап принят.
Переход к следующему этапу: РАЗРЕШЁН.
```

или:

```text
Этап не принят.
Переход к следующему этапу: ЗАПРЕЩЁН.
```

---

# 19. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | Создано точное техническое задание на сущность `UserAiProfile`, вкладку «Обо мне», безопасный предпросмотр контекста, тесты и документацию |
