# Handoff: исправление Дашбордов рекрутера

Дата: 2026-09-21
Ветка: `agent/antigravity-dev`
Коммит реализации: `0ad233c31 fix(dashboard): восстановить дашборды рекрутера`

## Причина

Kanban и «Воронка найма» получали `IteractionList` через
`iteractionList-edit-view`. Вложенный view `iteraction-list-type-view` не
содержал признаки `Iteraction`, используемые в `RecruiterDashboardStage.resolve`.
Обращение к `signOurInterviewAssigned`, `signEndCase` и другим признакам на
detached объекте приводило к `IllegalStateException: Cannot get unfetched attribute`.

## Реализация

- Добавлен узкий `recruiter-dashboard-iteraction-list-view`.
- View содержит только поля карточек и все признаки, применяемые нормализатором этапов.
- `RecruiterCandidateKanbanWidget` и `RecruiterHiringFunnelWidget` переведены на новый view.
- Существующие KPI, этапы, правила подсчёта и навигация в карточку кандидата не изменены.
- В `RecruiterDashboardsContractTest` добавлена регрессионная проверка view и его использования обоими виджетами.
- Актуализирован `docs/ui/RecruiterDashboards_Spec.md`.

## Проверки

- OCR code review: замечаний нет.
- `RecruiterDashboardsContractTest`: 5 тестов, 0 ошибок.
- `ScreenViewIntegrityTest`: 8 тестов, 0 ошибок.
- `:app-web:compileJava`: успешно.
- `:app-web:buildScssThemes`: успешно.
- Полный `clean assemble`: успешно завершён штатным Gradle wrapper.
