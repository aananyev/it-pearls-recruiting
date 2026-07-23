# Реализация вкладки «Обо мне» в контуре hunttech

> Проект: **HRM HuntTech**  
> Базовое ТЗ: [SettingWindow_AboutMe_UserAiProfile_Technical_Specification.md](SettingWindow_AboutMe_UserAiProfile_Technical_Specification.md)  
> Ветка реализации: `agent/settings-window-hunttech-refactor`  
> Базовый `master`: `4b787cd7921c97f9aee9521cb5c91aecb4be31c5`  
> Среда проверки и развертывания: строго локальная

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Документ фиксирует фактическую реализацию утверждённого дизайна `ExtSettingsWindow` после переноса проекта на локальную БД `hunttech`. Вкладка «Обо мне» хранит профессиональный профиль пользователя и параметры персонализации ответов, не изменяя данные кандидатов, вакансий, проектов и права доступа.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

```text
Главное окно HRM HuntTech
→ меню пользователя
→ Настройки
→ вкладка «Обо мне»
```

Экран сохраняет существующие вкладки «Интерфейс» и «Почта», существующие component ID и legacy-контракты `ExtUser`/`UserSettings`. Новый контур ИИ-профиля использует namespace `hunttech`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие → профиль найден → данные отображаются;
- профиль отсутствует → создаётся несохранённый экземпляр;
- включение без согласия → сохранение блокируется;
- предпросмотр → контекст формируется локально без внешнего HTTP;
- сохранение → `ExtUser`, `UserSettings` и `UserAiProfile` передаются в единый `CommitContext`;
- очистка → сбрасывается только ИИ-профиль.

## 1. Различие между базовым ТЗ и реализацией

Базовое ТЗ было подготовлено до окончательного перехода нового функционала в контур `hunttech` и содержит исторические примеры `itpearls_*`. Для реализованного ИИ-профиля источником истины являются значения:

| Объект | Фактическое значение |
|---|---|
| Java entity | `com.company.hunttech.entity.UserAiProfile` |
| CUBA entity name | `hunttech_UserAiProfile` |
| Таблица | `HUNTTECH_USER_AI_PROFILE` |
| Service contract | `com.company.hunttech.service.UserAiContextService` |
| Spring bean | `hunttech_UserAiContextService` |
| Controller | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| View resource | `com/company/hunttech/user-ai-profile-views.xml` |
| Локальная БД | `hunttech` |

Legacy-сущности проекта, которые не входили в рефакторинг (`ExtUser`, `UserSettings`), сохраняют существующие package/entity/table identifiers. Их массовое переименование в рамках задачи запрещено.

## 2. Реализованный дизайн

Вкладка `msgMyInfo` содержит:

- фиксированную левую панель 270 px;
- аватар, имя, должность, статус и заполненность профиля;
- правую вертикально прокручиваемую область;
- секции профессионального профиля, рекрутинга, предпочтений ответа, целей, конфиденциальности и предпросмотра;
- локальные SCSS-классы с корнем `.user-ai-profile-editor`.

Существующие ID `settingsTabSheet`, `msgMyInfo`, `msgInterface`, `mailAccessTab`, `okBtn`, `cancelBtn`, `userAvatarUpload`, `userPic`, `defaultPic` сохранены.

## 3. Новые свойства профиля

Реализованы:

- должность, функциональная роль и seniority;
- профессиональный и рекрутинговый опыт;
- обязанности, образование, сертификаты и доменная экспертиза;
- специализации рекрутинга, целевые роли, уровни и география;
- цели, интересы, зоны развития и приоритеты;
- язык, детализация, стиль, терминология и структура ответа;
- индивидуальные инструкции и ограничения коммуникации;
- состояние персонализации, согласие, версия и даты подтверждения.

## 4. Безопасность

- персонализация по умолчанию выключена;
- внешняя обработка по умолчанию запрещена;
- `customAiInstructions` отделено от структурированных данных;
- API-ключи, почтовые пароли и параметры подключения не загружаются сервисом;
- предпросмотр не вызывает внешний LLM;
- профиль не используется для ранжирования кандидатов или изменения бизнес-правил.

## 5. База данных

CUBA update script и Liquibase mirror создают `HUNTTECH_USER_AI_PROFILE` с:

- PK `PK_HUNTTECH_USER_AI_PROFILE`;
- FK `FK_HUNTTECH_USER_AI_PROFILE_ON_USER` на `SEC_USER.ID`;
- уникальным индексом `IDX_HUNTTECH_USER_AI_PROFILE_UNQ_USER`;
- проверками опыта 0–70.

Локальные Gradle-задачи `createDb` и `updateDb` направлены на БД `hunttech`. Любая фактическая миграция допускается только после проверенного backup локальной БД. Production запрещён.

## 6. Проверки перед приёмкой

Необходимо локально подтвердить:

- компиляцию global/core/web;
- `UserAiContextServiceBeanTest`;
- доступный integrity-тест экрана;
- `buildScssThemes`;
- `clean assemble`;
- локальный `updateDb` базы `hunttech`;
- запуск core/web и отсутствие `RemoteAccessException`;
- functional smoke нового дизайна;
- отсутствие потери данных.

До выполнения этих команд результат сборки, миграции и запуска не считается подтверждённым.

## 7. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | Зафиксирована реализация нового дизайна `ExtSettingsWindow` и перенос контура `UserAiProfile` в namespace и БД `hunttech` |
