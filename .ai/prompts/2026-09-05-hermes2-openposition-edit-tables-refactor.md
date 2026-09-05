# ЗАДАНИЕ Hermes-2: Оптимизация дизайна внутренних таблиц экранной формы OpenPositionEdit

Дата: 2026-09-05 · Постановщик: Hermes-1 (DevOps/PM) · Приоритет: высокий
Ветка: agent/hermes2-dev (перед стартом: git fetch && git rebase origin/master)

## ШАГ 0 — ОЧИСТКА КОНТЕКСТА (обязателен ПЕРЕД началом работы)
Эта задача стартует после другой задачи (таблицы JobCandidateEdit). Не тащи
контекст предыдущей работы в новую:
1. Убедись, что все субагент-делегирования предыдущей задачи завершены
   (иначе дождись их итогов — висячие делегаты сгорят при очистке).
2. Доведи до конца текущий коммит/пуш предыдущей задачи (дерево git чистое,
   всё в agent/hermes2-dev) — незакоммиченное теряется.
3. Начни НОВУЮ сессию: /new (или перезапусти лаунчер hrm-hermes2).
   /compact не годится — он оставляет сжатый хвост предыдущей задачи.
4. Первым делом в новой сессии прочитай ЭТОТ файл задания целиком и
   ReestrBrowse_Design_Contract.md из docs/ui/ — работай только по ним,
   без опоры на память о прошлой задаче.
5. git fetch && git rebase origin/master (подхватить уточнения контракта,
   сделанные в первой задаче).

## НОРМАТИВНАЯ БАЗА (обязательна к применению)
Твой же контракт: docs/ui/ReestrBrowse_Design_Contract.md (коммит 51fea26b в
agent/hermes2-dev; после его мержа — из master). Каждое визуальное решение
ниже проверяется по разделам 3 (табличная часть), 4 (кнопки/иконки),
7 (темизация), 8 (запреты), 9 (чеклист QA) контракта. Отступления — только
с явным обоснованием в PR (формат: пункт контракта → почему неприменим в
Edit-форме).

## ЦЕЛЬ
Привести дизайн 4 внутренних таблиц OpenPositionEdit к контракту.
МЕНЯЕТСЯ ТОЛЬКО ДИЗАЙН ТАБЛИЦ (stylename'ы таблиц/колонок, SCSS-классы
таблиц, визуальные состояния). Бизнес-логика и компоновка остального —
неприкосновенны.

## ОБЪЕКТЫ РАБОТЫ
Файл: modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml
(1898 строк; номера — ориентир, сверяйся с актуальной базой)

| # | id | Тип | ~Строка | Что это / проблемы |
|---|----|-----|---------|--------------------|
| 1 | laborAgreementDataGrid | dataGrid | 1194 | трудовые соглашения: perhaps (editable), company, laborAgreementType; editorEnabled + CRUD actions + buttonsPanel. Класс variant5 |
| 2 | someFilesTable | table | 1526 | файлы: описание, тип, размер, владелец. **height=300px фиксирован** — против контрактной сетки; buttonsPanel + actions add/create/edit/remove. Класс variant5 |
| 3 | openPositionSkillsListTable | treeDataGrid | 1721 | дерево навыков: иерархия skillTree/skillName, componentRenderer (лого, иконка комментария), wikiPage. Класс variant5 |
| 4 | openPostionNewsDataGrid | dataGrid | 1781 | новости: dateNews 200px, subject, candidates, author 250px; create/remove + кастомная кнопка addOpenPositionNewsButton. Класс variant5 |

SCSS-база: modules/web/themes/halo/com.company.hunttech/open-position-editor.scss,
блок .open-position-editor-table-variant5 (строка 1346+; синхронизирован во
всех 7 темах — проверено). Фрагмент some-files-open-position-edit.xml
(screens/somefiles/) — если содержит таблицы этой формы, тоже в объёме.

## КОНКРЕТНЫЕ ТРЕБОВАНИЯ (по контракту)
1. someFilesTable: убрать фиксированный height=300px в пользу контрактной
   схемы высоты (flex/expand контейнера карточки — но см. границу 2: если
   снятие высоты меняет компоновку карточки, зафиксировать и эскалировать,
   НЕ продавливать молча).
2. Единые шапки колонок 4 таблиц по variant5/контракту (высота 42px, веса,
   цвета — уже в SCSS; проверить фактический рендер, добить расхождения).
3. Сетка строк 38px (п.3 контракта) — проверить плотность variant5, привести
   отставшие таблицы; у treeDataGrid сохранить читаемость иерархии
   (expander-отступы не ломать).
4. word-break/ellipsis в текстовых колонках (subject новостей, описание
   файлов, specialisation навыков) — длинные значения не распирают сетку.
