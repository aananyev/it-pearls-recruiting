# Протокол работы агентов: Antigravity — Главный разработчик и Владелец master

Дата вступления в силу: 2026-08-15 (актуализировано с назначением Antigravity Главным разработчиком и Владельцем ветки `master`).
Проект: HRM HuntTech (CUBA 7.3). Канон: `.cursorrules`, секция «РАБОТА АГЕНТОВ (GIT-ПРОТОКОЛ)».

## Роли и полномочия

| Агент | Рабочая директория | Ветка / Режим | Роль и полномочия |
|---|---|---|---|
| **Antigravity** | `/Users/alekseyananyev/StudioProjects/hrm-antigravity` (worktree) + общая копия `/Users/alekseyananyev/StudioProjects/hunttech_recruiting` | `master` / `agent/antigravity-dev` | **Главный разработчик (Tech Lead), Руководитель проектов, Владелец ветки `master`**. Единоличное право слияния в `master`, утверждение PR, архитектурный надзор, управление миграциями БД/промптов, локальный деплой и релизный деплой на продакшен. |
| **Hermes-1** | `/Users/alekseyananyev/StudioProjects/hunttech_recruiting` | CI/CD воркер под контролем Antigravity | Автоматизированные тесты, фоновый CI/CD runner. |
| **Hermes-2** | `/Users/alekseyananyev/StudioProjects/hrm-hermes2` | `agent/hermes2-*` | Разработка фич (поток 1), сдача PR Главному разработчику Antigravity. |
| **dsh** | `/Users/alekseyananyev/StudioProjects/hrm-dsh` | `agent/dsh-*` | Разработка фич, сдача PR Главному разработчику Antigravity. |
| **ChatGPT** | `/Users/alekseyananyev/StudioProjects/chatgpt` | песочница | Исследования, прототипы, архитектурный анализ (без прямого пуша в master). |

Главный разработчик (Antigravity) осуществляет сквозное руководство проектом и субагентами («Аналитик», «Промпт-инженер», «UI/UX-дизайнер», «Java Backend-разработчик», «Frontend-разработчик», «Технический писатель», «Автоматизированный тестировщик (QA)», «Оператор HRM»).

## Владение веткой master и порядок слияния

1. **Владелец главной ветки**: Antigravity является единственным гейткипером ветки `master`.
2. **Мерж собственных изменений**: Antigravity мержит свои проверенные изменения в `master` напрямую или через PR. Метка `WAITING_FOR_HERMES` для Antigravity упразднена.
3. **Приемка PR других агентов**:
   - Агенты разработки (Hermes-2, dsh) создают PR в ветку `master`.
   - Главный разработчик (Antigravity) или назначенный QA-субагент валидирует изменения, проводит diff-ревью, проверяет контрактные тесты и OCR review.
   - Главный разработчик выполняет мерж в `master`: `gh pr merge N --merge --delete-branch` или `git merge`.

## Защита ключевых экранов (эксклюзивно под контролем Antigravity)

Ни один сторонний агент (Hermes-2, dsh) не имеет права изменять следующие компоненты без прямого утверждения Главного разработчика:

### 1. CompanyEdit и CompanyReestrEdit
- `modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/company/company-reestr-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/company/CompanyEdit.java`
- `modules/web/src/com/company/hunttech/web/screens/company/CompanyReestrEdit.java`
- `modules/web/themes/*/com.company.hunttech/company-editor.scss`
- `modules/core/test/com/company/hunttech/core/CompanyEdit*LayoutContractTest.java`
- `modules/core/test/com/company/hunttech/core/CompanyReestrEdit*LayoutContractTest.java`

### 2. ExtSettingsWindow
- `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml`
- `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml`
- `modules/web/themes/*/com.company.hunttech/settings-window-sections.scss`
- `modules/web/themes/*/com.company.hunttech/edit-screen-shared-styles.scss`
- `modules/core/test/com/company/hunttech/core/ExtSettingsWindow*ContractTest.java`

### 3. ExtUserEdit
- `modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml`
- `modules/web/themes/*/com.company.hunttech/ext-user-editor.scss`
- `modules/core/test/com/company/hunttech/core/ExtUserEdit*ContractTest.java`
- `modules/core/test/com/company/hunttech/core/ExtUserChangePasswordContractTest.java`

### 4. IteractionListEdit и IteractionListReestr
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-reestr.xml`
- `modules/web/themes/*/com.company.hunttech/job-candidate-editor.scss`
- `modules/core/test/com/company/hunttech/core/IteractionList*ContractTest.java`

## Обязательный OCR Code Review (для всех изменений)

Перед компиляцией и созданием PR **всегда** запускать OCR CLI review:
```bash
ocr review --audience agent
```
Результат review (PASS/FAIL + найденные проблемы) прикладывать к отчёту или PR.
OCR CLI использует скилл `open-code-review` (Alibaba). Не основной моделью и не субагентами — ТОЛЬКО через `ocr review --audience agent`.

## Сборка и деплой

1. **Сериализация gradle-процессов**: один gradle-процесс в один момент времени.
   Все gradle-прогоны — через обертку `scripts/agent-gradle.sh <args>` (shlock-mutex).
2. **Локальный запуск и деплой master**:
   - `scripts/start-app.sh` — сборка и запуск `master` на общем Tomcat (http://localhost:8080/hrm/).
   - `scripts/start-app.sh --branch <worktree>` — временный запуск ветки разработчика для smoke-проверки UI.
3. **Быстрый деплой (Fast Deployment)**:
   - `bash scripts/fast-deploy.sh --conf` — 1–3 секунды (XML-экраны, локализация messages*.properties без перезапуска);
   - `bash scripts/fast-deploy.sh --web` — 15–20 секунд (Java-контроллеры веб-модуля без перекомпиляции тем);
   - `bash scripts/fast-deploy.sh --themes` — 20–30 секунд (SCSS-стили).
4. **Продакшен-деплой**:
   - Выполняется Главным разработчиком Antigravity: `bash scripts/deploy-prod.sh -y`.
   - Строгое следование регламенту безопасных миграций (`safe-db-and-prompt-migrations.md`): резервная копия БД, регистрация changelog, deep diff 100%.

## Конфликты shared-файлов

- styles.scss (7 тем), messages*.properties, build.gradle, docs/README.md —
  конфликты резолвит автор ветки, сохраняя обе стороны.
- Версию build.gradle бампает только pre-commit hook.

## Коммуникация и артефакты

1. PR: описание (что сделано, обоснование, результаты тестов, OCR review).
2. `.ai/instructions/{date}-{topic}.md` — постановка задач.
3. `.ai/tasks/{date}-{topic}.md` — задания и чекпоинты для разработчиков.
4. `.ai/reports/{date}-PR{n}-build.md` — отчеты о сборках и деплоях.
5. Definition of Done:
   - [ ] mergeable clean (без конфликтов)
   - [ ] контрактные тесты + ScreenViewIntegrityTest зелёные
   - [ ] OCR review пройден (PASS, 0 blocking issues)
   - [ ] diff-ревью (bindings/actions/data-секции сохранены)
   - [ ] документация синхронизирована (docs/ui/*, docs/entities/*)
   - [ ] merge в master → локальный/прод деплой → HTTP 200