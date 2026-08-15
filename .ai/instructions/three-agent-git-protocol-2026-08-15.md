# Протокол работы трёх агентов (2×Hermes + Antigravity)

Дата вступления в силу: 2026-08-15 (агенты уже запущены).
Проект: HRM HuntTech (CUBA 7.3). Канон: навык `chatgpt-hermes-coordination`,
секция .cursorrules «РАБОТА АГЕНТОВ (GIT-ПРОТОКОЛ)».

## Роли (по умолчанию)

| Агент | Роль | Что делает |
|---|---|---|
| Hermes-1 | CI/CD (единственный девопс) | проверка PR, merge, deploy, restart, миграции, отчёты `.ai/reports/`, pr-watcher |
| Hermes-2 | разработчик | фичи, ветки `agent/*`, PR; своя сессия Hermes |
| Antigravity | разработчик | второй поток фич; **Antigravity IDE** (agentic IDE Google), лаунчер `~/bin/antigravity-ide` (форвардер 18080→209.46.2.183); ветки `agent/*`, PR |

Роли меняются только пользователем. Если какой-то агент не работает —
его роль переходит к другому по согласованию.

## Изоляция (обязательно)

1. Каждый агент = своя ветка + свой worktree:
   `git worktree add ../hrm-<agent> -b agent/<тема>`
2. Общая рабочая копия (корень репо) — только master и деплой.
3. Рабочее дерево держать чистым: коммит + push после каждого шага.
   Незакоммиченные правки в общей копии запрещены — они валят чужие сборки
   (инцидент 2026-08-15: JobCandidateTestBrowse сломал compileJava).
4. Если локальная ветка занята другой копией — в worktree работаем на
   detached HEAD и пушим `git push origin HEAD:agent/<тема>`.
5. Перед началом работы: `git fetch`, ветка от свежего `origin/master`.

## Сборка/деплой (сериализация)

1. Один gradle-процесс в один момент времени. Два параллельных =
   deadlock на кэше, FTS-локи (hrm-core/work/ftsindex/write.lock), SIGKILL.
2. Деплой/restart делает ТОЛЬКО Hermes-1, только из чистой ветки master,
   когда рабочая копия свободна.
3. Локальный деплой/рестарт Tomcat — ТОЛЬКО через `scripts/start-app.sh`
   (shlock-mutex `deploy/.local-deploy.lock` + проверки git master/чистоты,
   запрет параллельных gradle, контроль порта 8080, лог
   `deploy/tomcat/logs/local-deploy.log`). Прямые `./gradlew deploy` /
   `deploy/tomcat/bin/startup.sh` мимо скрипта ЗАПРЕЩЕНЫ: они не видят
   mutex и ломают чужой деплой. Smoke на своей ветке — только
   `scripts/start-app.sh --force`, и только когда mutex свободен.
4. После kill -9 gradle: чистить `~/.gradle/caches/*.lock`, `~/.gradle/daemon`,
   `.gradle` worktree, FTS write.lock. Lock-файл `deploy/.local-deploy.lock`
   после kill -9 владельца снимается shlock автоматически.
5. Первый прогон в новом worktree долгий (10–15 мин, медленный
   repo.cuba-platform.com) — это норма, не убивать.
6. Дубли jar в `deploy/tomcat/shared/lib` (напр. groovy 2.5.2 + 2.5.23 →
   «Conflicting module versions», Context startup failed) — удалять старые дубли.

## Конфликты shared-файлов

- styles.scss (7 тем), messages*.properties, build.gradle, docs/README.md —
  конфликтуют почти всегда; резолвит АВТОР ветки, беря обе стороны.
- Версию build.gradle бампает только pre-commit hook (один агент в момент).
- После мержа чужого PR — обновить свою ветку (`git merge origin/master`)
  и разрешить конфликты ДО создания своего PR.
- Если merge rejected (конфликт) — резолвить в своём worktree и пушить
  в ту же ветку (`git push origin HEAD:agent/<тема>`).

## Коммуникация (асинхронно, через артефакты)

1. PR: описание (что/зачем/как проверено), метки `WAITING_FOR_HERMES`,
   статусы draft/ready, комментарии.
2. `.ai/instructions/{date}-{topic}.md` — разработчик → Hermes-1 (перед PR).
3. `.ai/tasks/{date}-{topic}.md` — Hermes-1 → разработчику (ошибки, правки).
4. `.ai/reports/{date}-PR{n}-build.md` — Hermes-1 (итоги проверки/деплоя).
5. Merge — только Hermes-1 после проверки и согласия пользователя.

## Definition of Done (каждый PR)

- [ ] mergeable clean
- [ ] контрактные тесты + ScreenViewIntegrityTest зелёные
- [ ] diff-ревью (bindings/actions/data-секции сохранены)
- [ ] отчёт в `.ai/reports/`
- [ ] merge → deploy → restart → HTTP 200 → smoke в браузере