5. buttonsPanel'и: стиль кнопок по п.4 контракта (candidate-btn-паттерн:
   иконка+текст, 40px/600/4px) — только через stylename/SCSS, без правки
   action-привязок.
6. Состояния hover/selected/readonly-редактируемых ячеек (у
   laborAgreementDataGrid editorEnabled=true) — единообразно по контракту.
7. Темизация: все правки работают в hunttech-modern, hunttech-modern-dark,
   havana (+halo база); синхронизация SCSS между темами md5-идентичная
   (правило проекта), компиляцию тем проверить.

## ЖЁСТКИЕ ГРАНИЦЫ (нарушение = отклонение PR)
1. НЕ трогать: OpenPositionEdit.java, генераторы/обработчики (invoke,
   addOpenPositionNewsButton, rescanJobDescription), actions
   (create/edit/remove/add), dataContainer/property-привязки, id элементов,
   hierarchyProperty/hierarchyColumn, запросы, lazy-загрузку LOB-вкладок
   (План собеседования/Карта поиска/Чекл-лист — их richTextArea не трогать).
2. НЕ трогать компоновку нетабличных элементов: sidebar, header, toolbar,
   tabSheet-структуру вкладок (последние фиксы c98c5d29/033b392b —
   @Named-инъекции accordion/tabSheet не двигать), аккордеоны, карточки,
   layout-иерархию.
3. open-position-editor.scss — правки только внутри блока
   .open-position-editor-table-variant5 и НОВЫХ классов; существующие
   правила других variant*/секций не переопределять.
4. Защищённые формы Antigravity (CompanyEdit, ExtSettingsWindow, ExtUserEdit)
   и job-candidate-editor.scss — не трогать.
5. messages.properties/mainMsg — не трогать.
6. Preview-XML (open-position-edit-preview.xml) — синхронизировать только
   если меняются stylename'ы (стабы должны совпадать).

## ПОРЯДОК РАБОТЫ
1. Субагент-аналитик: «до»-описание 4 таблиц + таблица расхождений с
   ReestrBrowse_Design_Contract.md (пункт за пунктом).
   Артефакт .ai/reports/analysis-ope-tables-before.md.
2. Субагент UI/UX designer: спецификация «класс → правило контракта → тема»,
   без кода. Артефакт .ai/reports/ui-design-ope-tables.md.
3. Реализация: XML-stylename'ы + SCSS (variant5/новые классы). Каждый
   логический кусок — отдельный коммит (рус.) + push HEAD:agent/hermes2-dev.
4. Тесты: bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test
   --tests com.company.hunttech.core.ScreenViewIntegrityTest --no-daemon —
   зелёный. Если есть контрактный тест по variant5/таблицам OPE — тоже.
5. Визуальная приёмка: start-app.sh --branch "$PWD"; скриншоты до/после
   4 таблиц × 3 основные темы; регресс-скриншот JobCandidateReestrBrowse
   (общие SCSS не сломаны); ПОСЛЕ — немедленно вернуть master
   (start-app.sh без флагов).
6. Субагент-QA: построчный diff-ревью (ни один action/generator/
   dataContainer/layout вне таблиц не изменён), соответствие контракту,
   PASS в .ai/reports/qa-ope-tables.md. Без PASS PR не создавать.
7. ocr review --audience agent — результат в PR.
8. PR base=master, метка WAITING_FOR_HERMES: ссылка на контракт как
   норматив, таблица «4 объекта → что изменено», скриншоты до/после,
   evidence QA+OCR.
9. TG-отчёт: hermes send -t telegram:272980897 с секцией '## Субагенты'.

## ПОСЛЕДОВАТЕЛЬНОСТЬ С ДРУГИМИ ЗАДАЧАМИ
Эта задача — ВТОРАЯ после JobCandidateEdit-таблиц
(.ai/prompts/2026-09-05-hermes2-jobcandidate-edit-tables-refactor.md).
Сначала — та, потом эта: обе используют один контракт, вторая проверяет
универсальность правил на другом экране. Если в ходе JobCandidateEdit
контракт уточнялся — применять уточнённую версию.

## ЭСКАЛАЦИЯ
Выход за границы (нужна правка Java/компоновки/конфликт с классами других
экранов) — НЕ делать, зафиксировать в отчёте, спросить пользователя.

## ПРИЁМКА (проверяет Hermes-1)
- diff: только open-position-edit.xml (stylename'ы/height таблиц),
  open-position-edit-preview.xml (стабы), блок variant5 + новые классы в
  SCSS ×7 тем, .ai/reports/*;
- ScreenViewIntegrityTest зелёный;
- 4 таблицы единообразны по контракту в 3 темах; someFilesTable без
  фиксированных 300px (или эскалировано);
- JobCandidateReestrBrowse/OpenPositionReestrBrowse без визуальных изменений;
- QA PASS + OCR PASS в PR.
