# CandidateCVEdit — Спецификация экрана редактирования резюме

> **Документ:** `docs/ui/CandidateCVEdit_Spec.md`  
> **Сущность:** `CandidateCV`  
> **Связанные документы:** `docs/entities/CandidateCV.md`

---

## Business & Context Intro

### 1. Назначение и Бизнес-смысл

Форма `CandidateCVEdit` — основной экран работы с резюме кандидата в HRM HuntTech. Позволяет рекрутеру:
- просматривать и редактировать текст резюме;
- привязывать резюме к вакансии;
- управлять фотографией кандидата;
- генерировать письма и рекомендации на основе шаблонов.

Экран открывается из карточки кандидата (`JobCandidateEdit`) или из общего списка резюме (`CandidateCVBrowse`).

### 2. Связи в интерфейсе и Навигация

- **Родительский экран:** `JobCandidateEdit` (вкладка «Резюме») или `CandidateCVBrowse`
- **Открытие:** через кнопку «Создать»/«Изменить» в таблице резюме
- **Дочерние элементы:** lookup поля для вакансии (`OpenPosition`)
- **Меню:** не имеет прямого пункта меню (открывается из контекста)

### 3. Краткий обзор бизнес-логики поведения

- При открытии существующего резюме → загружается текст, фото, привязка к вакансии
- При создании нового → поля пусты, фото отсутствует
- Сохранение → валидация текста, сохранение фото через `FileDescriptor`
- Кнопка «AI-анализ» → вызывает `AiAnalysisHelper.analyze()` с промптом `RESUME_ANALYSIS`

---

## Lifecycle: загрузка фотографии

### Проблема (исправлена 2026-07-22)

`CandidateCVEdit.onBeforeShow()` вызывал `setCandidatePicImage()`, который обращался к `getEditedEntity().getFileImageFace()`. В `BeforeShowEvent` CUBA передаёт detached-экземпляр из вызывающего экрана с узким browse-view, не включающим `fileImageFace`. Это вызывало:

```
IllegalStateException: Cannot get unfetched attribute [fileImageFace] from detached object
```

### Решение

1. В `setCandidatePicImage()` добавлена проверка:
   ```java
   if (cv == null || !PersistenceHelper.isLoaded(cv, "fileImageFace")) {
       return;
   }
   ```

2. Инициализация фото перенесена из `onBeforeShow()` в `onAfterShow2()` — фазу, когда editor loader уже загрузил полный `candidateCV-view` в `candidateCVDc`.

3. Обработчик `onCandidatePicSourceChange` (реакция на загрузку/очистку фото через UI) продолжает вызывать `setCandidatePicImage()` без изменений — в этой фазе сущность уже загружена.

### Инварианты

- `candidateCV-browse-view` НЕ расширяется (не добавляется `fileImageFace`)
- `candidateCV-view` (editor view) включает `fileImageFace`
- Загрузка фото через UI работает после инициализации
- Очистка фото → default image

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-22 | Исправлен lifecycle фото: `PersistenceHelper.isLoaded` guard + перенос в `onAfterShow2` |
| 2026-07-21 | Добавлена кнопка AI-анализ резюме |
