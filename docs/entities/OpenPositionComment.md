# OpenPositionComment — комментарий к вакансии

> Дочерняя сущность OpenPosition (рейтинг + текст).
> Оптимизация: 2026-06-23.

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
| 2026-06-23 | browse/edit views, экраны, OpenPositionCommentServiceTest |
