# OvaFallbackImage — кастомный UI-компонент HRM HuntTech

> **Расположение документации:** `docs/ui-components/` (legacy). При миграции на нумерованную структуру проекта — целевой путь: `docs/03_ui_components/OvaFallbackImage.md`.  
> См. также: [OvalImage](OvalImage.md), [FallbackImage](FallbackImage.md), [ImageProcessingService](../../services/file-storage/ImageProcessingService.md).  
> Legacy-имя `OvaFallbackImage` и XML-тег `ovaFallbackImage` сохраняются без переименования.

## Назначение и бизнес-смысл (What & Why)

`OvaFallbackImage` — единый CUBA-компонент HRM HuntTech для круглого или овального изображения с theme fallback. Он применяется для фотографий кандидатов и пользователей, логотипов проектов и компаний, когда экран должен сохранять стабильную геометрию независимо от наличия файла.

Компонент решает три бизнес-задачи:

- пользователь всегда видит понятный визуальный идентификатор или placeholder;
- отсутствующий либо временно недоступный файл не блокирует открытие формы;
- экран не создаёт два независимых `Image` и не переключает их видимость вручную.

`FileDescriptor` содержит metadata, но физический файл хранится отдельно. Поэтому ненулевое значение поля не является достаточным подтверждением того, что ресурс можно прочитать и передать Vaadin.

## UI Context & Navigation

Компонент используется внутри screen descriptor как обычный data-bound `Image`:

- `JobCandidateEdit` — профиль кандидата;
- `CandidateCVEdit` — фотография кандидата;
- `ExtSettingsWindow` — фотография пользователя;
- `IteractionListEdit` — фотография кандидата и логотип проекта;
- другие экраны с локальным fallback-контрактом.

`OvaFallbackImage` не создаёт навигацию, loaders или отдельное состояние формы. Источником истины остаётся `dataContainer/property` либо явно заданный `Resource`.

## Behavior Summary

- bound значение равно `null` → применяется `fallbackThemePath` → layout сохраняет размер изображения;
- **без привязки к данным** (`dataContainer`/`property` отсутствуют — статичная иллюстрация AI Control Plane форм, справочников) → `fallbackThemePath` показывается как обычный source, но только пока контроллер не установил явный `Resource` через `setSource(...)`;
- bound значение содержит `FileDescriptor` → helper открывает файл через `FileLoader.openStream()` → при успехе используется стандартный `FileDescriptorResource`;
- metadata существует, но физический файл отсутствует или storage недоступен → ошибка преобразуется в fallback → UI продолжает строиться;
- nested property unfetched на detached entity → чтение `ValueSource` перехватывается → отображается fallback до загрузки подходящего view;
- физический файл восстановлен и экран получает загруженный descriptor → стандартное изображение отображается снова;
- изменение темы → fallback разрешается как `ThemeResource` активной темы;
- изменение `ovalWidth` или `ovalHeight` → второй размер синхронизируется → форма сохраняет круглую геометрию.

## 1. Архитектурная структура

| Слой | Путь | Роль |
|---|---|---|
| gui | `modules/gui/src/com/hunttech/hrm/gui/components/OvaFallbackImage.java` | интерфейс `extends OvalImage, FallbackImage` |
| web | `modules/web/src/com/hunttech/hrm/web/components/WebOvaFallbackImage.java` | реализация поверх стандартного `WebImage` |
| delegate | `modules/web/src/com/hunttech/hrm/web/components/delegate/OvalImageShapeDelegate.java` | геометрия и класс `ht-oval-image` |
| delegate | `modules/web/src/com/hunttech/hrm/web/components/delegate/FallbackImageResourceDelegate.java` | разрешение bound value и fallback |
| helper | `modules/web/src/com/company/hunttech/web/util/FileDescriptorImageHelper.java` | проверка физической читаемости и создание ресурсов |
| loader | `modules/web/src/com/hunttech/hrm/web/loaders/OvaFallbackImageLoader.java` | атрибуты `ovalWidth`, `ovalHeight`, `fallbackThemePath`, `ovalBackground` |
| XML registration | `modules/web/src/com/hunttech/hrm/web/cuba-ui-component.xml` | регистрация XML-тега |
| Java registration | `modules/web/src/com/hunttech/hrm/web/config/HunttechUiComponentsRegistrar.java` | регистрация в `UiComponents` |
| config | `modules/global/src/com/company/hunttech/config/HunttechImageConfig.java` | глобальный fallback path |

`WebOvaFallbackImage` наследует стандартный CUBA `WebImage`. Собственный Vaadin `ServerRpc` компонент не регистрирует; oval- и fallback-поведение реализуются composition/delegation.

## 2. Геометрия изображения и свойство `stretchToOval`

