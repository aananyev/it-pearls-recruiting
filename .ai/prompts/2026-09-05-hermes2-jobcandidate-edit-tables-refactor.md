# ЗАДАНИЕ Hermes-2: Рефакторинг дизайна внутренних таблиц JobCandidateEdit

Дата: 2026-09-05 · Постановщик: Hermes-1 (DevOps/PM) · Приоритет: высокий
Ветка: agent/hermes2-dev (перед стартом: git fetch && git rebase origin/master)

## ОБЯЗАТЕЛЬНОЕ ПРЕДВАРИТЕЛЬНОЕ УСЛОВИЕ
Сначала заверши текущую задачу «Контракт дизайна табличных форм»
(docs/ui/ReestrBrowse_Design_Contract.md) и ЗАКОММИТЬ её в agent/hermes2-dev.
Этот контракт — нормативная база данной задачи: каждое визуальное решение
в таблицах JobCandidateEdit обязано соответствовать ему. Если контракт ещё
не готов — эту задачу НЕ начинать.

## ЦЕЛЬ
Унифицировать дизайн 5 внутренних табличных компонентов экранной формы
JobCandidateEdit в соответствии с ReestrBrowse_Design_Contract.md.
МЕНЯТЬСЯ МОЖЕТ ТОЛЬКО ДИЗАЙН ТАБЛИЦ. Всё остальное — неприкосновенно.

## ОБЪЕКТЫ РАБОТЫ
Файл: modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
(номера строк — для ориентира, сверяйся с актуальным master)

| # | id | Тип | ~Строка | Что это |
|---|----|-----|---------|---------|
| 1 | socialNetworkTable | dataGrid | 1171 | соцсети: логотип 50px, название, URL (редакт.), ссылка; editorEnabled + CRUD actions |
| 2 | lastProjectTable | table | 1267 | последние проекты: №, вакансия 300px, дата, взаимодействия, исследователь, рекрутер, кнопка просмотра; stylename no-horizontal-lines |
| 3 | suggestVacancyTable | table | 1309 | подходящие вакансии: иконка статуса 20px + название (HTML-капшн); no-horizontal-lines |
| 4 | jobCandidateIteractionListTable | dataGrid | 1373 | вкладка «Взаимодействия»: основная таблица под vacancyPicker-фильтром |
| 5 | jobCandidateCandidateCvTable | dataGrid | 1505 | вкладка CV: bodyRowHeight 55px, HTML-капшны/описания |

## ЖЁСТКИЕ ГРАНИЦЫ (нарушение = отклонение PR на входе)
1. БИЗНЕС-ЛОГИКА ЗАПРЕЩЕНА: не трогать JobCandidateEdit.java, генераторы
   колонок (lastIteractionCount, lastInteractionGeneratorColumn,
   whoIsResearcherGeneratorColumn, whoIsRecruterGeneratorColumn,
   addInteractionsViewButton), actions (create/edit/remove), dataContainer-
   и property-привязки, id элементов, фильтры, запросы, экшены.
2. КОМПОНОВКА НЕТАБЛИЧНЫХ ЭЛЕМЕНТОВ ЗАПРЕЩЕНА: sidebar, header, навигация,
   аккордеоны, карточки, toolbar, вкладки, layout-иерархия (vbox/hbox,
   expandRatio, размеры контейнеров). Таблицы правятся только внутри себя:
   stylename таблицы и её колонок, SCSS-правила для этих классов, визуальное
   оформление шапки/строк/состояний.
3. ЛЕНТУ КОММЕНТАРИЕВ НЕ ТРОГАТЬ ВОВСЕ (scrollBox jobCandidateCommentsScroll,
   XML ~1612): это осознанный уход от dataGrid (Vaadin Grid не держит
   авто-высоту строк — см. комментарий в XML). Не «возвращать к таблице».
