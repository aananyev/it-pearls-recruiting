# HrmMainScreen — тематический фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Сервис выбора ресурса: `MainScreenBackgroundService`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран является основной рабочей областью HRM HuntTech. Фон персонализирует интерфейс, но не должен влиять на меню, dashboard, widgets, уведомления, резервы, favicon и загрузку бизнес-данных.

Системные фоны поставляются как статические JPG-файлы активной темы. Пользовательский фон загружается через `SettingsWindow`, хранится в FileStorage и имеет приоритет над системным каталогом.

Прямой URL вида `/hrm/dispatch/download?f=...` не используется для пользовательского фона: в этом сценарии браузер не получает файл, сохранённый через `FileLoader`. Пользовательский файл читается в память и публикуется как штатный `StreamResource` Vaadin.

Недоступность физического файла при сохранённом `FileDescriptor` не должна блокировать вход. Экран временно использует системный фон, не удаляя metadata; после восстановления FileStorage пользовательский фон снова становится доступен.

## UI Context & Navigation

Экран создаётся через Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen`;
- `@UiController("hrmMainScreen")`;
- `@UiDescriptor("hrm-main-screen.xml")`;
- legacy-регистрация `hrmMainScreen` в `web-screens.xml` отсутствует.

Путь настройки персонального изображения:

```text
SettingsWindow → Интерфейс → Фон главного экрана
```

После успешного сохранения `SettingsWindow` публикует `MainScreenBackgroundChangedEvent`. Текущая UI-сессия обновляет фон без повторного входа.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| Открытие главного экрана | Пользовательский descriptor отсутствует | Выбирается случайный системный `ThemeResource` активной темы |
| Открытие главного экрана | Descriptor не имеет префикс `hrm-main-background-` | Legacy-файл не считается пользовательским фоном |
| Разрешение пользовательского фона | Файл доступен в FileStorage | Поток читается через `readAllBytes()`, создаётся `StreamResource` |
| Регистрация пользовательского фона | Получен `StreamResource` | Вызывается `registerBackgroundResource()`, ресурс закрепляется скрытым `Image` в Vaadin connector tree |
| Формирование URL | `ResourceReference` вернул `app://APP...` | Префикс `app://APP` удаляется, CSS получает HTTP-путь |
| Чтение FileStorage | `FileStorageException` или `IOException` | Исключение перехватывается, применяется системный фон |
| Сохранение SettingsWindow | Commit успешен | Публикуется UI-scoped событие и фон обновляется |
| Смена системного варианта | Предыдущий индекс известен | Немедленное повторение варианта исключается |

## Каталог системных фонов

Для каждой поддерживаемой темы хранится ровно десять файлов `1920 × 1080`:

```text
modules/web/themes/{theme}/backgrounds/
├── 1.jpg
├── 2.jpg
├── ...
└── 10.jpg
```

Поддерживаемые темы:

```text
halo
havana
helium
hover
hunttech-modern
hunttech-modern-light
hunttech-modern-dark
```

Сервис возвращает системный ресурс без участия FileStorage:

```java
new ThemeResource("backgrounds/" + (variant + 1) + ".jpg")
```

URL разрешается относительно активной темы:

```text
VAADIN/themes/{activeTheme}/backgrounds/{1..10}.jpg
```

Исправление пользовательского фона не меняет `ThemeResource`, имена файлов, каталоги тем, алгоритм выбора и session-антиповтор.

## Пользовательский фон

Цепочка хранения:

```text
Upload
→ MainScreenBackgroundImageProcessor
→ FileStorage
→ UserSettings.fileImageFace
```

Контракт пользовательского файла:

- имя начинается с `hrm-main-background-`;
- файл имеет приоритет над системным ресурсом;
- загрузка ограничена `15 МБ` через `fileSizeLimit="15728640"`;
- PNG получает MIME `image/png`;
- JPG/JPEG получает MIME `image/jpeg`;
- WebP получает MIME `image/webp`;
- неизвестное расширение получает `application/octet-stream`;
- недоступный descriptor не удаляется автоматически;
- временные файлы при Cancel очищаются существующей логикой SettingsWindow.

`MainScreenBackgroundService.createCustomResource()` выполняет:

```text
FileLoader.openStream(descriptor)
→ InputStream.readAllBytes()
→ byte[]
→ new StreamResource(() -> new ByteArrayInputStream(bytes), fileName)
→ setMIMEType(...)
→ setCacheTime(-1)
```

