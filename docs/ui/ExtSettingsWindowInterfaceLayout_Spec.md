# ExtSettingsWindowInterfaceLayout — ровная компоновка вкладки «Интерфейс»

> Проект: **HRM HuntTech**.  
> Экран: `settings`.  
> Базовый контроллер: `ExtSettingsWindow`.  
> Навигационный контроллер: `ExtSettingsWindowEmailNavigation`.  
> Presentation-контроллер: `ExtSettingsWindowInterfaceLayout`.  
> Связанные документы: [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md), [ExtSettingsWindowEmailNavigation_Spec.md](ExtSettingsWindowEmailNavigation_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Вкладка «Интерфейс» управляет режимом главного окна, темой, языком, часовым поясом и стартовым экраном пользователя. После визуального редизайна все штатные компоненты CUBA Platform были сохранены, однако при фактическом разрешении около 1320 px подписи в первой колонке переносились на две–три строки, элементы управления имели разную ширину, а checkbox автоматического часового пояса сжимал lookup-поле. Форма выглядела ступенчатой и занимала лишнюю высоту.

Цель изменения — сформировать ровную двухколоночную форму, в которой пользователь быстрее сопоставляет подпись и значение. Изменение относится только к presentation state: данные, методы базового `SettingsWindow`, сохранение и бизнес-процессы не меняются.

### UI Context & Navigation

Пользовательский путь:

```text
Настройки → вкладка «Интерфейс»
           → Режим окна / Тема и язык / Часовой пояс / Экран запуска
           → ровная строка подпись → элемент управления
```

Screen ID `settings` продолжает использовать наследующий XML `ext-settings-window-email-navigation.xml`. Дескриптор подключает `ExtSettingsWindowInterfaceLayout`, который наследует действующий `ExtSettingsWindowEmailNavigation`; поэтому кликабельная навигация всех вкладок сохраняется.

### Behavior Summary

- открытие окна → базовый `ExtSettingsWindow` загружает значения прежним способом → presentation-контроллер не участвует в загрузке данных;
- завершение полной базовой инициализации → подписи получают единую ширину 190 px → длинные русские captions остаются в одной строке;
- отображение режима окна → `OptionsGroup` получает горизонтальную ориентацию → варианты располагаются в одной строке;
- отображение theme/language/default screen → lookup-поля занимают полную ширину второй колонки → вертикальные границы элементов совпадают;
- отображение часового пояса → `timeZoneLookup` становится расширяемым элементом `timeZoneBox`, а `timeZoneAutoField` сохраняет компактную ширину 96 px → checkbox больше не перекрывает значение;
- клик по навигации слева → работает прежний контроллер `ExtSettingsWindowEmailNavigation` → фокус переводится без изменения значения;
- Save/Cancel/смена пароля/сброс экранных настроек → выполняются прежними методами `SettingsWindow` → поведение не изменяется.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Screen ID | `settings` |
| XML | `ext-settings-window-email-navigation.xml` |
| Базовый XML | `ext-settings-window.xml` |
| Presentation-контроллер | `ExtSettingsWindowInterfaceLayout` |
| Родительский контроллер | `ExtSettingsWindowEmailNavigation` |
| Вкладка | `msgInterface` |
| Основная карточка | `interfaceAppearanceCard` |
| Сетка | `grid` |

## 2. Связь с моделью данных

Модель данных не изменяется. Вкладка продолжает использовать штатные компоненты и методы базового `SettingsWindow`.

Сохраняются без изменений:

- `modeOptions`;
- `appThemeField`;
- `appLangField`;
- `timeZoneLookup`;
- `timeZoneAutoField`;
- `defaultScreenField`;
- required-состояние темы;
- доступные значения lookup-компонентов;
- Save, Cancel, смена пароля и сброс экранных настроек;
- entity, datasource, views, JPQL, БД, Liquibase и сервисы.

Presentation-контроллер не содержит `setValue()`, `DataManager`, `UserSettings` или commit-операций.

## 3. Иерархия и взаимосвязь форм

```text
settings
└── ext-settings-window-email-navigation.xml
    └── ExtSettingsWindowInterfaceLayout
        └── ExtSettingsWindowEmailNavigation
            └── ExtSettingsWindow
                └── msgInterface
                    ├── interfaceSettingsSidebar
                    │   └── interfaceSettingsNavigation
                    └── interfaceSettingsContentScrollBox
                        ├── interfaceSettingsToolbar
                        └── interfaceAppearanceCard
                            └── grid
                                ├── mainWindowLabel → modeOptions
                                ├── visualThemeLabel → appThemeField
                                ├── languageLabel → appLangField
                                ├── timeZoneLabel → timeZoneBox
                                │                    ├── timeZoneLookup [expanded]
                                │                    └── timeZoneAutoField [96 px]
                                └── defaultScreenLabel → defaultScreenField
```

## 4. Модель поведения и интерактивность

| Строка | Подпись | Элемент | Геометрия |
|---|---|---|---|
| Режим окна | `mainWindowLabel` | `modeOptions` | подпись 190 px, варианты горизонтально |
| Тема | `visualThemeLabel` | `appThemeField` | lookup 100% второй колонки |
| Язык | `languageLabel` | `appLangField` | lookup 100% второй колонки |
| Часовой пояс | `timeZoneLabel` | `timeZoneLookup` + `timeZoneAutoField` | lookup расширяется, checkbox 96 px |
| Экран запуска | `defaultScreenLabel` | `defaultScreenField` | lookup 100% второй колонки |

Выравнивание выполняется после `super.init(params)`, когда базовый `SettingsWindow` уже создал и настроил компоненты. Значения компонентов не читаются и не записываются.

## 5. Логика управляющих элементов

`ExtSettingsWindowInterfaceLayout` вызывает только presentation API CUBA Platform:

- `Label.setWidth()` и `setAlignment()`;
- `OptionsGroup.setOrientation()`;
- `Component.setWidth()`;
- `HBoxLayout.resetExpanded()` и `expand()`;
- `CheckBox.setAlignment()`.

Навигация «Режим окна», «Тема и язык», «Часовой пояс», «Экран запуска» остаётся в `ExtSettingsWindowEmailNavigation` и по-прежнему меняет только active style и focus.

## 6. Визуальная компоновка элементов

### 6.1. До изменения

- первая колонка рассчитывалась по минимальной ширине и переносила captions;
- вертикальный `OptionsGroup` увеличивал высоту первой строки;
- lookup-поля имели разные theme-specific widths;
- `timeZoneBox` расширял checkbox вместо lookup-поля;
- границы элементов управления не совпадали по вертикали.

### 6.2. После изменения

- единая колонка подписей — 190 px;
- единая управляющая колонка — 100% доступной ширины;
- режим окна размещён горизонтально;
- часовой пояс занимает свободное место, checkbox «Авто» остаётся компактным;
- строки визуально образуют ровную сетку;
- SCSS и семь тем не изменяются: используются существующие локальные стили `.ext-settings-window`.

## 7. Соответствие CUBA Platform 7.3

1. Screen ID и XML inheritance сохраняются.
2. Базовый `SettingsWindow` и существующая навигация наследуются.
3. `OptionsGroup` использует штатный `HasOrientation.Orientation.HORIZONTAL`.
4. `HBoxLayout` использует штатный `ExpandingLayout.expand(Component)`.
5. Component ID, типы компонентов, bindings, required и actions не меняются.
6. Глобальные Vaadin-селекторы и локальный SCSS не изменяются.
7. Presentation-контроллер не изменяет значения компонентов.

## 8. Обязательные проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtSettingsWindowInterfaceLayoutTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowEmailNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAiNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowRemainingNavigationTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `ExtSettingsWindowInterfaceLayoutTest` — 3/3 PASS;
- navigation-тесты — PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A;
- SCSS — N/A по diff;
- `BUILD SUCCESSFUL`;
- local deploy того же HEAD;
- HTTP `/hrm/` = 200;
- critical Tomcat errors — NONE.

### Smoke Hermes

1. Открыть `settings` → «Интерфейс» в теме `halo`.
2. Проверить, что все пять captions отображаются в одну строку.
3. Проверить одинаковое начало и окончание lookup-полей.
4. Убедиться, что варианты режима окна расположены горизонтально.
5. Проверить отсутствие перекрытия `timeZoneLookup` и checkbox «Авто».
6. Переключить каждый пункт левой навигации и проверить focus.
7. Изменить режим, тему, язык, часовой пояс и стартовый экран; проверить Save и повторное открытие.
8. Проверить Cancel без сохранения.
9. Проверить смену пароля и сброс экранных настроек.
10. Проверить вкладки «Обо мне», email и AI без регрессии.
11. Повторить визуальный smoke во всех поддерживаемых темах, поскольку общий XML-компонентный контракт используется всеми темами, хотя SCSS не менялся.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Вкладка «Интерфейс» выровнена: подписи 190 px, режим окна горизонтальный, lookup-поля используют общую ширину, поле часового пояса расширяется без перекрытия checkbox; бизнес-логика и SCSS не изменены |
