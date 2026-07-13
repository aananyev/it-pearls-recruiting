# 00_initial_audit.md — Аудит исходного CUBA-проекта HuntTech Recruiting

> Дата аудита: 2026-07-09
> Аудитор: goose (AI Agent)
> Проект: `hunttech_recruiting`

---

## 1. Версия CUBA Platform

| Параметр | Значение |
|---|---|
| **CUBA Version** | `7.3-SNAPSHOT` |
| System of Record | `build.gradle` — `ext.cubaVersion = '7.3-SNAPSHOT'` |
| Gradle Plugin | `com.haulmont.gradle:cuba-plugin:$cubaVersion` |
| CUBA BOM | `com.haulmont.cuba:cuba-global:$cubaVersion` |

**Статус:** CUBA 7.3-SNAPSHOT (pre-release/development branch, не стабильный релиз).

---

## 2. Версия Java

| Параметр | Значение |
|---|---|
| **JDK (компиляция)** | Corretto 11.0.17 (`gradle.properties`: `org.gradle.java.home=...corretto-11.0.17...`) |
| **JDK (запуск, текущий)** | OpenJDK 22.0.1 (системный `/usr/bin/java`) |
| **Доступные JDK** | corretto-1.8.0_352, corretto-1.8.0_402, corretto-1.8.0_432, corretto-11.0.17, jbr-17.0.9, openjdk-22.0.1 |
| **Source/Target level** | CUBA 7.3 по умолчанию Java 8, настройка в `build.gradle` не указана явно |
| **JAXB dependency** | добавлена для Java > 8: `javax.xml.bind:jaxb-api:2.3.1` |

**Статус:** Проект собран под Java 11, но может работать и на Java 8. Для Jmix потребуется Java 17+.

---

## 3. Система сборки

| Параметр | Значение |
|---|---|
| **Build System** | Gradle 5.6.4 |
| **Wrapper** | `gradlew` / `gradlew.bat` |
| **Gradle Daemon** | отключён в CI (`--no-daemon`) |
| **CUBA Plugin** | встроенный `cuba-plugin` |
| **Add-ons (appComponent)** | 10 компонентов (см. ниже) |

### Сторонние зависимости (ключевые)

```groovy
// Из build.gradle — глобальные зависимости
compile 'org.apache.pdfbox:pdfbox:3.0.0-alpha3'
compile 'org.apache.poi:poi-ooxml-lite:5.2.3'
compile 'net.htmlparser.jericho:jericho-html:3.4'
compile 'com.rubiconproject.oss:jchronic:0.2.8'
compile 'ru.yaal.project.hhapi:HhJavaApi:0.2.6'
compile 'jakarta.mail:jakarta.mail-api:2.1.2'
compile 'io.jsonwebtoken:jjwt-api:0.11.5'
compile 'org.telegram:telegrambots:6.8.0'
compile 'org.telegram:telegrambotsextensions:6.8.0'
compile 'net.coobird:thumbnailator:0.4.20'
compile 'org.projectlombok:lombok:1.18.16'
compile 'com.ibm.icu:icu4j:69.1'
```

**Важно:** Проект использует `jakarta.mail:jakarta.mail-api:2.1.2` (JakartaEE), хотя CUBA 7.3 использует Javax. Это может создавать конфликт.

---

## 4. DBMS

| Параметр | Значение |
|---|---|
| **DBMS** | PostgreSQL 11 |
| **JDBC Driver** | `org.postgresql:postgresql:42.2.9` |
| **Локальная БД (dev)** | `localhost:5432/hunttech` |
| **Удалённая БД (prod)** | `hr.hunttech.ru:5432/itpearls` (через SSH-туннель) |
| **Пользователь (dev)** | `cuba` / `cuba` |
| **Пользователь (prod)** | `cuba` |
| **Tablespace** | Не используется (стандартный `pg_default`) |
| **Количество таблиц (dev)** | 152 |
| **Количество таблиц (prod)** | 154 (на 2 связки больше) |
| **Миграции** | CUBA-скрипты: `modules/core/db/update/postgres/` |
| **Changelog** | `modules/core/db/changelog/db.changelog-master.xml` |
| **Ручные скрипты** | `scripts/db-migration/*.sql`, `scripts/migrate_*.sql` |
| **Docker Compose** | `docker-compose.yml` (PostgreSQL 11 для разработки) |

