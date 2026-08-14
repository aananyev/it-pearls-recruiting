# CandidateCVEdit — Спецификация экрана редактирования резюме

> **Документ:** `docs/ui/CandidateCVEdit_Spec.md`  
> **Сущность:** `CandidateCV`  
> **Связанные документы:** `docs/entities/CandidateCV.md`

---

## Business & Context Intro

### 1. Назначение и Бизнес-смысл

Форма `CandidateCVEdit` — основной экран работы с резюме кандидата в HRM HuntTech. Позволяет рекрутеру просматривать и редактировать текст резюме, привязывать резюме к вакансии, управлять фотографией кандидата, генерировать письма и рекомендации.

### 2. Связи в интерфейсе и Навигация

- **Родительский экран:** `JobCandidateEdit` (вкладка «Резюме») или `CandidateCVBrowse`
- **Открытие:** кнопка «Создать»/«Изменить» в таблице резюме → `EditorScreenFacet`
- **Меню:** не имеет прямого пункта меню (открывается из контекста)

### 3. Краткий обзор бизнес-логики поведения

- **Открытие существующего резюме** → входной `CandidateCV` может быть detached с узким browse-view → `textCV` и `fileImageFace` читаются только после проверки `PersistenceHelper.isLoaded` или в post-load фазе → фото инициализируется в `onAfterShow2` → текст CV форматируется лениво при открытии вкладки CV
- **Создание нового** → поля пусты, `textCV` = null → `textResumeStringBuffer` пуст → фото отсутствует → default image
- **Сохранение** → валидация текста, сохранение фото через `FileDescriptor`
- **Кнопка «AI-анализ»** → вызывает `AiAnalysisHelper.analyze()` с промптом `RESUME_ANALYSIS`

---

## Lifecycle: загрузка фотографии и текста

### Проблема (исправлена 2026-07-22)

В `BeforeShowEvent` CUBA передаёт detached-экземпляр `CandidateCV` из вызывающего экрана (`CandidateCVBrowse` или `JobCandidateEdit`). Этот экземпляр загружен узким browse-view, не включающим `fileImageFace` и `textCV`. Вызов `getFileImageFace()` или `getTextCV()` в этой фазе вызывает:

```
IllegalStateException: Cannot get unfetched attribute [fileImageFace] from detached object
IllegalStateException: Cannot get unfetched attribute [textCV] from detached object
```

### Решение

1. **Фотография:** полностью удалена из `onBeforeShow()`. Инициализация → `onAfterShow2()` (после загрузки editor entity с полным `candidateCV-view`). Метод `setCandidatePicImage()` проверяет `PersistenceHelper.isLoaded(cv, "fileImageFace")` перед вызовом `getFileImageFace()`.

2. **Текст CV:** в `onBeforeShow()` добавлен guard:
   - `PersistenceHelper.isLoaded(edited, "textCV")` — проверка загруженности атрибута
   - если НЕ загружен → `textResumeStringBuffer` пуст, `convertToTextButton` disabled
   - полная инициализация → лениво при открытии вкладки CV (`ensureCvTextInitialized()`)

3. **Загрузка/очистка фото** через UI → `onCandidatePicSourceChange` → вызывает `setCandidatePicImage()` без изменений (сущность уже загружена).

### Инварианты

- `candidateCV-browse-view` НЕ содержит `fileImageFace` и `textCV`
- `candidateCV-view` содержит `fileImageFace` и `textCV`
- Тяжёлое форматирование текста CV НЕ запускается при открытии формы
- Сохранение без открытия вкладки CV НЕ изменяет `TEXT_CV`
- Новый `CandidateCV` открывается корректно

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-22 | Удалён вызов setCandidatePicImage из onBeforeShow; textCV защищён PersistenceHelper.isLoaded; восстановлен контракт AiAnalysisHelper |
| 2026-07-22 | Исправлен lifecycle фото: PersistenceHelper.isLoaded guard + перенос в onAfterShow2 |
| 2026-07-21 | Добавлена кнопка AI-анализ резюме |
