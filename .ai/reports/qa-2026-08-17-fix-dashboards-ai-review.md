# QA Report — 2026-08-17
## fix/dashboards-ai-review — фикс по OCR-ревью PR #178/#179 (amCharts watermark + кросстемовость AI-дашбордов)

PROJECT: HRM HuntTech
Checked by: QA (субагент Hermes)
Branch: fix/dashboards-ai-review (рабочая копия, правки НЕ закоммичены)
Base: master
HEAD: f08db6ed (= origin/master, ветка up to date; коммитов в ветке нет)
VERDICT: PASS
P1: 0
P2: 0
P3: 2

## Чеклист
- A. Git-состояние: PASS — ветка fix/dashboards-ai-review = origin/master f08db6ed (up to date). Незакоммиченные правки: 16 modified + 5 untracked — все в рамках фикса (7 ext-файлов тем, 5 styles.scss, 2 XML дашбордов, 2 ai-dashboard-styles.scss); untracked = ровно 5 новых ai-dashboard-styles.scss. Чужих/посторонних файлов нет. Правок в **/db/** нет (миграции отсутствуют), build.gradle не тронут. Коммит-сообщений нет (правки не закоммичены — ожидаемо, коммитит разработчик после QA).
- B. Diff-ревью: PASS — 1) во всех 7 *-ext.scss блок «Hide amCharts watermark» содержит ТОЛЬКО 8 скоупленных селекторов `…a[href*="amcharts.com"]`; grep `a[href*="amcharts"]` без `.com` по всем темам — 0 совпадений; старые широкие селекторы (`a[href*="amcharts"]`, `.amcharts-chart-div a` и т.п.) удалены во всех 7 темах. 2) В 5 styles.scss (havana, helium, hover, hunttech-modern-dark, hunttech-modern-light) добавлены `@import "com.company.hunttech/ai-dashboard-styles";` сразу после `@import "com.company.hunttech/company-editor";` и `@include ai-dashboard-styles;` сразу после `@include company-editor-theme;` — паттерн копии halo/hunttech-modern. 3) Файлы ai-dashboard-styles.scss: 6 тем (halo, havana, helium, hover, hunttech-modern, hunttech-modern-light) байт-идентичны (MD5 43a1c578752d1192180095ac019b86aa) — включая правку kpi-sub (line-clamp 2) и удаление мёртвого селектора `.v-horizontallayout-slot`; hunttech-modern-dark — тёмная версия (палитра $ht-*), см. дополнения ниже. 4) XML: admin-ai-dashboard.xml — у chartsRow1 удалены `expand="costDynamicsBox"` и `spacing="true"`, у chartsRow2 удалён `spacing="true"`, у kpiPanel удалён `spacing="true"`; user-ai-dashboard.xml — у chartsPanel удалены `expand="dynamicsChartBox"` и `spacing="true"`, у kpiPanel удалён `spacing="true"`. Других изменений в XML нет.
- C. Сборка и тесты: NOT_RUN (по заданию) — gradle не запускался (долгие прогоны; мутекс agent-gradle.sh не использовался). Статика: баланс скобок `{}` в 19 изменённых scss — без расхождений; оба XML well-formed (xml.etree); синтаксических очевидных ошибок нет; миграций нет.
- D. Документация: PASS — правок в docs/ нет; изменений UI-доков в диффе нет (проверять нечего). Замечание P3: в docs/ui вообще нет спецификаций AI-дашбордов (pre-existing пробел, вне скоупа ветки).

## Ошибки
- P1 — нет
- P2 — нет
- P3:
  1. docs/ui: нет спецификаций AI-дашбордов (admin/user AI dashboard) — пробел существовал до ветки; GLOBAL UI TRIGGER предлагает завести/актуализировать отдельной задачей.
  2. Правки не закоммичены: перед PR разработчику нужно закоммитить с русским сообщением `fix(web): …` по протоколу (bullets из фактического diff) и запушить ветку; `:app-web:buildScssThemes` (md5-канон hover) и `ScreenViewIntegrityTest` прогнать при сборке/деплое Hermes-1.

## Комментарий
Проверялся незакоммиченный diff рабочей копии (git diff против HEAD=f08db6ed) + 5 untracked-файлов (сверены по MD5 с halo-копией). Команды проверки: `git status`, `git diff --stat/--name-only`, `git diff` (XML, halo-ext, 5 styles.scss, 2 ai-dashboard-styles.scss), `grep -r 'a\[href\*="amcharts"\]'` (0), `grep -r 'href\*="amcharts'` (только скоупленные), `md5 modules/web/themes/*/com.company.hunttech/ai-dashboard-styles.scss`, python-проверка баланса скобок 19 scss, xml.etree на 2 XML. Gradle-прогоны не выполнялись по заданию (достаточно статики); при деплое Hermes-1 прогнать :app-web:buildScssThemes и ScreenViewIntegrityTest. НИЧЕГО не коммитилось, не пушилось, не менялось в рабочей копии (кроме создания этого отчёта).

## Дополнительные правки после QA (по замечаниям 2-го прогона OCR-ревью фикс-ветки, 7 замечаний)

Внесены после вердикта PASS и перепроверены контрольными grep-проверками (те же точки, что в чеклисте B):
1. `kpiPanel` в обоих XML (admin:60, user:40): удалён `spacing="true"` — двойной зазор с `padding: 0 6px` слотов KPI-грида (аналог замечания по chart-рядам). [bug-medium → исправлено]
2. Все 7 `ai-dashboard-styles.scss`: добавлено стандартное `line-clamp: 2 !important;` рядом с `-webkit-line-clamp` (fallback для не-WebKit браузеров). [maintainability-medium → исправлено]
3. Все 7 `ai-dashboard-styles.scss`: удалён мёртвый селектор `> .v-horizontallayout-slot` (не класс Vaadin 8; hbox рендерится как `.v-slot`). [maintainability-medium → исправлено]
4. `hunttech-modern-dark/com.company.hunttech/ai-dashboard-styles.scss`: создана тёмная версия mixin — палитра переведена на переменные темы (`$ht-black` фон, `$ht-gray` поверхности, `$ht-dark`/`$ht-muted` текст, акцент `$ht-red` + его rgba-вариации, `$ht-border-subtle`, `$ht-shadow`; KPI-акценты — светлые 400-уровневые тона для читаемости на тёмном; кнопки по паттерну dark-ext: primary `$ht-red`/`$ht-white`). Остальные 6 тем остаются идентичны halo-копии. [maintainability-high → исправлено]
5. НЕ исправлено (вне scope, задокументировано в .ai/reports/2026-08-17-PR178-179-merge-ocr-review.md): мёртвый `width: 25%` при `flex: 1 1 0%` (low), тотальные `!important` (low, стиль проекта).

Итог по финальному состоянию: вердикт остаётся PASS (P1: 0, P2: 0, P3: 2 без изменений).
