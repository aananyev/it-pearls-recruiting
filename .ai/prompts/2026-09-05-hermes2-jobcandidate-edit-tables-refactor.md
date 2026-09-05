# ЗАДАНИЕ Hermes-2: Рефакторинг дизайна внутренних таблиц JobCandidateEdit

Дата: 2026-09-05 (ред. 2 — норматив готов) · Постановщик: Hermes-1 (DevOps/PM)
Приоритет: высокий · Ветка: agent/hermes2-dev
(перед стартом: git fetch && git rebase origin/master)

## НОРМАТИВНАЯ БАЗА (обязательна к применению)
Твой контракт: docs/ui/ReestrBrowse_Design_Contract.md (коммит 51fea26b в
agent/hermes2-dev). Каждое визуальное решение проверяется по разделам 3
(табличная часть), 4 (кнопки/иконки), 7 (темизация), 8 (запреты), 9 (чеклист
QA). Отступление — только с обоснованием в PR: «пункт контракта → почему
неприменим в Edit-форме».

## ЦЕЛЬ
Унифицировать дизайн 5 внутренних табличных компонентов JobCandidateEdit по
контракту. МЕНЯЕТСЯ ТОЛЬКО ДИЗАЙН ТАБЛИЦ: stylename'ы таблиц/колонок,
SCSS-классы таблиц, визуальные состояния. Бизнес-логика и компоновка
остального — неприкосновенны.

## ОБЪЕКТЫ РАБОТЫ
Файл: modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
(1732 строки; номера — ориентир, сверяйся с актуальной базой)

| # | id | Тип | ~Строка | Что это / проблемы |
|---|----|-----|---------|--------------------|
| 1 | socialNetworkTable | dataGrid | 1171 | соцсети: лого 50px (componentRenderer), название, URL (editable), ссылка; editorEnabled + CRUD actions. Класс job-candidate-table |
| 2 | lastProjectTable | table | 1267 | проекты: №, вакансия 300px, дата, взаимодействия, исследователь, рекрутер, кнопка просмотра (генераторы!). no-horizontal-lines job-candidate-table |
| 3 | suggestVacancyTable | table | 1309 | подходящие вакансии: иконка статуса 20px + название (HTML-капшн). no-horizontal-lines job-candidate-table |
| 4 | jobCandidateIteractionListTable | dataGrid | 1373 | вкладка «Взаимодействия» под vacancyPicker-фильтром. **bodyRowHeight=36px — против контрактных 38px**; reorderingAllowed=true, textSelectionEnabled=false — НЕ трогать (это поведение, не дизайн). job-candidate-table |
| 5 | jobCandidateCandidateCvTable | dataGrid | 1505 | вкладка CV: bodyRowHeight 55px (HTML-контент — сохранить, обосновать в PR как отступление от 38px по п.3 контракта), HTML-капшны/описания. job-candidate-table |

SCSS-база: modules/web/themes/halo/com.company.hunttech/job-candidate-editor.scss
— класс .job-candidate-table (блок ~807–840+: header, hover, строки),
синхронизирован во всех 7 темах (проверено Hermes-1: 13 упоминаний ×7).
Общая с эталонными Reestr-экранами!

## КОНКРЕТНЫЕ ТРЕБОВАНИЯ (по контракту)
1. Единые шапки 5 таблиц (42px, веса/цвета капшнов — сверить фактический
   рендер с разделом 3 контракта, добить расхождения).
2. Сетка строк 38px: привести bodyRowHeight 36px (таблица 4) к контрактной;
   у CV-таблицы 55px остаётся (HTML-контент) — обосновать отступление.
3. hover/selected-состояния по образцу контракта (job-candidate-editor.scss
   строки 839+ как база).
4. word-break в текстовых колонках: длинные URL соцсетей и названия вакансий
   не должны распираять сетку.
5. Колонка-кнопка lastProjectTable (addInteractionsViewButton) — стиль
   candidate-btn ДОПУСТИМ только если генератор уже читает stylename без
   правки Java; иначе оставить и доложить.
6. no-horizontal-lines сохранить (часть эталонного вида).
7. Темизация: правки работают в hunttech-modern, hunttech-modern-dark,
   havana (+halo база); SCSS синхронизируется между всеми 7 темами
   md5-идентично; компиляцию тем проверить.

## ЖЁСТКИЕ ГРАНИЦЫ (нарушение = отклонение PR на входе)
1. БИЗНЕС-ЛОГИКА ЗАПРЕЩЕНА: JobCandidateEdit.java, генераторы колонок
   (lastIteractionCount, lastInteractionGeneratorColumn,
   whoIsResearcherGeneratorColumn, whoIsRecruterGeneratorColumn,
   addInteractionsViewButton), actions (create/edit/remove),
   dataContainer/property-привязки, id элементов, фильтры, запросы,
   reorderingAllowed/textSelectionEnabled/contextMenuEnabled (поведение).