---

## 5. Настройки подключения к БД

**Файл:** `modules/core/src/app.properties`

```properties
cuba.dataSource.url=jdbc:postgresql://localhost:5432/hunttech  # (на момент аудита — локальная БД)
cuba.dataSource.username=cuba
cuba.dataSource.password=cuba
```

**web-app.properties:** `com/company/hunttech/web-app.properties`
- Middleware connection: `cuba.connectionUrlList=http://localhost:8080/app-core`
- File storage: `cuba.fileStorageDir=${app.home}/fileStorage`

**Остальные конфигурационные файлы:**
- `etc/local.app.properties.example` — шаблон для локального оверрайда
- `.env`, `.env.example`, `.env.local` — окружение
- `hunttech.conf` — параметры для deploy-скриптов
- `deploy_shared.conf` — общая конфигурация деплоя

---

## 6. Модули CUBA

| Модуль | Путь | Тип | Файлов Java |
|---|---|---|---|
| `app-global` | `modules/global` | Общий (сущности, сервисы, DTO) | 102 |
| `app-core` | `modules/core` | Middleware (бизнес-логика) | 53 |
| `app-web` | `modules/web` | Web UI (экраны, виджеты) | 226 |
| `app-gui` | `modules/gui` | Desktop GUI (не используется активно) | 3 |
| `app-web-toolkit` | `modules/web-toolkit` | Vaadin Widgetset | 0 (только .gwt.xml) |

**Важно:** Модуль `web` самый большой (226 Java-файлов + 187 XML-экранов) — это основная зона миграции.

---

## 7. Base package

| Параметр | Значение |
|---|---|
| **Base package (основной)** | `com.company.hunttech` |
| **Дополнительный** | `com.hunttech.hrm.web` (кастомные UI-компоненты) |
| **CUBA custom persistence** | `com.haulmont.cuba.core.sys.persistence` (кастомные конвертеры) |
| **Group ID (Gradle)** | `com.company.hunttech` |
| **Version** | `0.1` (snapshot) |

---

## 8. Сущности

**Всего сущностей (entity classes):** 66 в `modules/global/src/com/company/hunttech/entity/`

### Бизнес-сущности

