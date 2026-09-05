# АНАЛИЗ ТЕКУЩЕГО СОСТОЯНИЯ 5 ВНУТРЕННИХ ТАБЛИЦ JobCandidateEdit
Сравнение с контрактом ReestrBrowse_Design_Contract.md

## Обзор 5 таблиц

| # | id | Тип | Строка | stylename | Проблемы |
|---|----|-----|--------|-----------|----------|
| 1 | socialNetworkTable | dataGrid | 1171 | job-candidate-table | editorEnabled=true, CRUD actions, componentRenderer для логотипа и ссылки |
| 2 | lastProjectTable | table | 1267 | no-horizontal-lines job-candidate-table | Генераторы колонок, no-horizontal-lines |
| 3 | suggestVacancyTable | table | 1309 | no-horizontal-lines job-candidate-table | captionAsHtml=true, иконка статуса 20px |
| 4 | jobCandidateIteractionListTable | dataGrid | 1373 | job-candidate-table | bodyRowHeight=36px (против контрактных 38px), reorderingAllowed=true, textSelectionEnabled=false |
| 5 | jobCandidateCandidateCvTable | dataGrid | 1505 | job-candidate-table | bodyRowHeight=55px (HTML-контент — сохранить как обоснованное отступление), captionAsHtml/descriptionAsHtml |

## Детальный анализ каждой таблицы

### 1. socialNetworkTable (строка 1171)
**Текущее состояние:**
- stylename: `job-candidate-table`
- editorEnabled: true
- height: 100%
- width: 100%
- columns: socialNetworkLogoColumn (50px, componentRenderer), networkName (max 200px), networkURLS (editable), linkToWeb (max 150px, componentRenderer)
- actions: create, edit, remove
- dataContainer: jobCandidateSocialNetworksDc

**Требования контракта (п.3):**
- Таблица должна иметь stylename=`borderless grid candidate-browse-grid`
- Строка 38px + word-break
- Колонки: одна резиновая с expandRatio, фиксированные ширины кратны смыслу
- Действия: стандартные edit/refresh (+create/remove где уместно)
- word-break в текстовых колонках (URL соцсетей не должны распирать сетку)

**Расхождения:**
1. stylename: `job-candidate-table` вместо `borderless grid candidate-browse-grid`
2. Нет word-break для networkURLS (длинные URL могут распирать сетку)
3. Высота строки не задана явно (зависит от .job-candidate-table SCSS)

### 2. lastProjectTable (строка 1267)
**Текущее состояние:**
- stylename: `no-horizontal-lines job-candidate-table`
- editable: false
- contextMenuEnabled: false
- width: 100%, height: 100%
- columns: number (30px, generator), vacancy (300px, maxTextLength=40), max (70px), lastInteraction (200px, maxTextLength=40, generator), researcher (100px, generator), recruter (100px, generator), idViewIteractionsButton (100px, generator)
- dataContainer: lastProjectDc (KeyValueCollection)

**Требования контракта:**
- stylename=`borderless grid candidate-browse-grid`
- Строка 38px + word-break
- no-horizontal-lines — сохранить (часть эталонного вида, п.6 задачи)
- Колонка-кнопка addInteractionsViewButton — стиль candidate-btn ДОПУСТИМ только если генератор уже читает stylename

**Расхождения:**
1. stylename: `no-horizontal-lines job-candidate-table` вместо `borderless grid candidate-browse-grid` (no-horizontal-lines сохранить)
2. Генераторы колонок — не трогать (п.55 задачи, бизнес-логика)
3. Колонка-кнопка требует проверки стиля candidate-btn

### 3. suggestVacancyTable (строка 1309)
**Текущее состояние:**
- stylename: `no-horizontal-lines job-candidate-table`
- contextMenuEnabled: false, editable: false
- width: 100%, height: 100%
- columns: notSendedIconColumn (20px), vacansyName (captionAsHtml=true, maxTextLength=60)
- dataContainer: suggestOpenPositionDc

**Требования контракта:**
- stylename=`borderless grid candidate-browse-grid`
- no-horizontal-lines сохранить
- word-break в текстовых колонках (названия вакансий)

**Расхождения:**
1. stylename: `no-horizontal-lines job-candidate-table` вместо `borderless grid candidate-browse-grid` (no-horizontal-lines сохранить)
2. captionAsHtml=true — не трогать (поведение)
3. word-break для vacansyName не задан

