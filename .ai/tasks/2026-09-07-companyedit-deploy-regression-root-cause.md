# CompanyEdit: заметки для продолжения диагностики

Дата: 2026-09-07

## Запрос

После исправлений компоновки первой вкладки CompanyEdit Antigravity отправляет PR, но после обработки Hermes-1 команда снова видит кривую форму и считает, что в деплой попала старая версия.

Сегодня в master были откаты:

- `8478ecd6` — финальный откат CompanyEdit к `eb795a67` + `8f425f0f`;
- `ed5523b1` — восстановление CompanyEdit к `eb795a67`;
- `8f425f0f` — структурное исправление компоновки вкладок и контрактная защита;
- `eb795a67` — удаление лишней карточки контактов.

## Уже подтверждено по локальному Git

Репозиторий: `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`.

- `origin/master` и локальный `master` сейчас указывают на `7f916f6c02f4d36100e76a4f3def2abdc0243e08` — merge PR #246 из `agent/antigravity-dev`.
- `8478ecd6`, `ed5523b1`, `8f425f0f`, `eb795a67` находятся в истории текущего master.
- PR #245: коммит `14eb50fcc86af15ecdb256e718275ca83c5f4493`, исправление первой вкладки.
- На ветке Antigravity перед PR #246 были коммиты `9c1ab7fd4`, `b85fe80a2`, `5a3e16146`; итоговый merge-коммит ветки — `a70f288d9`.
- PR #246 в master — merge-коммит `7f916f6c0` с родителями `eee8a9fc3` и `a70f288d9`.
- В worktree Antigravity: `/Users/alekseyananyev/StudioProjects/hrm-antigravity`, HEAD `a70f288d9`, ветка `agent/antigravity-dev`.

Вывод: локальная история не подтверждает потерю исправлений при merge. Исправления Antigravity действительно были опубликованы в master.

## Уже подтверждено по артефакту деплоя

После последнего деплоя:

- `deploy/tomcat/webapps/hrm/WEB-INF/lib/app-web-0.487-SNAPSHOT.jar` существует;
- XML внутри JAR содержит новую структуру CompanyEdit: `company-tab-scroll`, `company-main-tab-top-checks`, `box.expandRatio="0"` у чекбоксов, spacer;
- SHA-256 XML в исходнике и внутри JAR совпадает:
  `20b2af7301348c8fa1086b55288b49f2938bde9c7bd203f5ee4a12caefacac1b`;
- время JAR — 2026-09-07 17:04:26;
- compiled `styles.css` тем содержит новые правила CompanyEdit;
- `local-deploy.log` фиксирует последний деплой 2026-09-07 17:01:30–17:07:30 с HTTP 200.

Вывод: для текущего локального Tomcat нет доказательств, что Hermes-1 собрал старый XML. На уровне source → JAR текущая версия синхронизирована.

## Уточнение после проверки Git/PR (без CDP)

По запросу команды проверка продолжена только по Git, без новой CDP-проверки.