| # | Имя | CUBA Entity | Таблица | Поля | @NamePattern | Связи |
|---|---|---|---|---|---|---|
| 1 | ApplicationRecruitment | `hunttech_ApplicationRecruitment` | hunttech_application_recruitment | 18 | - | staffing_table, recruitment_list |
| 2 | ApplicationRecruitmentList | `hunttech_ApplicationRecruitmentList` | hunttech_application_recruitment_list | 20 | + | project, department, company |
| 3 | ApplicationSetup | `hunttech_ApplicationSetup` | hunttech_application_setup | 18 | + | - |
| 4 | CandidateCV | `hunttech_CandidateCV` | hunttech_candidate_cv | 39 | + | job_candidate |
| 5 | City | `hunttech_City` | hunttech_city | 8 | + | region, country |
| 6 | Company | `hunttech_Company` | hunttech_company | 30 | + | ownership, group, director, city, region, country |
| 7 | CompanyDepartament | `hunttech_CompanyDepartament` | hunttech_company_departament | 16 | + | company |
| 8 | CompanyGroup | `hunttech_CompanyGroup` | hunttech_company_group | 4 | + | - |
| 9 | Country | `hunttech_Country` | hunttech_country | 8 | + | - |
| 10 | Currency | `hunttech_Currency` | hunttech_currency | 4 | + | - |
| 11 | Employee | `hunttech_Employee` | hunttech_employee | 22 | + | work_status, currency, job_candidate |
| 12 | EmployeeWorkStatus | `hunttech_EmployeeWorkStatus` | hunttech_employee_work_status | 4 | + | - |
| 13 | ExtUser | `hunttech_ExtUser` | hunttech_ext_user | 41 | - | extends `sec$User` |
| 14 | FileType | `hunttech_FileType` | hunttech_file_type | 4 | + | - |
| 15 | Grade | `hunttech_Grade` | hunttech_grade | 2 | + | - |
| 16 | InternalEmailTemplate | `hunttech_InternalEmailTemplate` | hunttech_internal_email_template | 16 | + | position, open_position |
| 17 | InternalEmailer | `hunttech_InternalEmailer` | hunttech_internal_emailer | 22 | + | - |
| 18 | Interview | `hunttech_Interview` | hunttech_interview | 22 | - | open_position, candidate |
| 19 | ItearctionRequirements | `hunttech_ItearctionRequirements` | hunttech_itearction_requirements | 8 | - | iteraction |
| 20 | Iteraction | `hunttech_Iteraction` | hunttech_iteraction | 98 | + | - |
| 21 | IteractionList | `hunttech_IteractionList` | hunttech_iteraction_list | 34 | - | iteraction_type, candidate |
| 22 | JobCandidate | `hunttech_JobCandidate` | hunttech_job_candidate | 58 | + | position, city, skill_tree, specialisation |
| 23 | JobCandidatePositionLists | `hunttech_JobCandidatePositionLists` | hunttech_job_candidate_position_lists | 4 | - | job_candidate, position_list |
| 24 | JobHistory | `hunttech_JobHistory` | hunttech_job_history | 8 | + | job_candidate |
| 25 | LaborAgeementType | `hunttech_LaborAgeementType` | hunttech_labor_ageement_type | 6 | + | - |
| 26 | LaborAgreement | `hunttech_LaborAgreement` | hunttech_labor_agreement | 44 | + | employee, company, type |
| 27 | OpenPosition | `hunttech_OpenPosition` | hunttech_open_position | 116 | + | grade, city, position_type, project |
| 28 | OpenPositionComment | `hunttech_OpenPositionComment` | hunttech_open_position_comment | 10 | + | open_position |
| 29 | OpenPositionNews | `hunttech_OpenPositionNews` | hunttech_open_position_news | 14 | + | open_position |
| 30 | OutstaffingRates | `hunttech_OutstaffingRates` | hunttech_outstaffing_rates | 12 | + | grade |
| 31 | Person | `hunttech_Person` | hunttech_person | 34 | + | city, position |
| 32 | PersonelReserve | `hunttech_PersonelReserve` | hunttech_personel_reserve | 22 | - | person, open_position |
| 33 | Position | `hunttech_Position` | hunttech_position | 10 | + | - |
| 34 | Project | `hunttech_Project` | hunttech_project | 28 | + | department, owner |
| 35 | RecruitingRecrutiers | `hunttech_RecruitingRecrutiers` | hunttech_recruiting_recrutiers | 10 | - | user, open_position |
| 36 | RecrutiesTasks | `hunttech_RecrutiesTasks` | hunttech_recruties_tasks | 16 | + | recruiter, open_position |
| 37 | Region | `hunttech_Region` | hunttech_region | 8 | + | country |
| 38 | Setup | `hunttech_Setup` | hunttech_setup | 8 | - | - |
| 39 | SignIcons | `hunttech_SignIcons` | hunttech_sign_icons | 12 | - | user |
| 40 | SkillTree | `hunttech_SkillTree` | hunttech_skill_tree | 24 | + | parent_skill, open_position, candidate_cv |
| 41 | SkillsFilterLastSelection | `hunttech_SkillsFilterLastSelection` | hunttech_skills_filter_last_selection | 12 | - | user |
| 42 | SocialNetworkType | `hunttech_SocialNetworkType` | hunttech_social_network_type | 8 | + | - |
| 43 | SocialNetworkURLs | `hunttech_SocialNetworkURLs` | hunttech_social_network_ur_ls | 8 | + | person, type |
| 44 | SomeFiles | `hunttech_SomeFiles` | hunttech_some_files | 12 | + | file_descriptor, owner |
| 45 | SomeFilesAgreement | `hunttech_SomeFilesAgreement` | hunttech_some_files_agreement | 2 | + | extends SomeFiles |
| 46 | SomeFilesCandidateCV | `hunttech_SomeFilesCandidateCV` | hunttech_some_files_candidate_cv | 2 | + | extends SomeFiles |
| 47 | SomeFilesOpenPosition | `hunttech_SomeFilesOpenPosition` | hunttech_some_files_open_position | 2 | + | extends SomeFiles |
| 48 | Specialisation | `hunttech_Specialisation` | hunttech_specialisation | 4 | + | - |
| 49 | StaffCurrent | `hunttech_StaffCurrent` | hunttech_staff_current | 8 | - | employee, position, department |
| 50 | StaffingTable | `hunttech_StaffingTable` | hunttech_staffing_table | 16 | + | grade |
| 51 | SubscribeCandidateAction | `hunttech_SubscribeCandidateAction` | hunttech_subscribe_candidate_action | 8 | + | open_position, candidate |
| 52 | UserAiConfiguration | `hunttech_UserAiConfiguration` | hunttech_user_ai_configuration | 10 | + | user |
| 53 | UserSettings | `hunttech_UserSettings` | hunttech_user_settings | 28 | - | user |
| 54 | VacancyPromptTemplate | `hunttech_VacancyPromptTemplate` | hunttech_vacancy_prompt_template | 10 | + | - |
| 55 | MyActiveCandidateExclude | `hunttech_MyActiveCandidateExclude` | hunttech_my_active_candidate_exclude | 6 | - | user |
| 56 | Ownershup | `hunttech_Ownershup` | hunttech_ownershup | 4 | + | - |
| 57 | PossibleNames | `hunttech_PossibleNames` | hunttech_possible_names | 6 | - | - |
| 58 | JobCandidateSignIcon | `hunttech_JobCandidateSignIcon` | hunttech_job_candidate_sign_icon | 6 | - | job_candidate, sign_icon |

