# Синхронизация Antigravity — 06.09.2026

## Задача
Обновить код Antigravity (ветка agent/antigravity-dev) с последними коммитами и смерженными PR из репозитория HRM HuntTech.

## Контекст
- Текущая ветка: `agent/antigravity-dev`
- В репозитории hrm-hermes2 смержены: PR #241 (OpenRouter/B.AI providers), PR #239 (web-toolkit jar), PR #240 (ai-chat fix с конфликтом build.gradle)
- Общая копия ../hunttech_recruiting — только master, деплой Hermes-1
- Antigravity НЕ трогает общую копию, работает в своем worktree

## Последний sync state
- origin/master впереди на 2 коммита относительно origin/agent/antigravity-dev (commits: cf029e61, e357b6a1)
- В worktree Antigravity есть модифицированные файлы (CompanyReestrBrowse, CompanyReestrEdit, LlmChatScreen, llm-chat-launcher.js и 7 тем styles.css)
- 15 untracked файлов в .ai/reports/ от предыдущих задач

## Шаги синхронизации (по протоколу 3-agent git protocol 2026-08-15)

### 1. Обновить tracking ветку от origin/master
```bash
git fetch origin
git merge origin/master agent/antigravity-dev    # или git rebase origin/master
# или принудительная синхронизация:
git fetch origin agent/antigravity-dev
git checkout -B agent/antigravity-dev origin/agent/antigravity-dev
```

### 2. Проверить конфликты shared-файлов
Shared-файлы, которые могут конфликтовать (по протоколу):
- `modules/web/themes/*/styles.scss` (7 тем) — аддитивные конфликты оставлять обе стороны
- `modules/core/test/com/company/hunttech/core/*ContractTest.java` (CompanyEdit, ExtSettingsWindow, ExtUserEdit — EXCLUSIVE Antigravity, НЕ ТРОГАТЬ)
- `build.gradle` — версия бампается pre-commit hook'ом в момент коммита
- `modules/core/test/com/company/hunttech/core/messages*.properties`

Список изменений в diff от смерженных PR, которые могут затронуть Antigravity:
- build.gradle: версия 0.469 (из PR #240, сохранена протоколом "keep PR branch version")
- modules/web/src/com/company/hunttech/web/extension/llm-chat-launcher.js
- modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml
- modules/web/src/com/company/hunttech/web/screens/mainscreen/ExtMainScreen.java
- 7 тем: halo, hunttech-modern-dark, hunttech-modern-light, hunttech-modern, hover, Havana, helium — chat-style.css

### 3. Резолвить конфликты (если есть)
- styles.scss: если обе стороны добавили @import/@include — оставить обе строки (аддитивный конфликт)
- build.gradle: версия меняет только pre-commit hook, вручную не бампать
- ContractTests (CompanyEdit, ExtSettingsWindow, ExtUserEdit): НЕ ТРОГАТЬ — эксклюзивно Antigravity, Hermes-1 отклоняет PR с этими файлами

### 4. Собрать и проверить локально (по желанию, guard'ы opcional)
```bash
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*Antigravity*" --no-daemon
# или проверка SCSS:
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-web:buildScssThemes --no-daemon
```

### 5. Commit + push в свою ветку
Каждый шаг: коммит (русское сообщение) + git push origin HEAD:agent/antigravity-dev
Формат коммита: `feat(antigravity): синхронизация с последними смерженными PR — <описание>`

### 6. Создать отчёт
Файл отчета: `.ai/reports/{date}-sync-antigravity-dev.md`
- Список обновленных файлов
- Резолвленные конфликты (если были)
- Статус: mergeable clean / conflict status
- Подготовка к PR (base=master, метка WAITING_FOR_HERMES)

## Важные запреты (по протоколу)
- ❌ Не трогать CompanyEdit, ExtSettingsWindow, ExtUserEdit — EXCLUSIVE Antigravity, Hermes-1 отклоняет
- ❌ Не деплоить master / не рестарт общей среды — только Hermes-1
- ❌ Не коммитить чужие изменения из общей копии
- ❌ Не бампить version build.gradle вручную — pre-commit hook делает это автоматически
- ❌ Не запускать parallel gradle-processes (мутек FTS-локи)

## Ожидаемый результат
Ветка agent/antigravity-dev синхронизирована с последними смерженными изменениями, conflict'ы resolved, push в origin/agent/antigravity-dev, отчет в .ai/reports/.