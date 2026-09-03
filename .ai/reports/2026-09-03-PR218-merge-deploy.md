# Deploy Report — 2026-09-03 (Hermes-1)

## PR #218 — Реестр компетенций (SkillTreeReestrBrowse)
**Branch:** agent/antigravity-dev @ 2b48a054 → merge commit 27b60aea (master)
**Result:** ✅ MERGED + DEPLOYED + RESTARTED

## Предмержевая проверка
- QA-отчёт автора: PASS, тесты 23/23 зелёные (.ai/reports/qa-skilltree-reestr-browse.md)
- Code review ocr CLI: High-находки (Wiki-XSS, stale-data) исправлены в 43b7745f
- Diff-ревью: 13 файлов, маркеров конфликта нет, секретов нет; data-секция экрана сохранена (skillTreesDc + loader, view skillTree-reestr-browse-view со всеми геттерами)
- mergeable: CLEAN, миграций в PR нет
- build.gradle: версия 0.417→0.419 (bump хуком по коммитам) — корректно

## Деплой
- Tomcat остановлен ЯВНО перед деплоем (версия jar изменилась 0.419→0.422 — защита от on-the-fly rescan, см. cuba-deployment-runbook)
- PostgreSQL проверен поднят (select 1 = 1)
- `./gradlew deploy restart --no-daemon` — BUILD SUCCESSFUL in 52s
- Deployment finished: hrm 18233ms, hrm-core 18393ms — без SEVERE при старте

## Верификация
- HTTP /hrm/ → 200
- Widgetset .nocache.js → 200
- app-web-0.422-SNAPSHOT.jar (23:32): SkillTreeReestrBrowse.class (cafe babe) + skill-tree-reestr-browse.xml внутри
- web-menu.xml в jar: пункт SkillTreeReestr присутствует
- sys_db_changelog: все 62 миграций 26*/ применены (0 отсутствующих)
- catalina.out: ошибок регистрации нового экрана нет

## Предсуществующие ошибки (НЕ регрессия PR #218)
- `templateLetterBox`/`projectEditorWorkspace` to expand — битые expand-ID в project-edit.xml / open-position-edit.xml (падают при открытии этих экранов; существовали до мержа)
- `AiCredentialService` bean not found — AdminAiConfigurationBrowse (335 упоминаний в логе, давно)
- ClassCastException ObjectStreamClass$Caches$1 — benign Tomcat stop-фазы

## Итог
master = 27b60aea, версия 0.422, приложение работает. Ветку agent/antigravity-dev удалить после завершения работы агента (PR-мерж с --delete-branch выполнен для remote).