`ExternalResource` и `FileDescriptorImageHelper.buildDispatchDownloadUrl()` для пользовательского фона не применяются.

## Регистрация StreamResource

`HrmMainScreen.buildBackgroundUrl()` поддерживает два типа ресурса:

1. `ThemeResource` — формируется статический путь активной темы;
2. `StreamResource` — вызывается `registerBackgroundResource()`.

`registerBackgroundResource()`:

1. удаляет предыдущий holder, если он остался в connector tree;
2. создаёт `Image` с пользовательским ресурсом;
3. устанавливает размер holder `0 × 0` и скрывает его;
4. добавляет holder в `mainVBox`;
5. получает URL через `ResourceReference.create(resource, holder, "src").getURL()`;
6. преобразует `app://APP` в относительный HTTP-путь;
7. отклоняет пустой URL как ошибку регистрации.

Скрытый `Image` является владельцем `StreamResource`. Его нельзя удалять до использования URL браузером, иначе connector resource key перестанет обслуживаться.

## Применение CSS

Фон назначается только контейнеру `mainVBox`. `mainDashboard` получает локальный прозрачный стиль.

Используются свойства:

```css
background-position: center center;
background-repeat: no-repeat;
background-size: 100% 100%;
```

Контракт `100% 100%` означает:

- рабочая область заполняется полностью;
- пустые полосы отсутствуют;
- изображение не обрезается;
- при несовпадении пропорций допускается геометрическое растягивание;
- логика одинакова для системного и пользовательского ресурса.

## Runtime-маркеры

| Элемент | Маркер | Назначение |
|---|---|---|
| `mainVBox` | `data-hrm-main-background="applied"` | подтверждает выполнение background lifecycle |
| `mainVBox` | `data-hrm-main-controller="HrmMainScreen"` | подтверждает фактический root controller |

## Автоматические проверки

`MainScreenBackgroundContractTest.customBackgroundUsesDirectDispatchUrlAndFallsBackOnStorageFailure()` сохраняет legacy-имя метода, но проверяет актуальный контракт:

- `readAllBytes()` и `ByteArrayInputStream`;
- создание `StreamResource`;
- назначение MIME-типа;
- перехват ошибок FileStorage и системный fallback;
- вызов `registerBackgroundResource()`;
- регистрацию через `ResourceReference` и `backgroundResourceHolder`;
- преобразование `app://APP`;
- отсутствие `ExternalResource` в `HrmMainScreen`;
- неизменность системного `ThemeResource` и каталога тем.

`HrmMainScreenIntegrationTest` не изменяется этой задачей и продолжает проверять root screen, `mainVBox`, dashboard, DOM-маркеры и UI-scoped обновление.

## Проверка Hermes

Hermes проверяет точный HEAD PR:

- совпадение branch HEAD и PR HEAD;
- отсутствие конфликтов;
- `git diff --check`;
- compile web/core tests;
- `MainScreenBackgroundContractTest`;
- `HrmMainScreenIntegrationTest`;
- `ScreenViewIntegrityTest 8/8`;
- сборку SCSS семи тем;
- `clean assemble` с `BUILD SUCCESSFUL`;
- clean local deploy;
- `http://localhost:8080/hrm/` → HTTP 200;
- пользовательский connector URL возвращает HTTP 200 и корректный `Content-Type`;
- URL не содержит `/dispatch/download?f=` и необработанный `app://APP`;
- системные фоны семи тем продолжают работать;
- недоступный файл включает системный fallback без удаления descriptor;
- Tomcat critical errors отсутствуют; P1=0; P2=0.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Пользовательский фон закреплён как `StreamResource` с байтами FileStorage, MIME-типом и Vaadin connector URL через `registerBackgroundResource()`; системные `ThemeResource` не изменены |
| 2026-07-26 | Пользовательский фон временно переводился на прямой FileStorage dispatch URL; ошибка чтения файла включала системный fallback без удаления descriptor |
| 2026-07-26 | Фон переведён с `cover` на точное растягивание `100% × 100%` |
| 2026-07-26 | Системные фоны вынесены в каталоги семи тем: 7 × 10 JPG |
| 2026-07-26 | Добавлены UI-scoped refresh, антиповтор системного варианта и screen-level integration test |
| 2026-07-26 | Синхронизированы `mainScreenId`; удалена несовместимая legacy-регистрация |