2. КОМПОНОВКА НЕТАБЛИЧНЫХ ЭЛЕМЕНТОВ ЗАПРЕЩЕНА: sidebar 312px, header,
   навигация, аккордеоны, карточки, toolbar, вкладки, layout-иерархия
   (vbox/hbox, expandRatio, размеры контейнеров), режим диалога 1200×750
   (строка 257).
3. ЛЕНТУ КОММЕНТАРИЕВ НЕ ТРОГАТЬ ВОВСЕ (scrollBox jobCandidateCommentsScroll,
   XML ~1612): осознанный уход от dataGrid — Vaadin Grid не держит
   авто-высоту пузырей (комментарий в XML). Не «возвращать к таблице».
4. job-candidate-editor.scss — ОБЩИЙ с эталонными Reestr-экранами. Правки
   ТОЛЬКО аддитивные: новые классы или селекторы, скоупнутые через
   .job-candidate-editor / stylename таблиц. Существующие правила
   .job-candidate-table и candidate-browse-grid НЕ переопределять — их
   потребляют JobCandidateReestr/OpenPositionReestr.
5. Защищённые формы (CompanyEdit, ExtSettingsWindow, ExtUserEdit) и их SCSS —
   не трогать.
6. messages.properties — не трогать.

## ПОРЯДОК РАБОТЫ
1. Субагент-аналитик: «до»-описание 5 таблиц + таблица расхождений с
   контрактом (пункт за пунктом). Артефакт
   .ai/reports/analysis-jce-tables-before.md.
2. Субагент UI/UX designer: спецификация «класс → правило контракта → тема»,
   без кода. Артефакт .ai/reports/ui-design-jce-tables.md.
3. Реализация: XML-stylename'ы + аддитивный SCSS. Каждый логический кусок —
   отдельный коммит (рус. сообщение) + push origin HEAD:agent/hermes2-dev.
4. Тесты: bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test
   --tests com.company.hunttech.core.ScreenViewIntegrityTest --no-daemon —
   зелёный обязателен.
5. Визуальная приёмка: start-app.sh --branch "$PWD"; скриншоты до/после
   5 таблиц (вкладки Профессия/Взаимодействия/CV) × 3 основные темы;
   РЕГРЕСС-скриншот JobCandidateReestrBrowse (общий SCSS не сломан);
   ПОСЛЕ — немедленно bash ../hunttech_recruiting/scripts/start-app.sh
   (вернуть master).
6. Субагент-QA: построчный diff-ревью — ни один generator/action/
   dataContainer/layout вне таблиц не изменён; соответствие контракту;
   PASS в .ai/reports/qa-jce-tables.md. Без PASS PR не создавать.
7. ocr review --audience agent (скилл open-code-review) — результат в PR.
8. PR base=master, метка WAITING_FOR_HERMES: ссылка на контракт как норматив,
   таблица «5 объектов → что изменено», скриншоты до/после, evidence QA+OCR.
9. TG-отчёт: hermes send -t telegram:272980897 с секцией '## Субагенты'.

## ПОСЛЕДОВАТЕЛЬНОСТЬ С ДРУГИМИ ЗАДАЧАМИ
Эта задача — ПЕРВАЯ. За ней: OpenPositionEdit-таблицы
(.ai/prompts/2026-09-05-hermes2-openposition-edit-tables-refactor.md) —
вторая проверяет универсальность контракта на другом экране. Контракт
уточняется по ходу первой задачи — правки в контракт коммитить отдельными
коммитами docs и применять актуальную версию.

## ЭСКАЛАЦИЯ
Если унификация требует выхода за границы (правка Java-генератора, трогает
компоновку вне таблицы, конфликт с классами эталонных реестров) — НЕ делать,
зафиксировать в отчёте и спросить пользователя.

## ПРИЁМКА (проверяет Hermes-1)
- diff содержит ТОЛЬКО: job-candidate-edit.xml (stylename/bodyRowHeight
  таблиц), аддитивные SCSS-блоки job-candidate-editor.scss ×7 тем,
  .ai/reports/*, docs (уточнения контракта);
- ScreenViewIntegrityTest зелёный;
- 5 таблиц единообразны по контракту в 3 основных темах; CV 55px и
  candidate-btn-эскалация обоснованы в PR;
- JobCandidateReestrBrowse/OpenPositionReestrBrowse без визуальных изменений
  (регресс-скриншот в PR);
- QA PASS + OCR PASS в PR.