### Enum-сущности (встроенные enum-поля в сущностях)

| Enum | Значения | Используется в |
|---|---|---|
| `FormEmployment` | FULL_TIME, PART_TIME, REMOTE, PROJECT | OpenPosition |
| `GradeCode` | INTERN, JUNIOR, MIDDLE, SENIOR, LEAD | Grade |
| `GradeIntCode` | 1-5 | Grade |
| `HuntPriority` | HIGH, NORMAL, LOW | Iteraction, OpenPosition |
| `OpenPositionPriority` | CRITICAL, HIGH, NORMAL, LOW | OpenPosition |
| `RemoteWork` | NO, POSSIBLE, YES | OpenPosition |
| `StaffInteractionStatus` | OPEN, IN_PROGRESS, CLOSED | Iteraction, Employee |
| `StdPictures` | — | SignIcons |
| `StdSelections` | — | IteractionList |
| `StdSelectionsColor` | — | IteractionList |
| `StdUserGroup` | — | User |
| `EmailKeys` | — | InternalEmailer |

### Наследование

- `ExtUser extends com.haulmont.cuba.security.entity.User` (с `@Extends(User.class)`)
- `SomeFilesAgreement extends SomeFiles`
- `SomeFilesCandidateCV extends SomeFiles`
- `SomeFilesOpenPosition extends SomeFiles`
- `InternalEmailerTemplate extends InternalEmailer` (в коде, не везде)

**Важно:** `@MappedSuperclass` не используется. Все наследования — через `@Extends` (CUBA-специфичный) и JOINED/TABLE_PER_CLASS.

---

## 9. Views / Fetch plans

**Файл:** `modules/global/src/com/company/hunttech/views.xml` — 4 файла (с разбивкой по сущностям)

Всего views: **~200+** определений во всех view-файлах (подтверждено анализом XML).

Типичные view-имена:
- `*-browse-view` — для списков
- `*-edit-view` — для редактирования
- `*-picker-view` — для lookup-полей
- `*-child-view` / `*-parent-view`

Все view используют `extends="_minimal"` или `extends="_local"` как базовые.

**Миграция:** Views.xml → Fetch plans Jmix (сохраняются в XML с новым namespace).

---

## 10. Services, beans, listeners, scheduled tasks

### Services (global module — интерфейсы)

