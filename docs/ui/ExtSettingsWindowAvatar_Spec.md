# ExtSettingsWindowAvatar — фотография пользователя и левая панель «Обо мне»

> Проект: **HRM HuntTech**.  
> Экран: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Вкладка: `msgMyInfo` («Обо мне»).  
> XML: `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml`.  
> Компонент: `OvaFallbackImage`, XML-тег `<ovaFallbackImage>`, legacy ID `userPic`.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Фотография во вкладке «Обо мне» помогает пользователю идентифицировать собственный профиль и визуально связывает персональные настройки с единым дизайн-языком HRM HuntTech. Компонент должен показывать фотографию в круглом контейнере и сохранять существующий сценарий загрузки и очистки изображения.

Левая панель содержит фотографию, имя, должность, состояние ИИ-профиля, навигацию и предупреждение. Она должна оставаться внутри доступной высоты окна, прокручиваться независимо при нехватке места и не допускать наложения многострочных пунктов навигации.

Используется уже зарегистрированный UI-компонент `OvaFallbackImage`. Изменение не вводит новую сущность, поле, таблицу или миграцию и не меняет правила хранения файла.

### UI Context & Navigation

Экран открывается из стандартного меню настроек CUBA Platform. Пользователь выбирает вкладку «Обо мне» и видит фотографию в верхней части левой контекстной панели. Под фотографией остаётся существующий `Upload` с drop zone `dropZone`.

Связанные компоненты:

| Компонент | Контракт |
|---|---|
| `userPic` | основная фотография, `<ovaFallbackImage>` |
| `defaultPic` | существующий резервный `<image>`, не изменяется |
| `userAvatarUpload` | загрузка и очистка файла, не изменяется |
| `dropZone` | зона drag-and-drop, не изменяется |
| `picVBox` | контейнер изображений, не изменяется |
| `userAiProfileSidebar` | левая панель с локальной вертикальной прокруткой |
| `userAiProfileSectionNavigation` | контейнер пунктов, заменяемых контроллером на кнопки |

### Behavior Summary

- открытие вкладки → контроллер вызывает существующий `refreshProfilePhoto()` → фотография загружается прежним `FileDescriptorImageHelper`;
- фотография существует → `userPic` отображается через `OvaFallbackImage` → более специфичный локальный SCSS сохраняет круглую геометрию;
- фотография отсутствует → существующая логика переключает `userPic` и `defaultPic` → placeholder отображается круглым за счёт того же локального визуального слоя;
- высоты окна недостаточно → `userAiProfileSidebar` ограничивается доступной областью → появляется внутренняя вертикальная прокрутка без захода под footer;
- подпись навигационной кнопки переносится на несколько строк → кнопка получает автоматическую высоту → соседние пункты не перекрываются;
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
| XML-размер | `176px × 176px` |
| Фактический размер в исправленном sidebar | `152px × 152px` |
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
    ├── userAiProfileSidebar
    │   ├── dropZone
    │   │   ├── picVBox
    │   │   │   ├── userPic [ovaFallbackImage, XML 176×176]
    │   │   │   └── defaultPic [image, существующий fallback]
    │   │   └── userAvatarUpload
    │   ├── userAiProfileIdentity
    │   ├── userAiProfileSummary
    │   ├── userAiProfileSectionNavigation
    │   └── userAiProfileSensitiveWarningBox
    └── userAiProfileContentScrollBox [не изменяется]
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

Причина квадратного отображения на локальном screenshot находилась не в XML-компоненте: общий слой `settings-window-sections.scss` переопределял радиус изображения значением `8px !important`. Исправление подключается после общего слоя и ограничено `.ext-settings-window .user-ai-profile-editor`, поэтому для изображения применяется `border-radius: 50%`, круговой clip-path и локальное обрезание содержимого.

## 5. Визуальный контракт левой панели

Во всех семи темах подключается локальный partial:

```text
modules/web/themes/<theme>/com.company.hunttech/ext-settings-about-sidebar-fix.scss
```

Он изменяет только левую часть вкладки «Обо мне»:

- высота sidebar ограничена доступной областью с резервом перед footer;
- `min-height: 0` разрешает корректное сжатие Vaadin layout;
- `overflow-y: auto` включает независимую вертикальную прокрутку;
- прямоугольная drop zone больше не навязывает изображению рамку и радиус 8 px;
- `userPic` и отображаемый placeholder получают круглую геометрию 152×152 px;
- динамически созданные навигационные кнопки имеют `height: auto`;
- `.v-button-caption` допускает перенос строк с line-height 17 px;
- summary и предупреждение получают более компактные внутренние отступы.

Partial не содержит селекторов правой части:

- `.user-ai-profile-content`;
- `.user-ai-profile-toolbar`;
- `.user-ai-profile-section`.

Следовательно, рабочие поля, toolbar, карточки и аккордеоны справа не меняются.

## 6. Неизменяемые контракты

Запрещено и не выполнено:

- изменение `ExtSettingsWindow.java` и `ExtSettingsWindowEmailNavigation.java`;
- изменение базового или расширяющего XML;
- изменение entity `ExtUser` или её полей;
- изменение datasource, property, view или JPQL;
- добавление Liquibase/SQL;
- переименование `userPic`, `defaultPic`, `userAvatarUpload`, `dropZone`, `picVBox`;
- изменение бизнес-логики загрузки, сохранения и очистки;
- изменение правой рабочей области вкладки «Обо мне»;
- добавление глобальных SCSS-правил вне `.ext-settings-window`.

## 7. Обязательные проверки

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

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `ExtSettingsWindowAvatarComponentTest` — 3/3 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A, Java, XML, модель и view не изменены;
- SCSS всех семи тем — PASS;
- `BUILD SUCCESSFUL`;
- локальный deploy того же HEAD;
- HTTP `/hrm/` = 200;
- критические ошибки Tomcat отсутствуют.

### Visual smoke

Во всех поддерживаемых темах проверить:

- вкладка «Обо мне» открывается без SCSS/runtime ошибок;
- тёмная левая панель заканчивается выше footer и не выходит за нижнюю границу окна;
- при недостаточной высоте панель прокручивается внутри себя;
- пункты «Профиль рекрутера», «Предпочтения ответов», «Цели и интересы», «Конфиденциальность» и «Предпросмотр контекста» не накладываются друг на друга;
- фотография пользователя визуально круглая, без прямоугольной двойной рамки;
- существующая фотография отображается без искажения;
- при отсутствии фотографии отображается круглый placeholder;
- upload, очистка, сохранение и повторное открытие работают без регрессии;
- правая рабочая область, toolbar, поля и аккордеоны визуально не изменились;
- остальные вкладки `ExtSettingsWindow` и другие экраны не изменились.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Исправлена только левая панель вкладки «Обо мне»: устранён выход под footer и наложение многострочной навигации, принудительный радиус 8 px перекрыт локальным круглым контрактом `OvaFallbackImage` во всех семи темах; Java, XML и правая область не изменены |
| 2026-07-24 | Компонент `userPic` во вкладке «Обо мне» заменён с `Image` на `OvaFallbackImage` 176×176 px; Java-контроллер, entity, views и миграции БД не изменены |
