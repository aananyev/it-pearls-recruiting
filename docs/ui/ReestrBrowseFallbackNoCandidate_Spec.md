# ReestrBrowse: sidebar-аватар кандидата (fallback `icons/no-candidate.png`, `SCALE_DOWN`)

> Контракт sidebar-изображения реестров **взаимодействий** и **резюме**: `IteractionListReestrBrowse` (`hunttech_IteractionListReestr.browse`) и `CandidateCVReestrBrowse` (`hunttech_CandidateCVReestr.browse`).
> Компонент: [OvaFallbackImage](../screens/components/OvaFallbackImage.md). Образцы реестров: [ProjectReestrBrowse_Spec.md](ProjectReestrBrowse_Spec.md), [OpenPositionReestrBrowse_Spec.md](OpenPositionReestrBrowse_Spec.md).

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Обе Reestr-формы показывают в левом sidebar (Split-View, 312px) круглый аватар кандидата 120×120px. Если у кандидата нет фотографии (`fileImageFace == null`) или файл недоступен, вместо логотипа компании (`icons/no-company.png`) должен показываться **силуэт человека** (`icons/no-candidate.png`) — аватар по смыслу относится к кандидату, а не к компании/проекту. Изображение масштабируется по размеру элемента (`SCALE_DOWN`), а не по исходному размеру файла.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

- `IteractionListReestrBrowse` — «Реестр взаимодействий»: таблица взаимодействий + профильный сайдбар кандидата.
- `CandidateCVReestrBrowse` — «Реестр резюме»: таблица резюме + профильный сайдбар кандидата.
- Обе формы используют `SmartCvUploadScreen` (см. [SmartCvUploadUnifiedArchitecture.md](../02_business_logic/SmartCvUploadUnifiedArchitecture.md)).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Есть читаемое фото кандидата → `FileDescriptorResource` из `candidate.fileImageFace`.
- Нет кандидата / нет фото / файл недоступен / запись не выбрана → `ThemeResource` `icons/no-candidate.png`.
- Контроллер никогда не перетирает fallback чужим путём (только `icons/no-candidate.png`); XML-дескриптор декларирует тот же fallback для первичной отрисовки.

## 1. Точка вызова и контекст (Invocation & Context)

| Форма | Controller | Java-класс | XML-дескриптор | Lookup | Сущность |
|-------|------------|------------|----------------|--------|----------|
| Реестр взаимодействий | `hunttech_IteractionListReestr.browse` | `com.company.hunttech.web.screens.iteractionlist.IteractionListReestrBrowse` | `iteraction-list-reestr-browse.xml` | `iteractionListsTable` | `IteractionList` |
| Реестр резюме | `hunttech_CandidateCVReestr.browse` | `com.company.hunttech.web.screens.candidatecv.CandidateCVReestrBrowse` | `candidate-cv-reestr-browse.xml` | `candidateCVsTable` | `CandidateCV` |

Оба контроллера: `extends StandardLookup<T>`, `@LoadDataBeforeShow`.

## 2. Контракт sidebar-аватара (ovaFallbackImage `logoPic`)

Одинаковый для обеих форм (XML-дескрипторы, sidebar `detailPane` 312px, шапка `profileHeader`):

```xml
<ovaFallbackImage id="logoPic" width="120px" height="120px" ovalWidth="120px" ovalHeight="120px"
                  align="TOP_CENTER" stylename="job-candidate-avatar"
                  fallbackThemePath="icons/no-candidate.png" scaleMode="SCALE_DOWN"/>
```

| Атрибут | Значение | Комментарий |
|---------|----------|-------------|
| `id` | `logoPic` | единый ID для обеих форм (контракт инжекции в Java) |
| `width` / `height` | `120px` | слот элемента |
| `ovalWidth` / `ovalHeight` | `120px` | круглая геометрия (`ht-oval-image`, `border-radius: 50%`) |
| `stylename` | `job-candidate-avatar` | стиль аватара кандидата |
| `fallbackThemePath` | `icons/no-candidate.png` | **замена `icons/no-company.png`** — силуэт человека, а не логотип компании |
|| `scaleMode` | `SCALE_DOWN` | масштабирование по размеру элемента 120×120px (валидное значение enum Image.ScaleMode в CUBA 7.3) |

`ovaFallbackImage` объявлен **без** `dataContainer`/`property` (статичный fallback-контракт): при загрузке XML применяется `fallbackThemePath`, далее источник управляет Java-контроллер.

## 3. Java-контракт контроллеров

Обе формы в `updateSidebarDetails(...)` / `clearSidebarDetails()` управляют `logoPic` через `setSource(...)`; единственный допустимый ThemeResource-путь — `icons/no-candidate.png`:

| Контроллер | Метод | Строки | Поведение |
|------------|-------|--------|-----------|
| `IteractionListReestrBrowse` | `updateSidebarDetails` | ~381 | `candidate != null && fileImageFace != null` → `FileDescriptorResource`; иначе → `ThemeResource "icons/no-candidate.png"` |
| `IteractionListReestrBrowse` | `clearSidebarDetails` | ~446 | всегда → `ThemeResource "icons/no-candidate.png"` (запись не выбрана) |
| `CandidateCVReestrBrowse` | `updateSidebarDetails` | ~442 | то же правило по `cv.getCandidate().fileImageFace` |
| `CandidateCVReestrBrowse` | `clearSidebarDetails` | ~524 | всегда → `ThemeResource "icons/no-candidate.png"` |

Инвариант: **контроллеры не перетирают fallback чужим путём** — в коде обеих форм отсутствует `icons/no-company.png`; `ThemeResource` устанавливается только с `icons/no-candidate.png`, а при наличии фото — `FileDescriptorResource` (реальное изображение приоритетнее placeholder).

## 4. Тематический ресурс `icons/no-candidate.png`

Файл `no-candidate.png` — PNG-силуэт человека (по образцу `icons/no-programmer.jpeg`) — добавляется во **все 7 тем**:

`halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-dark`, `hunttech-modern-light`

Путь в каждой теме: `modules/web/themes/{theme}/icons/no-candidate.png`. Синхронизация по 7 темам обязательна (правило README.md «Обязательная синхронизация тем»); частичное обновление одной темы запрещено. `ThemeResource` резолвится относительно активной темы при смене темы.

## 5. Проверки

1. Открыть реестр взаимодействий / резюме без выбранной записи → в sidebar круглая заглушка `no-candidate.png` (силуэт человека), 120×120, без искажений.
2. Выбрать кандидата с фото → отображается фото (`FileDescriptorResource`).
3. Выбрать кандидата без фото → заглушка `no-candidate.png` (`SCALE_DOWN`).
4. Файл фото недоступен в FileStorage → fallback `no-candidate.png`, экран не падает (механика `OvaFallbackImage`).
5. Проверка во всех 7 темах.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-28 | Fallback sidebar-аватара `logoPic` реестров `IteractionListReestrBrowse` / `CandidateCVReestrBrowse` переведён с `icons/no-company.png` на `icons/no-candidate.png`; `scaleMode` изменён на `SCALE_DOWN` (валидное значение enum Image.ScaleMode в CUBA 7.3, 120×120px). Java-контроллеры (`updateSidebarDetails`/`clearSidebarDetails`) ставят `ThemeResource icons/no-candidate.png`. Новый ресурс `icons/no-candidate.png` (PNG-силуэт человека) добавлен во все 7 тем. |