| Service Interface | Реализация (core) |
|---|---|
| `ApplicationSetupService` | `ApplicationSetupServiceBean` |
| `EmailGenerationService` | `EmailGenerationServiceBean` |
| `InteractionService` | `InteractionServiceBean` |
| `OpenPositionCommentService` | `OpenPositionCommentServiceBean` |
| `OpenPositionService` | `OpenPositionServiceBean` |
| `ParseCVService` | `ParseCVServiceBean` |
| `PdfParserService` | `PdfParserServiceBean` |
| `ProjectService` | `ProjectServiceBean` |
| `RecruterStatService` | `RecruterStatServiceBean` |
| `ResumeRecognitionService` | `ResumeRecognitionServiceBean` |
| `SendNotificationsService` | `SendNotificationsServiceBean` |
| `SignIconService` | `SignIconServiceBean` |
| `StarsAndOtherService` | `StarsAndOtherServiceBean` |
| `StrSimpleService` | `StrSimpleServiceBean` |
| `TelegramBotService` | `TelegramBotServiceBean` |
| `TelegramService` | `TelegramServiceBean` |
| `TextManipulationService` | `TextManipulationServiceBean` |
| `WebLoadService` | `WebLoadServiceBean` |

### Прочие service-интерфейсы (в global/service)

| Service | Описание |
|---|---|
| `GetRoleService` | Получение роли |
| `GetUserRoleService` | Получение роли пользователя |
| `HrmAiService` | AI-интеграция |
| `SendEmailService` | Отправка почты |
| `SubscribeDateService` | Подписка по дате |

### Дополнительные beans (core, не через service interface)

- `ApprovalHelper` — утилита для согласований
- `ExchangeBean` — обмен данными
- `OpenPositionApprovalBean` — согласование вакансий
- `ImageProcessingServiceBean` — обработка изображений
- `TelegramBotComponent` — Telegram бот компонент

### AI-интеграция (core/ai/)

- `AIProvider` — интерфейс AI-провайдера
- `AIProviderRegistry` — реестр провайдеров
- `MiMoProvider` — провайдер MiMo AI
- `OpenAiProvider` — провайдер OpenAI

### Entity Listeners

| Listener | Событие |
|---|---|
| `ExtUserChangedListener` | before/after изменения `hunttech_ExtUser` |
| `OpenPositionCommentChangedListener` | before/after изменения `hunttech_OpenPositionComment` |

### JMX Beans

- `OpenPositionApproval` — JMX для согласования вакансий

### Events

- `BeanNotificationEvent` — внутреннее уведомление (Spring Event)
- `UiNotificationEvent` — UI-уведомление

### Scheduled Tasks

- Не обнаружено в явном виде через `@Scheduled` аннотации или `scheduled` XML.
- Настройки `cuba.schedulingActive=true` в `app.properties` — могут быть задачи через CUBA Scheduled Tasks UI.

---

## 11. Экраны CUBA Generic UI

**Всего экранов (Java + XML):** ~187 XML-дескрипторов, ~170 Java-контроллеров

### Типы экранов

| Тип | Количество | Пример |
|---|---|---|
| Browse screens (таблицы) | ~60+ | `JobCandidateBrowse`, `OpenPositionBrowse` |
| Edit screens (редактирование) | ~60+ | `JobCandidateEdit`, `OpenPositionEdit` |
| Lookup screens | ~5 | `CandidateCVChoiseBrowse`, `SelectPersonPositions` |
| Custom screens | ~20+ | `Interviewcalendar`, `WeeklyInterviewCalendar` |
| Fragments | ~10 | `Skillsbar`, `TetrisCandidates`, `JobCandidateCommentFragment`, `OnlyTextFromFile` |
| Widgets (dashboard) | ~20+ | `MyActiveCandidatesDashboard`, `FunnelHuntingWidget`, `ResearcherDiagramWidget` |
| Main screen | 1 | `ExtMainScreen` |
| Login screen | 1 | `AppLoginScreen` |
| Dialogs / Helpers | ~7 | `ProgressBarScreen`, `LoadFromFileScreen`, `ViewerTextScreen` |

### Ключевые особенности UI

1. **Кастомные компоненты Vaadin:**
   - `WebFallbackImage` — fallback при загрузке изображения
   - `WebOvalImage` — круглое изображение
   - `WebOvaFallbackImage` — круглое с fallback
   - `InterviewCalendarWidget` — календарь собеседований