- Дерево `a70f288d9` (HEAD PR #246) и дерево `7f916f6c0` (merge PR #246 в master)
  одинаковы: merge не выбросил изменения из ветки.
- `git diff origin/master...origin/agent/antigravity-dev` для CompanyEdit в PR #246
  содержит только семь SCSS-файлов; `company-edit.xml` в этом PR не менялся.
- Значит, PR #246 не мог вернуть XML-исправление, которого не было в его diff.
  XML текущего `master` уже пришёл из PR #245 (`14eb50fcc`), а rollback-состояние
  `8478ecd6` не было чистым равенством `eb795a67 + 8f425f0f`.
- Корень проблемы передачи: команда считала историю коммитов доказательством
  содержания PR. Но для GitHub PR важны `base SHA`, точный `head SHA`, `diff base...head`
  и фактическое дерево merge commit.

Таким образом, подтверждённой потери кода при merge PR #246 нет. Есть ошибка
формирования базового состояния и отсутствие проверки, что заявленный файл/фикс
действительно входит в diff PR.

## Подтверждение Hermes-1

Отчёт Hermes-1: `PASSED_VERIFICATION` для PR #246.

- `BASE_SHA`: `eee8a9fc3822f224cdde98eeeb4e09e96727f4b7`.
- `PR_HEAD`: `a70f288d97a6635ad6f3c974381706ff997b72e7`.
- `MERGE_SHA`: `7f916f6c02f4d36100e76a4f3def2abdc0243e08`.
- `merge-tree` совпал с деревом merge commit; конфликтов и потери кода нет.
- Сборка выполнена из чистого `master`, HTTP 200.
- SHA исходного `company-edit.xml` совпал с XML внутри `app-web-0.487.jar`.

Критичное уточнение: PR #246 содержал документацию и SCSS ×7, но не
`company-edit.xml`. Поэтому он подтверждает корректность передачи и деплоя
checkbox-фикса, но не является исправлением XML-компоновки CompanyEdit.

Практическая причина сообщения «доработки не видны» — проверка на порту `8081`,
где работала старая песочница с JAR 0.485. Основной контур `8080` содержит merge
PR #246. Дополнительный фактор — кэш `styles.css` на один час; после деплоя нужна
новая вкладка или жёсткая перезагрузка.

Правки ChatGPT из `/private/tmp/it-pearls-recruiting-audit` в Git не передавались
и отсутствуют в репозитории. Для них нужен отдельный topic-branch от актуального
`origin/master` после commit `ab62c206` и отдельный PR.

Обязательный handoff-процесс сохранён отдельно:
`.ai/instructions/company-edit-pr-handoff-2026-09-07.md`.

## Что ещё не доказано

Не проверено через новую авторизованную Vaadin-сессию:

1. Какой фактический DOM и геометрия CompanyEdit на первой вкладке.
2. Какой compiled CSS реально использует браузер; текущая сессия загружает `halo/styles.css`.
3. Не показывает ли браузер старый `styles.css` из cache или мёртвую Vaadin-сессию после рестарта.
4. Не открывается ли другая форма/старый screen route вместо `hunttech_Company.edit`.
5. Не ломает ли сам текущий SCSS компоновку даже при корректно доставленном новом XML.

## Подозрительная текущая точка в SCSS

Во всех семи темах в `company-editor.scss` сначала есть правило для чекбоксов:

```scss
.edit-workspace-content .company-main-tab-top-checks > .v-slot {
  flex: 0 0 auto !important;
}
```

Но позднее, внутри `.company-main-tab`, есть более позднее правило:

```scss
.company-main-tab-top-checks > .v-slot,
.company-main-tab-top-checks > .v-expand > .v-slot {
  flex-basis: 220px !important;
}
```

Оно частично переопределяет заявленное поведение «чекбоксы без растяжения». Это ещё не доказанная первопричина; нужно подтвердить computed styles и `getBoundingClientRect()` в браузере.

Также текущий partial содержит много широких CSS-правил, которые принудительно переводят Vaadin `hbox`/`v-expand`/`v-slot` в flex-flow и перебивают inline-геометрию `box.expandRatio`. Нельзя продолжать чинить это новыми CSS-патчами без измерений.

## Правильная следующая проверка runtime

1. Создать новую вкладку браузера после последнего деплоя, не использовать старую вкладку после рестарта Tomcat.
2. До открытия CompanyEdit отключить browser cache / сделать hard reload; тему определить по фактическому `styles.css`.
3. Открыть именно `CompanyEdit`, первую вкладку, на широком и узком viewport.
4. Снять для каждого элемента и его Vaadin-слота:
   - `getBoundingClientRect()`;
   - `getComputedStyle()` для `width`, `min-width`, `max-width`, `display`, `position`, `left`, `top`, `flex`, `flex-basis`, `overflow`;
   - выход за границы рабочей области;
   - пересечения прямоугольников чекбоксов, полей названия, группы и директора.
5. Сверить CSSOM с исходником и compiled `styles.css` именно активной темы.
6. Только после этого решить: это browser/Vaadin cache, неверный runtime route, CSS-регрессия или действительно неверный artifact/deploy.

## Важное правило доказательства деплоя

Каждый деплой CompanyEdit должен логировать и проверять одну цепочку:

`master SHA → SHA PR head → merge SHA → source XML/SCSS SHA → JAR/WAR SHA → exploded Tomcat file SHA → runtime CSS/theme → fresh browser DOM`.

HTTP 200 недостаточен. Для CompanyEdit обязательны:

- проверка `git rev-parse HEAD` перед сборкой;
- проверка `git status` и ветки `master`;
- проверка merged PR head SHA;
- проверка XML внутри `app-web` JAR;
- проверка compiled CSS активной темы;
- полный restart/очистка Tomcat cache для XML/SCSS-изменений;
- fresh browser session + CDP smoke первой вкладки;
- отчёт с этими SHA и скриншотом/геометрией.

## Ограничения процесса

- Hermes-1 — единственный merge/deploy агент.
- Antigravity — владелец CompanyEdit и автор исправлений.
- Не редактировать CompanyEdit напрямую в Hermes-1 при наличии живой ветки Antigravity.
- Не деплоить рабочую копию Antigravity в общую среду как доказательство master-деплоя.
- Не считать `HTTP 200` доказательством, что новая форма реально отрисована.
- После merge каждого PR заново проверять очередь PR и итоговый `origin/master`.
- Для runtime-проверки не использовать старую Vaadin-вкладку после рестарта.

## Единый Git/PR-промпт агентам

Задача: устранить регрессию CompanyEdit и доказать, какая версия реально работает.

Antigravity:

- работать в свежей topic-ветке от актуального `origin/master`, а не в долгоживущей
  ветке с историей merge старых PR;
- перед изменениями зафиксировать `BASE_SHA`, `HEAD_SHA`, `git status` и список файлов;
- указать в PR точный baseline и разрешённые изменения;
- убедиться, что каждый файл, который заявлен как исправленный, реально присутствует
  в `git diff BASE_SHA...HEAD_SHA`;
- не копировать файлы из старых worktree и не делать полный rollback смешанного PR;
- передать Hermes-1 `READY_FOR_HERMES` с точным `VERIFIED_HEAD`, тестами и OCR.

Hermes-1:

- сверить GitHub PR `headRefOid` с локальным `origin/<head>`;
- проверить `git diff BASE_SHA...PR_HEAD`, `git diff --check` и `git merge-tree --write-tree`;
- если нужного файла нет в diff или есть конфликт protected-файла — остановить merge;
- после merge проверить `MERGE_SHA`, ancestry PR_HEAD и итоговое дерево merge commit;
- собирать только из чистого `master`, не из Antigravity worktree;
- после сборки сверить SHA исходного файла с файлом внутри JAR/WAR;
- в отчёте указать `BASE_SHA`, `PR_HEAD`, `MERGE_SHA`, artifact SHA и verdict;
- при любом несовпадении остановиться и не объявлять deploy успешным.

QA/проверяющий:

- проверять не только контрактные тесты, но и реальный DOM первой вкладки;
- принимать только если нет пересечений, выхода за workspace и скрытых полей на двух viewport;
- отдельно проверять, что показан `CompanyEdit`, а не legacy/другая форма;
- при stale UI сначала открыть новую вкладку и очистить cache, затем повторять smoke.

## Статус

Продуктовый код CompanyEdit этим расследованием не менялся. Добавлена процессная
инструкция для handoff/merge/deploy: `.ai/instructions/company-edit-pr-handoff-2026-09-07.md`.
Для восстановления самого экрана нужен отдельный узкий PR Antigravity с явным baseline
и проверкой итогового merge result; Hermes-1 не должен «чинить» экран при разрешении
конфликтов.