### 4. jobCandidateIteractionListTable (строка 1373)
**Текущее состояние:**
- stylename: `job-candidate-table`
- bodyRowHeight=36px (против контрактных 38px)
- reorderingAllowed=true (не трогать — поведение)
- textSelectionEnabled=false (не трогать — поведение)
- contextMenuEnabled=false
- width: 100%
- actions: create, edit, remove, refresh
- buttonsPanel: create, frequentInteractionPopupButton, edit, copyIteractionButton, remove, openPositionProjectDescriptionButton
- columns: (нужно прочитать дальше)

**Требования контракта:**
- stylename=`borderless grid candidate-browse-grid`
- bodyRowHeight=38px (привести к контрактной, п.2 задачи)
- reorderingAllowed/textSelectionEnabled — НЕ трогать
- hover/selected-состояния по образцу контракта

**Расхождения:**
1. stylename: `job-candidate-table` вместо `borderless grid candidate-browse-grid`
2. bodyRowHeight=36px вместо контрактных 38px — ИСПРАВИТЬ (п.2 задачи)
3. word-break в текстовых колонках не задан

### 5. jobCandidateCandidateCvTable (строка 1505)
**Текущее состояние:**
- stylename: `job-candidate-table`
- bodyRowHeight=55px (HTML-контент — сохранить)
- captionAsHtml=true, descriptionAsHtml=true
- width: 100%
- columns: projectLogoColumn (70px, componentRenderer), datePost (100px), toVacancy, resumePosition (collapsible), iconOriginalCVFile (100px), iconHuntTechCVFile (100px), letter (100px), candidateOriginalCVColumn (150px, componentRenderer), candidateHuntTechCVColumn (150px, componentRenderer), author
- dataContainer: jobCandidateCandidateCvsDc

**Требования контракта:**
- stylename=`borderless grid candidate-browse-grid`
- bodyRowHeight=38px стандарт, но для CV-таблицы 55px остаётся (HTML-контент) — обосновать отступление в PR (п.2 задачи)
- word-break в текстовых колонках

**Расхождения:**
1. stylename: `job-candidate-table` вместо `borderless grid candidate-browse-grid`
2. bodyRowHeight=55px — ОСТАВИТЬ, обосновать в PR как отступление от 38px (HTML-контент)
3. word-break в текстовых колонках не задан

## SCSS анализ

Текущий класс `.job-candidate-table` (строка 807+):
- Высота заголовка: не задана явно (цвет, фон, border-bottom)
- Высота строки: не задана явно (только hover/selected фоны)
- Нет word-break
- hover: background #f3f8fe
- selected: background #dcecff !important
- border-radius: 7px, border: 1px solid #dce3eb

Контрактное требование (candidate-browse-grid, edit-screen-shared-styles.scss:1017-1051):
- min-height: 38px для строк
- padding-top/bottom: 6px
- white-space: normal, line-height: 1.35, word-break: break-word
- Заголовок: min-height: 42px (как в job-candidate-table сейчас)

## План действий

1. **XML изменения** (каждый коммит отдельно):
   - socialNetworkTable: stylename → "borderless grid candidate-browse-grid", добавить word-break в networkURLS
   - lastProjectTable: stylename → "borderless grid candidate-browse-grid no-horizontal-lines" (сохранить no-horizontal-lines)
   - suggestVacancyTable: stylename → "borderless grid candidate-browse-grid no-horizontal-lines"
   - jobCandidateIteractionListTable: stylename → "borderless grid candidate-browse-grid", bodyRowHeight="38px"
   - jobCandidateCandidateCvTable: stylename → "borderless grid candidate-browse-grid", bodyRowHeight="55px" (оставить, обосновать)

2. **SCSS изменения** (аддитивные, в job-candidate-editor.scss):
   - Добавить новые селекторы для `.job-candidate-editor .candidate-browse-grid` (скопнутые через .job-candidate-editor)
   - Наследовать стили от .candidate-browse-grid для всех таблиц
   - Добавить word-break для текстовых колонок
   - Синхронизировать во всех 7 темах

3. **Тестирование:**
   - ScreenViewIntegrityTest — зелёный
   - Визуальная приёмка в 3 темах
   - Регресс-скриншот JobCandidateReestrBrowse

4. **Документация:**
   - Обновить PR-описание с обоснованием CV 55px
   - Обосновать candidate-btn для lastProjectTable (если генератор читает stylename)