2. **Client-side виджеты:**
   - `AppWidgetSet.gwt.xml` — кастомный GWT widgetset
   - (client-side классы не найдены; возможно, widgetset пустой или используется дефолтный)

3. **UI-темы:** `hover` (основная), `halo`, `havana`, `helium` (доступны, не активны)

4. **Dashboard виджеты:** ~16 кастомных виджетов для CUBA Dashboard add-on

5. **Custom SCSS:** присутствует в `modules/web/themes/hover/`

**Миграция:** CUBA Generic UI (Vaadin 8) → Jmix Flow UI (Vaadin 24/7). Полная переработка UI — **самый трудозатратный этап**.

---

## 12. Security

| Компонент | Статус |
|---|---|
| **Роли (roles)** | Через CUBA Security UI (в БД `sec_role`, `sec_user_role`) |
| **Access Groups** | CUBA groups (в БД `sec_group`, `sec_group_hierarchy`) |
| **Constraints** | CUBA constraints (в БД `sec_constraint`) |
| **Permissions** | В БД `sec_permission`, `sec_localized_constraint_msg` |
| **Session Attributes** | В БД `sec_session_attr` |
| **User substitution** | В БД `sec_user_substitution` |
| **web-permissions.xml** | Пустой (`<permission-config/>`) |
| **Anonymous session** | Настроен `cuba.anonymousSessionId` |
| **User entity** | `ExtUser extends User` (кастомизированный) |
| **Login screen** | `AppLoginScreen` (кастомный, `loginBranded`) |
| **OAuth / LDAP** | Не обнаружено |

**Все security-данные хранятся в старой CUBA-БД и требуют миграции в новую модель Jmix.**

---

## 13. Add-ons

| Add-on | Версия | Модуль | Комментарий |
|---|---|---|---|
| CUBA Platform | 7.3-SNAPSHOT | — | Базовый |
| Global Events | 0.6.1 | cubaglevt | Простые события |
| Email Templates | 1.4.2 | yet | Шаблоны писем |
| Data Import | 0.14.1 | dataimport | Импорт данных |
| Dashboard | 3.2.3 | dashboard | Дашборды и виджеты |
| Helium Theme | 0.4.0 | helium | Тема (опционально) |
| Full-Text Search | 7.3-SNAPSHOT | fts | Поиск |
| Charts | 7.3-SNAPSHOT | charts | Графики |
| Reports | 7.3-SNAPSHOT | reports | Отчёты |
| BPM | 7.3-SNAPSHOT | bpm | Бизнес-процессы |

**Примечание:** Наличие BPM (Activiti) — отдельный блокер для миграции, т.к. Jmix BPM существенно отличается.

---

## 14. Интеграции

| Интеграция | Тип | Технология | Статус |
|---|---|---|---|
| **Telegram Bot** | Внешнее API | telegrambots 6.8.0 | Активно |
| **Email** | SMTP | Jakarta Mail 2.1.2 / CUBA email | Активно |
| **HeadHunter API** | REST | HhJavaApi 0.2.6 | Активно |
| **AI/GigaChat/OpenAI** | REST | HTTP-клиенты | Активно |
| **Image Processing** | Локальная | Thumbnailator, PDFBox | Активно |
| **File Storage** | Файловая система | CUBA FileStorage (`fileStorage`) | Активно |
| **REST API (CUBA)** | REST | CUBA REST API | Встроено |
| **PDF Generation** | Локальная | PDFBox 3.0, Apache POI | Активно |

---

## 15. SQL-скрипты CUBA

**Базовая схема:** CUBA-скрипты в `modules/core/db/`:

