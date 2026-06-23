# RecrutiesTasks — подписка рекрутёра на вакансию

> Транзакционная связь рекрутёр ↔ вакансия с планом и датами.
> Оптимизация: 2026-06-23.

## 4. Представления

| View | Назначение |
|------|------------|
| `recrutiesTasks-browse-view` | Browse |
| `recrutiesTasks-edit-view` | Edit |
| `openPosition-rtasks-browse-view` | FK openPosition в browse |
| `openPosition-rtasks-picker-view` | picker openPosition в edit |

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-23 | browse/edit views, узкий FK openPosition, RecrutiesTasksServiceTest |
