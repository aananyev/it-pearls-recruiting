# Antigravity IDE — рабочее дерево агента-разработчика (схема 3 агентов)

Ты работаешь В ЭТОМ каталоге (worktree, ветка agent/antigravity-dev).
Схема: Hermes-1 (CI/CD) · Hermes-2 (разработка) · ты (разработка, 2-й поток).

ОБЯЗАТЕЛЬНО прочитай перед началом работы:
1. .ai/instructions/agent-brief-antigravity.md — твой брифинг
2. .ai/instructions/three-agent-git-protocol-2026-08-15.md — полный протокол
3. .cursorrules — секция «РАБОТА АГЕНТОВ (GIT-ПРОТОКОЛ, ОБЯЗАТЕЛЕН)»

Правила:
- Работай только здесь. Общая копия (../hunttech_recruiting) — только master и деплой Hermes-1, не трогай. В worktree Hermes-2 (../hrm-hermes2) не заходи
- Каждый готовый шаг: коммит (русское сообщение) + git push origin HEAD:agent/antigravity-dev
- PR base=master, описание + метка WAITING_FOR_HERMES; merge делает только Hermes-1
- gradle-прогоны сериализуй (один в момент, первый прогон в worktree долгий — норма). ВСЕ gradle-вызовы — через обёртку: bash ../hunttech_recruiting/scripts/agent-gradle.sh <args>
- Локальный запуск СВОЕЙ ветки для проверки UI (разрешено): bash ../hunttech_recruiting/scripts/start-app.sh --branch "$PWD" — соберёт твою ветку на общем Tomcat (http://localhost:8080/hrm/); guard'ы: без merge-конфликтов, без новых миграций относительно origin/master, mutex. После проверки верни master: bash ../hunttech_recruiting/scripts/start-app.sh. Миграции на общую БД и деплой master — только Hermes-1
- Конфликты shared-файлов резолвь сам, обе стороны; version build.gradle — только pre-commit hook
- Всё на русском; не коммить чужое
- **Перед отправкой PR**: запускай субагента-тестировщика (QA), который проверяет выполненную работу и не принимает результат при наличии ошибок
- **Перед выполнением работы**: запускай субагента-аналитика, который разрабатывает и актуализирует документацию
- **Если разработка касается дизайна форм**: запускай субагента UI/UX дизайнера, который помогает аналитику в проектировании и дизайне форм