```
db/
├── changelog/
│   ├── db.changelog-master.xml
│   └── 260627-1-addAiEntities.xml
├── init/
│   └── postgres/  ... (начальные скрипты)
└── update/
    └── postgres/
        ├── 24/  (старые миграции 2024)
        │   ├── 240325-2-updateApplicationSetup_DropScript.sql
        │   ├── 240325-2-updateCandidateCv.sql
        │   ├── 240325-2-updateIteraction.sql
        │   ├── 240325-2-updateOpenPosition.sql
        │   ├── 240325-2-updatePosition.sql
        │   └── 240325-2-updateSecUser.sql
        └── 26/  (миграции 2026)
            ├── 260627-1-updateOpenPosition.sql
            ├── 260627-2-createUserAiConfiguration.sql
            ├── 260627-3-createVacancyPromptTemplate.sql
            ├── 260627-4-updateUserAiConfiguration.sql
            ├── 260629-2-updateSecUser.sql
            ├── 260701-0-dropOpenPositionPartnersPartnersLink.sql
            ├── 260701-1-updatePerson.sql
            ├── 260701-2-updateCandidateCv.sql
            ├── 260701-2-updateCompany.sql
            ├── 260701-2-updateIteractionList.sql
            ├── 260701-2-updateJobCandidate.sql
            ├── 260701-2-updateOpenPosition.sql
            ├── 260701-2-updatePerson.sql
            ├── 260701-2-updateProject.sql
            ├── 260701-2-updateVacancyPromptTemplate01.sql
            ├── 260704-2-updateProjectPerformanceIndexes.sql
            ├── 260704-3-updateJobCandidatePerformanceIndexes.sql
            ├── 260704-4-updateCandidateCvPerformanceIndexes.sql
            └── 260704-5-updateIteractionPerformanceIndexes.sql
```

**Ручные скрипты в корне проекта:**
- `scripts/db-migration/260630-schema-sync-pending-idempotent.sql`
- `scripts/db-migration/migrate_itpearls_to_hunttech_fdw.sql`
- `scripts/cleanup-test-entities.sql`
- `scripts/fix-anonymous-user.sql`

**Статус:** CUBA использует собственный механизм миграции (`createDb`/`updateDb`). В Jmix всё будет через Liquibase.

---

## 16. Потенциальные блокеры миграции

| # | Блокер | Степень | Описание |
|---|---|---|---|
| 1 | **CUBA 7.3-SNAPSHOT** | 🔴 Высокая | Нестабильная/пре-релизная версия. Не все CUBA 7.2→Jmix сценарии покрыты |
| 2 | **UI: CUBA Generic UI (Vaadin 8) → Jmix Flow UI** | 🔴 Высокая | 226 Java-файлов + 187 XML-экранов. Фактически полная переработка UI |
| 3 | **Кастомный Vaadin Widgetset** | 🟠 Средняя | `AppWidgetSet.gwt.xml` требует анализа совместимости с Jmix |
| 4 | **Кастомные Vaadin-компоненты** | 🟠 Средняя | 5 кастомных компонентов (`WebFallbackImage`, `WebOvalImage` и др.) |
| 5 | **BPM (Activiti/CUBA BPM)** | 🟠 Средняя | Требует отдельной миграции в Jmix BPM |
| 6 | **FTS (Full-Text Search)** | 🟠 Средняя | CUBA FTS → Jmix FTS (Elasticsearch) |
| 7 | **Dashboard + кастомные виджеты** | 🟠 Средняя | CUBA Dashboard → Jmix Dashboard (переработка виджетов) |
| 8 | **Charts** | 🟠 Средняя | CUBA Charts → Jmix Charts |
| 9 | **Reports** | 🟠 Средняя | CUBA Reports → Jmix Reports |
| 10 | **Data Import** | 🟡 Низкая | Add-on dataimport не имеет прямого аналога в Jmix |
| 11 | **Email Templates** | 🟡 Низкая | CUBA Email Templates → Jmix Email Templates |
| 12 | **Global Events** | 🟡 Низкая | Заменяется Spring Events |
| 13 | **Telegram Bot (telegrambots 6.8.0)** | 🟡 Низкая | Совместимость с Spring Boot (проверить conflict slf4j) |
| 14 | **Jakarta Mail vs javax.mail конфликт** | 🟡 Низкая | Проект использует jakarta.mail 2.1.2 |
| 15 | **ExtUser extends User** | 🟡 Низкая | Jmix имеет другую модель пользователя |
| 16 | **Custom security screens** | 🟡 Низкая | Кастомные экраны пользователей/ролей |
| 17 | **@Extends(User.class)** | 🟡 Низкая | CUBA-specific, требует замены на Jmix-аналог |
| 18 | **CUBA-specific persistence (CustomDbTypeConverter)** | 🟡 Низкая | Требует адаптации под Jmix |
| 19 | **Soft-delete в CUBA** | 🟡 Низкая | Моделируется через Jmix мягкое удаление |
| 20 | **File Storage (cuba.fileStorageDir)** | 🟡 Низкая | Конфигурация Jmix file storage |