4. job-candidate-editor.scss — ОБЩИЙ с эталонными Reestr-экранами. Правки
   ТОЛЬКО аддитивные: новые классы или селекторы, скоупнутые через
   .job-candidate-editor (или новый stylename таблиц). Существующие правила
   .job-candidate-table (строки 807–840+) и candidate-browse-grid НЕ
   переопределять и не ломать — их потребляют эталонные browse-экраны.
5. Защищённые формы Antigravity (CompanyEdit, ExtSettingsWindow, ExtUserEdit)
   и их SCSS — не трогать.
6. messages.properties — не трогать (капшны чисто визуальные менять через
   существующие ключи; если нужен новый ключ — доложить отдельно).

## ТРЕБОВАНИЯ К РЕЗУЛЬТАТУ (по контракту)
- Единый стиль шапок всех 5 таблиц (капшны: вес/регистр/цвет/высота — как в
  контракте);
- Единая базовая высота и вертикальное выравнивание строк (38px-сетка где
  применимо; у CV-таблицы сохранить 55px — HTML-контент);
- hover/selected-состояния строк по образцу контракта;
- word-break в текстовых колонках (длинные URL соцсетей и названия вакансий
  не должны распираять сетку);
- колонки-кнопки (просмотр взаимодействий) — визуальный стиль candidate-btn
  ДОПУСТИМ только если генератор уже читает stylename из XML/контекста без
  правки Java; иначе оставить как есть и доложить;
- no-horizontal-lines сохранить там, где стоит;
- всё должно работать в hunttech-modern, hunttech-modern-dark, havana (+halo
  как база) — проверить компиляцию тем.

## ПОРЯДОК РАБОТЫ
1. Субагент-аналитик: «до»-описание 5 таблиц + таблица расхождений с
   ReestrBrowse_Design_Contract.md. Артефакт .ai/reports/analysis-jce-tables-before.md.
2. Субагент UI/UX designer: спецификация унификации «класс → правило
   контракта → тема», без кода. Артефакт .ai/reports/ui-design-jce-tables.md.
3. Реализация: XML-stylename'ы + аддитивный SCSS. Каждый логический кусок —
   отдельный коммит (рус. сообщение) + push origin HEAD:agent/hermes2-dev.
4. Тесты: bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test
   --tests com.company.hunttech.core.ScreenViewIntegrityTest --no-daemon —
   зелёный обязателен.
5. Визуальная приёмка: start-app.sh --branch "$PWD", скриншоты «до/после»
   всех 5 таблиц (вкладки Профессия/Взаимодействия/CV), ПОСЛЕ — немедленно
   bash ../hunttech_recruiting/scripts/start-app.sh (вернуть master).
6. Субагент-QA: построчный diff-ревью — ни один generator/action/
   dataContainer/layout вне таблиц не изменён; соответствие контракту;
   вердикт PASS в .ai/reports/qa-jce-tables.md. Без PASS PR не создавать.
7. ocr review --audience agent (скилл open-code-review) — результат в PR.
8. PR base=master, метка WAITING_FOR_HERMES: список 5 таблиц, что
   унифицировано, скриншоты до/после, evidence QA+OCR, явная ссылка на
   ReestrBrowse_Design_Contract.md как норматив.
9. TG-отчёт: hermes send -t telegram:272980897 с секцией '## Субагенты'.

## ЭСКАЛАЦИЯ
Если унификация требует выхода за границы (правка Java-генератора, трогает
компоновку вне таблицы, конфликт с классами эталонных реестров) — НЕ делать,
зафиксировать в отчёте и спросить пользователя.

## ПРИЁМКА (проверяет Hermes-1)
- diff содержит ТОЛЬКО: job-candidate-edit.xml (stylename-атрибуты таблиц),
  аддитивные SCSS-блоки, .ai/reports/*, docs;
- ScreenViewIntegrityTest зелёный;
- 5 таблиц выглядят единообразно по контракту во всех 3 основных темах;
- эталонные Reestr-экраны не изменились визуально (регресс-скриншот);
- QA PASS + OCR PASS в PR.
