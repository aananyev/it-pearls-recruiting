# OpenPositionComment — комментарий к вакансии

> Дочерняя сущность OpenPosition (рейтинг + текст).
> Оптимизация: 2026-06-23.

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`OpenPositionComment` — комментарии и оценки по вакансии в HRM HuntTech (внутренняя переписка рекрутёров, рейтинг позиции). Композиция `OpenPosition.openPositionComments`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

`itpearls_OpenPositionComment.browse`, `itpearls_OpenPositionComment.edit`; вкладка/попап в OpenPositionBrowse и Edit. UI Spec: [browse](../ui/itpearls_OpenPositionComment.browse_Spec.md), [edit](../ui/itpearls_OpenPositionComment.edit_Spec.md).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Standalone loader в OpenPositionEdit; lazy по вкладке; кнопка рейтинга в browse открывает popup комментариев.

---

## 4. Представления

| View | Назначение |
|------|------------|
| `openPositionComment-browse-view` | Browse без LOB `comment` |
| `openPositionComment-edit-view` | Edit с LOB |
| `openPositionComment-view` | legacy |

## 5. Экраны

- Browse/Edit → specialized views, picker `openPosition-picker-view`, `extUser-picker-view`

## 7. Производительность

| Экран | Было | Стало |
|-------|------|-------|
| Browse | `_local` + openPosition `_local` | `openPositionComment-browse-view` |
| Edit | `_local` + глубокий openPosition | `openPositionComment-edit-view` |

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | browse/edit views, экраны, OpenPositionCommentServiceTest |
