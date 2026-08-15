# OOM тестовых экранов кандидатов (JobCandidateTest*)

**Статус:** Closed 2026-08-15 (устранено Hermes-1, PR #143 автора agent/antigravity-dev проверен и смержен)
**Автор исходного кода:** agent/antigravity-dev (PR #143)
**Исполнитель фикса:** Hermes-1 (по поручению пользователя «добейся открытия всех экранов»)

## Симптом

«Тест 2: High-Density DataGrid» при открытии: `RemoteException: Java heap space`
(Tomcat Xmx2048m, `deploy/tomcat/bin/setenv.sh`). Loader тянул всех 11 537
кандидатов + генераторы аватаров на каждую строку.

## Причины (3)

1. **OOM:** тестовые loaders `jobCandidatesDl` без `maxResults` — все 11 537 строк
   в память с генераторами колонок.
2. **Окна «Файл … не найден» (CubaImage):** локальный `fileStorage/` пуст (0 файлов),
   в `sys_file` — 13 616 записей; 1816 кандидатов и 76 пользователей ссылались на
   несуществующие файлы аватаров. CubaImage падал на рендере → ErrorNotification.
3. **Канбан Тест 3 пустой:** `buildKanbanBoard()` вызывался в `BeforeShowEvent`,
   когда `jobCandidatesDc` ещё не загружен (REFRESH приходит позже) → «Всего кандидатов 0».

## Исправления (коммиты master)

- `9d868b99 fix(ui): ограничение выборки тестовых экранов кандидатов — устранение OOM`
  — `maxResults="200"` на `jobCandidatesDl` во всех 6 `job-candidate-test*-browse.xml`.
- `72d939fb fix(ui): канбан Тест 3 строится по событию загрузки данных, а не в BeforeShow`
  — `@Subscribe(id="jobCandidatesDc", target=Target.DATA_CONTAINER)` на
  `CollectionChangeEvent` (`CollectionChangeType.REFRESH`) → `buildKanbanBoard()`.
- **Данные (dev-БД hunttech, не код):** `UPDATE hunttech_job_candidate SET file_image_face = NULL`
  (1816), `sec_user` аватары NULL (76), `hunttech_user_settings.file_image_face` NULL (0) —
  битые ссылки при пустом FileStorage; fallback-аватары (инициалы) вместо ошибок.

## Проверка (CDP smoke, 2026-08-15)

| Экран | URL окна | Ошибки | Итог |
|-------|----------|--------|------|
| Тест 1: Split-View (Halo) | #main/0 | нет | PASS, 200 строк |
| Тест 2: High-Density DataGrid | #main/1 | нет (был heap space) | PASS, 200 строк |
| Тест 3: Kanban Pipeline & Метрики | #main/2 | нет (были нулевые метрики) | PASS после фикса |
| Тест 4: Executive Card-Grid | #main/3 | нет | PASS |
| Тест 5: Row Expansion (Details) | #main/4 | нет | PASS, 200 строк |

## Backlog

- Восстановить `fileStorage/` с прода (rsync) для полноценных аватаров — по запросу.
- Тест 3: распределение по колонкам канбана — демо-имитация `i % 4`
  (не реальные стадии кандидата) — автору эскиза на доработку при желании.
- Решить, нужны ли 5 пунктов «Тест 1–5» в общем меню (сейчас в «Рекрутинг») —
  по запросу пользователя.