- `ht-oval-image` задаёт `border-radius: 50%`;
- `ovalWidth` и `ovalHeight` задаются явно для стабильного slot;
- при указании только одного oval-размера loader синхронизирует второй;
- **Правило «один размер → круг»**: если задан только `width` или только `height`, второй недостающий размер логически принимается равным первому (например, `width="80px"` трактуется как круг `80 × 80px`);
- `SCALE_DOWN` или `CONTAIN` используются, когда искажение исходного изображения недопустимо;
- локальный экранный SCSS может задавать рамку, фон и тень только внутри namespace экрана.

### Свойство `stretchToOval`

| Параметр | Значение |
|---|---|
| **Имя XML-атрибута** | `stretchToOval` |
| **Java API** | `boolean isStretchToOval()`, `void setStretchToOval(boolean stretchToOval)` |
| **Тип** | `boolean` |
| **Default** | `false` |

**Назначение:**
> Масштабирует переданное изображение независимо по горизонтали и вертикали до внутренних размеров `OvalFallbackImage`, после чего изображение отображается через овальную маску. Если задан только `width` или только `height`, компонент трактует область отображения как круг с диаметром, равным указанному размеру.

Когда `stretchToOval="true"`:
- Изображение масштабируется по осям X и Y так, чтобы занимать 100% ширины и 100% высоты компонента (`object-fit: fill`), после чего маскируется овалом (`border-radius: 50%; overflow: hidden;`).
- Допускается изменение исходного aspect ratio изображения (эффект изображения, натянутого на круглую/овальную поверхность, без обрезания краёв по правилу cover и без пустых полей по правилу contain).
- Исходный бинарный файл в `FileStorage` не модифицируется — преобразование является строго визуальным на уровне UI/rendering.
- Полная обратная совместимость: если `stretchToOval="false"` или атрибут не задан, действует стандартное поведение компонента (`scaleMode=SCALE_DOWN`).

### XML-примеры использования

**Пример 1 — круг с натянутым логотипом:**
```xml
<custom:ovalFallbackImage
        id="companyLogo"
        width="80px"
        stretchToOval="true"
        fallbackThemePath="icons/no-company.png"/>
```

**Пример 2 — овал:**
```xml
<custom:ovalFallbackImage
        id="projectLogo"
        width="120px"
        height="80px"
        stretchToOval="true"
        fallbackThemePath="icons/no-company.png"/>
```

**Пример 3 — со стандартным тегом `ovaFallbackImage` и привязкой к данным:**
```xml
<ovaFallbackImage id="candidateImage"
                  dataContainer="iteractionListDc"
                  property="candidate.fileImageFace"
                  width="112px"
                  height="112px"
                  stretchToOval="true"
                  fallbackThemePath="icons/no-programmer.jpeg"/>
```


## 3. Fallback и FileStorage

Приоритет fallback:

| Приоритет | Источник |
|---|---|
| 1 | `setFallbackResource()` / `setFallbackThemePath()` в Java |
| 2 | `fallbackThemePath` в screen XML |
| 3 | `hunttech.defaultFallbackImagePath` |

Безопасная цепочка:

```text
ValueSource<FileDescriptor>
→ tryApplyFallback()
→ FileDescriptorImageHelper.fileExists()
→ FileLoader.openStream(descriptor)
→ readable: стандартное обновление WebImage
→ unavailable/error: fallbackResource
```

### 3.1. Проверка физического файла

`FileDescriptorImageHelper.fileExists()` использует `FileLoader.openStream()` в `try-with-resources`. Это проверяет фактический маршрут FileStorage, а не только наличие metadata. Поток закрывается сразу после открытия; helper не вызывает `readAllBytes()` и не удерживает бинарное содержимое в памяти.

Перехватываются:

- `FileStorageException` при недоступности storage или отсутствии файла;
- `IOException` при закрытии/чтении потока;
- `RuntimeException` при создании presentation-ресурса.

При ошибке descriptor не удаляется и сущность не изменяется.

### 3.2. Защита detached/unfetched value

`FallbackImageResourceDelegate.tryApplyFallback()` оборачивает чтение `valueSource.getValue()` в `try/catch`. Это важно для nested binding вида `candidate.fileImageFace`: detached-объект может быть получен из picker с узким view, в котором поле изображения отсутствует.

Результат ошибки presentation-слоя:

- исключение не выходит в lifecycle экрана;
- компонент получает `fallbackResource`;
- owning screen обязан использовать view, содержащий атрибуты, которые реально читает контроллер или binding.

Fallback не заменяет Data View Integrity. Он предотвращает падение UI, а корректный узкий view обеспечивает отображение фактического изображения.

### 3.3. Статичный fallback без привязки к данным

Сценарий: `ovaFallbackImage` объявлен **без** `dataContainer`/`property` — компонент используется как статичная тематическая иллюстрация (`fallbackThemePath="icons/ai/*.png"` в AI Control Plane формах, `icons/hunttech-logo.png` в справочниках). У такого компонента `valueSource == null`, поэтому `updateComponent()` не вызывается вообще (он срабатывает только при `setValueSource`/change). Два уровня защиты:

- **Немедленное применение в `setFallbackThemePath()`**: при `valueSource == null` и `host.getSource() == null` fallback ставится как source сразу при загрузке XML-дескриптора — иначе пустой овал;
- **`tryApplyFallback()`** дополнительно применяет fallback при `valueSource == null`, но НЕ затирает явный source, установленный контроллером через `setSource(...)` (эталон: `OpenPositionEdit.updateProjectLogoImage()`).

Проверка `host.getSource()` добавлена в интерфейс `FallbackImageHost` и делегат, чтобы не ломать контроллеры, управляющие изображением вручную.

## 4. Программное использование

```java
OvaFallbackImage image = uiComponents.create(OvaFallbackImage.NAME);
image.setOvalWidth("80px");
image.setOvalHeight("80px");
image.setFallbackThemePath("icons/no-company.png");
image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
```

Для обычного `Image` и программного источника используется `FileDescriptorImageHelper`:

```java
FileDescriptorImageHelper.setCandidateFace(image, fileLoader, descriptor);
FileDescriptorImageHelper.setCompanyLogo(image, fileLoader, descriptor);
```

Helper сначала сбрасывает прежний `ValueSource`, проверяет физическую доступность и назначает `FileDescriptorResource` либо `ThemeResource`.

## 5. Ограничения и правила применения

- component ID, `dataContainer` и `property` сохраняются при визуальном рефакторинге;
- нельзя считать ненулевой `FileDescriptor` гарантией существования бинарного файла;
- нельзя читать nested image getter detached-сущности без подходящего view или `PersistenceHelper.isLoaded`;
- нельзя возвращать пару `realImage/defaultImage` и ручное `setVisible()` без отдельного обоснования;
- storage failure не должен удалять metadata автоматически;
- fallback не должен инициировать loaders, запросы или commit;
- глобальные Vaadin-селекторы ради одного изображения запрещены.

## 6. Проверки

Обязательные сценарии:

1. `null` descriptor → theme fallback.
2. Существующий и читаемый файл → фактическое изображение.
3. Metadata существует, физический файл отсутствует → fallback без ошибки экрана.
4. FileStorage временно недоступен → fallback и диагностический warning.
5. Nested property unfetched → fallback без `Cannot get unfetched attribute` в UI-thread.
6. После загрузки подходящего view реальное изображение отображается.
7. Размер, круглая форма и scale mode одинаковы для файла и placeholder.
8. Проверка выполняется во всех поддерживаемых темах экрана.

Контракт компонента и интеграции дополнительно защищается тестами:

- `WebOvaFallbackImageTest`;
- `LeftSidebarAvatarComponentTest`;
- `IteractionListRpcCompatibilityContractTest`;
- `IteractionListLayoutStorageContractTest`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-16 | Исправлена работа `stretchToOval`: селектор CSS `.v-image.ht-oval-stretch` скорректирован под DOM Vaadin (`CubaImageWidget` как корневой `<img>`), добавлен перехват `setSource` в `WebOvaFallbackImage` для сохранения `ScaleMode.FILL`, в `ProjectEdit` настроен кликабельный размер fileupload и расширен `person-picker-view` свойством `fileImageFace` |
| 2026-09-16 | Реализовано XML-свойство `stretchToOval` (boolean, default: false), правило «один размер → круг», CSS-класс `.ht-oval-stretch` во всех 7 темах, регистрация алиаса `ovalFallbackImage` |
| 2026-08-28 | Исправлен `scaleMode` с `SCALE_TO_FIT` на `SCALE_DOWN` (валидное значение enum Image.ScaleMode в CUBA 7.3) в реестрах `IteractionListReestrBrowse` / `CandidateCVReestrBrowse` |
| 2026-08-14 | `OvaFallbackImageLoader` читает XML-атрибут `ovalBackground` (фон-подложка под прозрачные изображения), вызов `setOvalBackground(...)`; в `ProjectEdit` логотипу проекта задан тёмно-серый фон `#3a3e44` |
| 2026-08-14 | Унаследован атрибут `ovalBackground` от `OvalImage` (фон-подложка под прозрачные изображения); `WebOvaFallbackImage` реализует его через общий `OvalImageBackgroundSupport` |
| 2026-08-13 | Статичный fallback без `dataContainer`/`property` применяется сразу в `setFallbackThemePath()` (`updateComponent` у компонента без valueSource не вызывается); guard `host.getSource() == null` — не затирает ручной `setSource(...)` контроллера; в `FallbackImageHost` добавлен `getSource()` |
| 2026-07-27 | Проверка descriptor переведена на фактическое `FileLoader.openStream()`; `FallbackImageResourceDelegate` защищён от unfetched `ValueSource` и ошибок FileStorage с переходом на theme fallback |
| 2026-07-21 | `JobCandidateEdit.candidatePic` переведён на `OvaFallbackImage` 176×176 с binding и fallback |
| 2026-06-30 | `RoundImageWithFallback` переименован в `OvaFallbackImage`; oval и fallback поведение вынесены в delegates |
