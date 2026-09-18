# Отчёт: почему «в деплое старая версия» CompanyEdit / ExtSettingsWindow

**Дата:** 2026-09-05 09:15  
**Вопрос:** исследуй, почему в текущей реализации нет исправлений экранов из PR #225, #226, #227.

---

## 1. Гипотеза не подтвердилась: в ГИТЕ и в ЛОКАЛЬНОМ ДЕПЛОЕ исправления ЕСТЬ

Проверено программно на всех уровнях:

### Git (master = 2fc96ead)
| Проверка | Результат |
|---|---|
| `git merge-base 8f425f0f HEAD` (PR #226) | = `8f425f0f` — коммит **в истории master** |
| `git merge-base b16358ff HEAD` (PR #225) | = `b16358ff` — **в истории master** |
| `git merge-base c5967a15 HEAD` (PR #227) | = `c5967a15` — **в истории master** |
| `git diff 8f425f0f..HEAD -- company-edit.xml` | пусто — фиксы не откатывались |
| `git diff b16358ff..HEAD -- ext-settings-window.xml` | пусто |
| `git diff c5967a15..HEAD -- *.scss (halo)` | пусто |

### Файлы в master
- `company-edit.xml`: `expand` снят с 4 вкладок, scrollBox вкладок с классом `company-tab-scroll` (строки 266, 436, 731, 787) ✅
- `ext-settings-window.xml`: `settingsTabSheet` без `height="100%"` (строка 141) ✅
- `ext-settings-window-main-background.xml`: комментарий «height убран (фикс D1)» на месте ✅
- `edit-screen-shared-styles.scss`: `overflow-x: hidden` на tabsheetpanel ✅
- `company-editor.scss`: `height:100%!important` снят с вложенного `.v-scrollable` (комментарий «регрессия сент. 2026») ✅

### Локальный деплой (этот Tomcat, localhost:8080) — проверено по HTTP, а не по git
- `app-web-0.434-SNAPSHOT.jar` (собран 05.09 00:28): XML внутри jar содержит `company-tab-scroll` ×4, tabSheet без height ✅
- `curl http://localhost:8080/hrm/VAADIN/themes/hunttech-modern/styles.css` (667 КБ, HTTP 200):
  - `company-tab-scroll{...}` — есть (PR #226) ✅
  - `v-tabsheet-tabsheetpanel{box-sizing...overflow-x:hidden!important}` ×3 — есть (PR #227) ✅
  - `settings-section-toolbar{...min-height:64px;margin-bottom:12px}` — есть (PR #225) ✅
  - старого `overflow:hidden!important` на tabsheetpanel — 0 вхождений (регрессии нет) ✅
- Все 7 тем в `deploy/tomcat/webapps/hrm/VAADIN/themes/*/styles.css` — свежие (mtime 05.09 00:28), содержат фиксы ✅

---

## 2. НАЙДЕНАЯ ИСТИННАЯ ПРИЧИНА: разрыв между merge и деплоем Hermes-1

### Хронология 4 сентября
| Время | Событие | Версия |
|---|---|---|
| 00:58–01:00 | **Hermes-1: merge PR #219/#220/#221 + DEPLOY + restart** (отчёт `.ai/reports/2026-09-04-PR219-220-221-merge-deploy.md`) | **0.424** |
| 08:00–08:01 | merge PR #225 (ExtSettingsWindow фиксы) | 0.428 |
| 08:23–08:24 | merge PR #226 (CompanyEdit фиксы + контрактный тест) | 0.429 |
| 08:37 | merge PR #227 (SCSS-дополнение) | 0.430 |
| 17:58–22:16 | PR #228/#229 (протокол, тест) | 0.434 |

### Ключевой факт
**Отчётов о деплое PR #225–#229 в `.ai/reports/` НЕТ.**  
Последний задеплоенный Hermes-1 билд — **0.424 от 04.09 01:00**, то есть **до** слияния всех трёх фиксов экранов (они влиты в master только в 08:00–08:37 того же дня).

### Вывод
Исправления **есть в master и в локальном деплое** (поднят сегодня в 00:28 из master 0.434), но **там, где观察ается «старая версия» — это деплой, собранный Hermes-1 от 0.424** (прод/удалённый инстанс). Git-протокол соблюдён (merge → WAITING_FOR_HERMES), но цикл «merge → deploy → restart» для PR #225–#227 **не был завершён Hermes-1**: после мержей сборка и выкладка master не запускались.

Вторичный фактор (если смотреть локально): Vaadin кэширует `styles.css` в браузере жёстко (URL без версии) — без Ctrl+Shift+R можно видеть старый CSS даже поверх нового деплоя.

---

## 3. План корректировки

### Шаг 1 — Hermes-1: деплой актуального master (ОСНОВНОЕ ДЕЙСТВИЕ)
Передать Hermes-1 задачу:
```
Деплой master 2fc96ead (версия 0.434): сборка, restart, миграций новых нет
(PR #225–229 — только XML/SCSS/тесты), HTTP 200, smoke CompanyEdit +
ExtSettingsWindow, отчёт .ai/reports/2026-09-05-PR225-229-deploy.md.
```
Самостоятельный деплой master мне (Antigravity) запрещён протоколом — только Hermes-1.

### Шаг 2 — проверка после деплоя (evidence-based)
На задеплоенном инстансе:
```bash
# версия
curl -s <URL>/hrm/VAADIN/themes/hunttech-modern/styles.css | grep -c "company-tab-scroll"   # ≥1
# XML
# открыть CompanyEdit — вкладки скроллятся целиком, footer виден
# открыть ExtSettingsWindow — правая часть без «поехавшей» компоновки
```

### Шаг 3 — клиентский кэш
После деплоя: Ctrl+Shift+R / Incognito (Vaadin отдаёт styles.css по стабильному URL).

### Шаг 4 — незакрытый хвост ветки
В `agent/antigravity-dev` есть коммит `47b2e759` (перенос очистки устаревших контрактных тестов, версия 0.437), **не влитый в master** — нужен PR, иначе следующая ветка снова разойдётся с master по тестам.

### Шаг 5 — процессное улучшение (предложение в протокол)
Деплой должен запускаться на КАЖДУЮ пачку мержей (порог: merge PR, затрагивающий `modules/web/**`), с обязательным отчётом `.ai/reports/{date}-PR{n}-deploy.md`. Сейчас PR без deploy-отчёта = «мерж есть, кода на сервере нет» — ровно этот инцидент.

---

## 4. Резюме одним абзацем

Исправления PR #225/#226/#227 **присутствуют** и в master, и в локальном деплое (проверено diff'ами, содержимым jar и HTTP-выдачей styles.css всех 7 тем). «Старая версия» там — потому что **последний деплой Hermes-1 был сделан 04.09 в 01:00 от версии 0.424, до мержей этих PR (08:00–08:37)**, и с тех пор master не переразвёртывался. Нужен один цикл деплоя Hermes-1 текущего master (0.434) + жёсткая перезагрузка браузера.

---

## Субагенты
В исследовании участвовал только основной агент (hrm-dev): git-археология, инспекция jar, HTTP-верификация задеплоенного CSS. Субагенты (аналитик/QA/UI-UX) не требовались — задача диагностическая, кодовых правок не вносилось.