---

## 17. Рекомендация по целевой версии Jmix

### Доступные версии Jmix

| Версия | Дата релиза | Стабильность | Java | Spring Boot | Vaadin |
|---|---|---|---|---|---|
| **Jmix 3.0.0** | 2026-06-30 | ✅ Стабильный | Java 17+ | Spring Boot 3.x (вероятно 3.4+) | Vaadin 24 (Flow UI) |
| Jmix 2.8.2 | 2026-06-09 | ✅ LTS | Java 17+ | Spring Boot 3.x | Vaadin 24 (Flow UI) |
| Jmix 1.7.3 | 2026-06-01 | ❌ Заморожен | Java 11+ | Spring Boot 2.7 | Vaadin 14 |

### Анализ сценариев

#### Сценарий A: Прямая миграция CUBA 7.3 → Jmix 3.0.0

**Плюсы:**
- Последняя версия со всеми новыми возможностями
- Долгосрочная поддержка (3.x будет LTS)
- Современный Spring Boot 3.x
- Не нужно дважды делать миграцию

**Минусы:**
- Может отсутствовать прямой migration path CUBA 7.3-SNAPSHOT → Jmix 3.x
- Официальный инструмент миграции ориентирован на CUBA 7.2 → Jmix 2.x
- Меньше готовых рецептов для сложных сценариев

#### Сценарий Б: Двухэтапная миграция: CUBA 7.3 → Jmix 2.8 LTS → Jmix 3.0

**Плюсы:**
- Migration path хорошо документирован: CUBA 7.2 → Jmix 2.x (официальный AI template)
- После Jmix 2.8 upgrade до 3.0 — стандартный процесс
- Меньше рисков на первом этапе
- Возможность протестировать на Jmix 2.8 перед финальным переходом

**Минусы:**
- Двойная работа (миграция UI, два upgrade)
- Дополнительное время

### Вердикт

**Рекомендуется: Двухэтапный путь (Сценарий Б)**

1. **Этап 1:** Миграция CUBA 7.3 → **Jmix 2.8 LTS** (используя официальный migration template)
2. **Этап 2:** Upgrade Jmix 2.8 → **Jmix 3.0** (стандартная процедура upgrade)

**Обоснование:**
- Официальный AI-шаблон и документация миграции ориентированы на Jmix 2.x
- Меньше неизвестных: CUBA 7.3-SNAPSHOT может содержать недокументированные отличия
- Jmix 2.8 — LTS, upgrade до 3.0 — рутинная операция
- Более безопасно для production

---

## Итоговая статистика проекта

| Метрика | Значение |
|---|---|
| Версия CUBA | 7.3-SNAPSHOT |
| Java | 11 (возможен апгрейд до 17) |
| DBMS | PostgreSQL 11 |
| Модулей | 5 (global, core, web, gui, web-toolkit) |
| Java-файлов (всего) | ~384 |
| Entity-классов | 58 бизнес-сущностей + CUBA system entities |
| Enums | 12 |
| XML-экранов | ~187 |
| XML-views | 4 файла (~200 view-определений) |
| Сервисов (global) | 18 |
| Сервисов (core beans) | 8 |
| Entity Listeners | 2 |
| JMX Beans | 1 |
| Кастомных UI компонентов | 5 |
| Dashboard виджетов | ~16 |
| Add-ons | 10 |
| Блокеров высокой степени | 2 (версия CUBA, UI объём) |
| Блокеров средней степени | 5 (Widgetset, BPM, FTS, Dashboard, Charts) |
| **Рекомендуемая целевая версия** | **Jmix 2.8 LTS → Jmix 3.0** |
