# Position — должность

> Справочник должностей с LOB-описаниями.
> Оптимизация: 2026-06-23.

## 4. Представления

| View | Назначение |
|------|------------|
| `position-browse-view` | Browse без LOB |
| `position-edit-view` | Edit без LOB (LOB в BeforeShow) |
| `position-picker-view` | FK |

## 5. Экраны

- **PositionBrowse:** batch-кэш `standartDescription` / `whoIsThisGuy` для иконок
- **PositionEdit:** reload LOB в `onBeforeShow`

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-23 | PositionEdit: исправлен lazy LOB reload — отдельные `.add()` для `standartDescription` и `whoIsThisGuy` |
| 2026-06-23 | browse/edit/picker views, batch LOB Browse, PositionServiceTest |
