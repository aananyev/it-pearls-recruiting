# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> XML: `ext-settings-window.xml`.  
> Связанные документы: [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md).

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран объединяет персональные настройки пользователя. Вкладка «Обо мне» формирует профессиональный ИИ-профиль, чтобы сервисы HRM HuntTech адаптировали ответы к роли и опыту пользователя без изменения объективных данных рекрутмента.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из стандартного меню настроек CUBA. Содержит вкладки `msgMyInfo`, `msgInterface` и `mailAccessTab`. «Обо мне» работает с `ExtUser` и `hunttech_UserAiProfile`; интерфейсные параметры сохраняет базовый `SettingsWindow`; почтовые параметры — существующая legacy-сущность `UserSettings`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие → загружаются `ExtUser`, `UserSettings`, `UserAiProfile`;
- загрузка аватара → изображение нормализуется существующим `ImageProcessingService`;
- профиль отсутствует → создаётся несохранённый объект;
- предпросмотр → локально формируется очищенный контекст;
- включение без согласия → сохранение блокируется;
- сохранение → три сущности фиксируются единым `CommitContext`;
- очистка → сбрасывается только `UserAiProfile`.

## 1. Invocation & Context

| Параметр | Значение |
|---|---|
| Контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| Базовый класс | `SettingsWindow` |
| XML schema | legacy `window.xsd` |
| Data API | legacy `dsContext` |
| Messages pack | `com.company.itpearls.web.screens.extsettingswindow` — сохранён как legacy-ресурс |

Существующие component ID сохранены для совместимости: `settingsTabSheet`, `msgMyInfo`, `msgInterface`, `mailAccessTab`, `okBtn`, `cancelBtn`, `userAvatarUpload`, `userPic`, `defaultPic`.

## 2. Data & Entity Binding

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | аватар и данные пользователя |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | существующий legacy-контракт почтовых настроек |
| `userAiProfileDs` | `hunttech_UserAiProfile` | `userAiProfile-view` | профессиональный профиль и узкая связь с владельцем |

Профиль загружается запросом:

```jpql
select e from hunttech_UserAiProfile e where e.user = :currentUser
```

Отсутствующий профиль создаётся только в памяти и сохраняется после нажатия `okBtn`.

## 3. Form Hierarchy

```text
settingsTabSheet
├── msgMyInfo
│   └── userAiProfileMainBox
│       ├── userAiProfileSidebar
│       │   ├── dropZone / picVBox / userPic / defaultPic
│       │   └── summary labels
│       └── userAiProfileContentScrollBox
│           └── userAiProfileContent
│               ├── professionalProfileGroup
│               ├── recruitingProfileGroup
│               ├── responsePreferencesGroup
│               ├── goalsGroup
│               ├── privacyGroup
│               └── previewGroup
├── msgInterface
└── mailAccessTab
```

## 4. Business Behavior

### Безопасные значения по умолчанию

- профиль выключен;
- внешняя обработка запрещена;
- язык — автоматически;
- детализация — сбалансированно;
- стиль — нейтральный;
- терминология — профессиональная;
- структура — автоматически.

### Согласие

`profileEnabled=true` разрешено только при `externalProcessingAllowed=true`. При первом согласии сохраняются `consentVersion=2026-07-22-v1` и `consentAcceptedAt`. При отзыве согласия персонализация выключается.

### Предпросмотр

`previewAiContext()` вызывает `UserAiContextService.buildContextPreview()` и раскрывает `previewGroup` через совместимый с CUBA 7.3 вызов `setExpanded(true)`. HTTP к LLM не выполняется.

## 5. Actions & Methods

| Метод | Назначение |
|---|---|
| `loadOrCreateUserAiProfile()` | загружает профиль или создаёт несохранённый |
| `initAiProfileOptions()` | задаёт локализованные enum options |
| `refreshProfileSummary()` | обновляет левую карточку |
| `previewAiContext()` | показывает очищенный контекст |
| `clearAiProfile()` | запрашивает подтверждение очистки |
| `validateAiProfile()` | проверяет согласие и диапазон опыта |
| `prepareProfileConsent()` | фиксирует или отзывает согласие |
| `commit()` | сохраняет настройки единым `CommitContext` |

## 6. Layout & Components

Вкладка следует направлению дизайна `JobCandidateEdit`: фиксированная левая панель 270 px и правая рабочая область с вертикальными секциями. Стили подключены через локальный mixin `user-ai-profile` и ограничены корнем `.user-ai-profile-editor`.

Глобальные `.v-*` правила не добавлялись. При ширине окна 1200 px горизонтальная прокрутка не требуется; содержимое справа прокручивается вертикально.

## 7. Проверки

Обязательны `ScreenViewIntegrityTest` (8/8), компиляция web/core/global, сборка SCSS, `clean assemble`, локальный smoke аватара, почты, профиля и локальный HTTP. Production-проверки для этого этапа запрещены.

## 8. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | Контроллер, datasource ИИ-профиля и JPQL перенесены в namespace `hunttech`; legacy-контракты `ExtUser`, `UserSettings` и messages pack сохранены |
| 2026-07-22 | Исправлено программное раскрытие секции предпросмотра ИИ-контекста: используется совместимый с CUBA 7.3 метод `setExpanded(true)` |
| 2026-07-22 | Вкладка «Обо мне» переработана в двухпанельный профессиональный ИИ-профиль; добавлены согласие, предпросмотр и атомарное сохранение |
