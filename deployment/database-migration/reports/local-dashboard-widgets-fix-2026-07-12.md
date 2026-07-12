# Исправление dashboard/widgets после локальной миграции

Дата: 2026-07-12
Контур: локальная база `hunttech`, локальный Tomcat проекта `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`
Production: не изменялся

## Симптом

После локальной миграции `itpearls` -> `hunttech` не работали виджеты главного экрана.

Главный экран приложения использует dashboard:

- screen: `extMainScreen`;
- dashboard code: `recruiting-dashboard`;
- таблица данных: `dashboard_persistent_dashboard`.

## Причина

В `dashboard_persistent_dashboard.dashboard_model` после миграции остались старые `frameId` вида:

- `itpearls_MyPhotoWidget`;
- `itpearls_MyJobCandidatesWidget`;
- `itpearls_ResearchingDiagramms`;
- другие `itpearls_*` widget frame id.

В текущем коде проекта эти виджеты зарегистрированы уже как `hunttech_*`, например:

- `hunttech_MyPhotoWidget`;
- `hunttech_MyJobCandidatesWidget`;
- `hunttech_ResearchingDiagramms`.

Из-за этого dashboard пытался создать несуществующие legacy screen/widget ids.

## Исправление

В локальной базе `hunttech` выполнена замена:

```sql
update dashboard_persistent_dashboard
set dashboard_model = replace(dashboard_model, 'itpearls', 'hunttech')
where dashboard_model like '%itpearls%';

update dashboard_widget_template
set widget_model = replace(widget_model, 'itpearls', 'hunttech')
where widget_model like '%itpearls%';
```

Также обновлены миграционные файлы:

- `deployment/database-migration/migration/20-transform-restored-copy-to-hunttech.sql`;
- `deployment/database-migration/validation/validate-migration-target.sql`.

## Проверки

Результат после исправления:

| Метрика | Значение |
|---|---:|
| `dashboard_model` с `itpearls` | 0 |
| `widget_model` с `itpearls` | 0 |
| `recruiting-dashboard` содержит `hunttech_` frame ids | да |

Все `frameId`, извлеченные из dashboard-моделей, соответствуют текущим `@UiController("hunttech_*")` виджетов в коде.

## Перезапуск

Tomcat был штатно остановлен и запущен заново.

Проверка:

- `http://localhost:8080/hrm/` возвращает HTTP 200;
- `hrm` web block стартовал;
- `hrm-core` core block стартовал;
- remoting servlet инициализирован.

## Остаточные локальные предупреждения

В логах остаются ранее известные фоновые ошибки, не связанные с dashboard migration:

- FTS indexing падает из-за локального PDFBox/Tika mismatch и отсутствующих файлов в локальном `fileStorage`;
- Emailer встречает сообщения с `caption is null`;
- `cuba.tempDir` указывает на локальный путь без записи.

Эти проблемы не блокируют старт приложения, но требуют отдельной задачи перед production-переключением.
