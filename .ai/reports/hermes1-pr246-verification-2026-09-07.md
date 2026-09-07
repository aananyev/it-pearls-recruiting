# Отчёт Hermes-1: верификация PR #246 (CompanyEdit checkbox-фикс) по протоколу передачи

Дата: 07.09.2026 17:30 (+04)
Статус: **PASSED_VERIFICATION** — merge и deploy состоялись, артефакты сверены по SHA.

## Ключевые SHA

| Роль | SHA |
|---|---|
| BASE_SHA (baseRefOid PR #246) | `eee8a9fc3822f224cdde98eeeb4e09e96727f4b7` |
| PR_HEAD (headRefOid = локальный origin/agent/antigravity-dev) | `a70f288d97a6635ad6f3c974381706ff997b72e7` |
| MERGE_SHA (merge-commit в master) | `7f916f6c02f4d36100e76a4f3def2abdc0243e08` |

## Шаг 1 — headRefOid vs локальный HEAD
GitHub `refs/heads/agent/antigravity-dev` = `a70f288d...`; локальный `origin/agent/antigravity-dev` = `a70f288d...`. **Совпадение.**

## Шаг 2 — diff BASE...PR_HEAD и git diff --check
Файлы в diff (8): `docs/ui/CompanyEdit_Spec.md` + `company-editor.scss` ×7 тем.
Заявленные в PR файлы **все присутствуют в diff**; посторонних файлов нет.
`git diff --check eee8a9fc...a70f288d` — чисто (нет whitespace-ошибок).

## Шаг 3 — git merge-tree --write-tree BASE PR_HEAD
`git merge-tree --write-tree eee8a9fc a70f288d` → exit 0, дерево `4834191e...`.
Дерево фактического merge-commit `7f916f6c^{tree}` = `4834191e...` — **идентично**. Конфликтов нет, выбор ours/theirs не требовался (резолв «обе стороны» сделан автором в ветке до мержа: `:not(.v-checkbox)` + `.v-checkbox`-сброс поверх master-версии).

## Шаг 4 — конфликт CompanyEdit
Не возникал на момент мержа (MERGEABLE после резолва автора). Остановки мержа не потребовалось.

## Шаг 5 — PR_HEAD в истории MERGE_SHA
`git merge-base --is-ancestor a70f288d 7f916f6c` — **да**.
`git diff origin/master origin/agent/antigravity-dev` — пусто (ветка не несёт ничего сверх master, потерь кода нет).

## Шаг 6 — сборка и деплой
Только из чистого master (`7f916f6c`, working tree clean, mutex деплоя свободен).
`scripts/start-app.sh`: BUILD SUCCESSFUL 4м44с, Tomcat перезапущен 17:06, HTTP 200.
В catalina.out: `Deployment of web application directory hrm-core has finished`, `Server startup in [46160] ms`, SEVERE по деплою нет.

## Шаг 7 — сверка SHA исходников с артефактами (JAR/WAR)

| Исходник (origin/master) | SHA-256 (16) | Внутри app-web-0.487-SNAPSHOT.jar | Статус |
|---|---|---|---|
| `company-edit.xml` | `20b2af7301348c8f` | `20b2af7301348c8f` | ✅ |
| `llm-chat-screen.xml` | `f75a71c949d3eafc` | `f75a71c949d3eafc` | ✅ |
| `llm-chat-launcher.js` | `bef4a0a297a75cdb` | `bef4a0a297a75cdb` | ✅ |

SCSS: компилируется в `VAADIN/themes/<t>/styles.css`; checkbox-фикс
`edit-form-control:not(.v-checkbox)` присутствует в **7/7** задеплоенных тем.
CSS лаунчера (`llm-chat-launcher`, круглая кнопка 52px, градиент) — в
`VAADIN/themes/*/com.company.hunttech/chat-style.css`, подключается через
`@import` в styles.css; HTTP-отдача подтверждена (curl 8080: 15 совпадений).

Artifact SHA-256 (16):
- `app-web-0.487-SNAPSHOT.jar` = `24bbb330c22bb797`
- `app-global-0.487-SNAPSHOT.jar` = `e19b390f0f327555`
- `app-core-0.487-SNAPSHOT.jar` = `e24266862e080a40`

## Шаг 8 — результат deploy
- 8080 (основной контур): jar 0.487, PR #246 + LLM-чат (модалка 420×560, draggable launcher) — **в наличии, SHA совпадают**.
- 8081 (песочница ChatGPT): jar 0.485 (сборка 14:49) — checkbox-фикса **нет** (`:not(.v-checkbox)` = 0 совпадений), LLM-чат есть.

## Диагноз «доработки не видны»
1. **Порт**: если проверка шла на 8081 — там песочница ChatGPT без фикса #246. На 8080 всё на месте.
2. **Кэш браузера**: `styles.css` отдаётся с `Cache-Control: max-age=3600` — после деплоя нужен жёсткий reload (Cmd+Shift+R) или закрытая/новая сессия Vaadin; старый UI-пиджинг в открытой вкладке не подхватывает новый CSS/JS.
3. Правки ChatGPT из `/private/tmp/it-pearls-recruiting-audit` (атомарность квот, guard fallbackPolicy, ключ alan) **не передавались в git ни одной веткой** — их нет ни в 8080, ни в 8081. Для них нужен PR по протоколу передачи (BASE_SHA/HEAD_SHA/diff-верификация).

## Вердикт
PR #246: **PASSED_VERIFICATION**. Merge и deploy подтверждены по SHA на всех уровнях (git → JAR → HTTP). Претензия «потерян код» не подтверждается: diff ветки vs master пуст, фикс в 7/7 темах в задеплоенном CSS.
