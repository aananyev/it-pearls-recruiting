# ExtSettingsWindowAvatar — фотография пользователя

> Проект: **HRM HuntTech**.  
> Экран: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Вкладка: `msgMyInfo` («Обо мне»).  
> XML: `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml`.  
> Компонент: `OvaFallbackImage`, XML-тег `<ovaFallbackImage>`, legacy ID `userPic`.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Фотография во вкладке «Обо мне» помогает пользователю идентифицировать собственный профиль и визуально связывает персональные настройки с единым дизайн-языком HRM HuntTech. Компонент должен показывать фотографию в круглом контейнере и сохранять существующий сценарий загрузки и очистки изображения.

Замена стандартного `Image` на `OvaFallbackImage` использует уже зарегистрированный UI-компонент HRM HuntTech. Изменение не вводит новую сущность, поле, таблицу или миграцию и не меняет правила хранения файла.

### UI Context & Navigation

Экран открывается из стандартного меню настроек CUBA Platform. Пользователь выбирает вкладку «Обо мне» и видит фотографию в верхней части левой контекстной панели. Под фотографией остаётся существующий `Upload` с drop zone `dropZone`.

Связанные компоненты:

| Компонент | Контракт |
|---|---|
| `userPic` | основная фотография, теперь `<ovaFallbackImage>` |
| `defaultPic` | существующий резервный `<image>`, не изменяется |
| `userAvatarUpload` | загрузка и очистка файла, не изменяется |
| `dropZone` | зона drag-and-drop, не изменяется |
| `picVBox` | контейнер изображений, не изменяется |

### Behavior Summary

- открытие вкладки → контроллер вызывает существующий `refreshProfilePhoto()` → фотография загружается прежним `FileDescriptorImageHelper`;
- фотография существует → `userPic` отображается в круглом контейнере 176×176 px → источник данных и Java-код не меняются;
- фотография отсутствует → существующая логика переключает `userPic` и `defaultPic` → пользователь видит прежний placeholder;
- загрузка или очистка файла → работает существующий `userAvatarUpload` → datasource `extUserDs` и property `userAvatar` сохраняются;
- повторное открытие → изображение читается из прежнего поля `ExtUser.userAvatar` → миграция БД не требуется.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Screen ID | `settings` |
| Tab ID | `msgMyInfo` |
| Component ID | `userPic` |
| GUI-контракт | `com.hunttech.hrm.gui.components.OvaFallbackImage` |
| XML-тег | `ovaFallbackImage` |
| Размер | `176px × 176px` |
| Масштабирование | `SCALE_DOWN` |
| Fallback theme path | `icons/no-programmer.jpeg` |

Фактическое legacy-имя компонента — `OvaFallbackImage`. Оно не переименовывается в `OvalFallbackImage`, поскольку используется в регистраторах, загрузчике и XML-контрактах приложения.

## 2. Связь с моделью данных

Модель данных не изменяется:

- datasource: `extUserDs`;
- entity: `ExtUser`;
- поле файла: `userAvatar`;
- view: `extUser-view`;
- загрузка изображения: `FileDescriptorImageHelper.setUserProfilePhoto()`.

Не добавляются поля entity, Liquibase, SQL, новые таблицы, JPQL или views.

## 3. Иерархия компонентов

```text
msgMyInfo
└── userAiProfileMainBox
    └── userAiProfileSidebar
        └── dropZone
            ├── picVBox
            │   ├── userPic [ovaFallbackImage, 176×176]
            │   └── defaultPic [image, существующий fallback]
            └── userAvatarUpload
```

## 4. Модель поведения и совместимость

`OvaFallbackImage` наследует контракты `OvalImage` и `FallbackImage`, которые в конечном итоге расширяют базовый CUBA `Image`. Поэтому существующая Java-инъекция:

```java
@Inject private Image userPic;
```

остаётся совместимой. Контроллер, lifecycle, обработчики загрузки, сохранение и очистка изображения не изменяются.

Настройки компонента:

```xml
<ovaFallbackImage id="userPic"
                  width="176px"
                  height="176px"
                  ovalWidth="176px"
                  ovalHeight="176px"
                  fallbackThemePath="icons/no-programmer.jpeg"
                  scaleMode="SCALE_DOWN"/>
```

## 5. Неизменяемые контракты

Запрещено и не выполнено:

- изменение `ExtSettingsWindow.java`;
- изменение entity `ExtUser` или её полей;
- изменение datasource, property, view или JPQL;
- добавление Liquibase/SQL;
- переименование `userPic`, `defaultPic`, `userAvatarUpload`, `dropZone`, `picVBox`;
- изменение бизнес-логики загрузки, сохранения и очистки;
- изменение глобальных SCSS-правил.

## 6. Обязательные проверки

Hermes проверяет точный HEAD без изменения кода:

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAvatarComponentTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `ExtSettingsWindowAvatarComponentTest` — 2/2 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A, модель и view не изменены;
- `BUILD SUCCESSFUL`;
- локальный deploy того же HEAD;
- HTTP `/hrm/` = 200;
- критические ошибки Tomcat отсутствуют.

### Visual smoke

Во всех поддерживаемых темах проверить:

- вкладка «Обо мне» открывается без XML loader/runtime ошибок;
- фотография пользователя круглая и имеет размер 176×176 px;
- существующая фотография загружается без искажения;
- при отсутствии фотографии отображается прежний placeholder;
- upload, очистка, сохранение и повторное открытие работают без регрессии;
- остальные вкладки `ExtSettingsWindow` и другие экраны не изменились.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-24 | Компонент `userPic` во вкладке «Обо мне» заменён с `Image` на `OvaFallbackImage` 176×176 px; Java-контроллер, entity, views и миграции БД не изменены |
