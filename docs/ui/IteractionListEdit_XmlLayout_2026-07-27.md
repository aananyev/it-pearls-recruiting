# IteractionListEdit — XML-компоновка sidebar и аккордеонов

> Дополнение к [IteractionListEdit_Spec.md](IteractionListEdit_Spec.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Scope: только XML layout и регрессионные тесты

## 1. Назначение и бизнес-смысл (What & Why)

Изменение устраняет визуальную перегруженность `IteractionListEdit` без изменения бизнес-логики. Рекрутёр должен видеть состояние вакансии до перехода по разделам формы, а основной рабочий раздел должен быть доступен сразу после открытия экрана.

Критичный контекст вакансии:

- открыта или закрыта позиция;
- наличие альтернативных вакансий;
- текущий приоритет;
- пиктограмма приоритета.

Этот контекст размещён непосредственно после наименования вакансии, поскольку он влияет на решение продолжать регистрацию взаимодействия.

## 2. UI Context & Navigation

### 2.1. Новый порядок sidebar

```text
candidateImage + projectLogoImage
→ ФИО кандидата
→ наименование вакансии
→ статус вакансии + warning alternatives
→ приоритет + traffic light
→ label-navigation
→ номер и дата взаимодействия
→ срок / компания / проект / стоимость / rating context
```

Component order:

```text
iteractionVacancyNameLabel
→ vacancyStateSummary
   ├── vacancyStatusSummary
   │   ├── statusOfVacansyLabel
   │   └── alternativeVacancyLinkButton
   └── vacancyPrioritySummary
       ├── trafficLighterImage
       └── currentPriorityLabel
→ iteractionListNavigation
```

Нижняя `iteraction-list-vacancy-card` больше не содержит `statusOfVacansyLabel` и `currentPriorityLabel`, поэтому значения не дублируются.

### 2.2. Рабочая область

```text
toolbar
→ mostPopularQuickActions
→ vertical scroll
   ├── participantsAccordion — открыт
   ├── interactionAccordion — закрыт
   ├── resultAccordion — закрыт
   └── commentAccordion — закрыт
→ footer
```

`participantsAccordion` содержит отдельный `participantsAccordionContent`, объединяющий двухколоночный grid и строку подписочного фильтра.

## 3. Behavior Summary

- открытие формы → `participantsAccordion` имеет `collapsed="false"` → кандидат и вакансия доступны сразу;
- остальные рабочие GroupBox → `collapsed="true"` → форма не создаёт длинную полосу одновременно раскрытых блоков;
- выбор вакансии → прежний controller обновляет status/priority → значения отображаются перед label-navigation;
- закрытая вакансия с альтернативами → прежний `alternativeVacancyLinkButton` остаётся рядом со статусом;
- изменение priority → прежний `trafficLighterImage` и `currentPriorityLabel` обновляются без новых listeners;
- save/cancel/subscribe → прежние actions и invoke не изменены.

## 4. Разрешённый XML scope

Изменены только:

- visual containers;
- порядок уже существующих компонентов;
- `width`, `height`, `spacing`, `margin`, `align`, `expand`;
- semantic/local stylename;
- initial `collapsed` state;
- XML-комментарии о назначении layout.

## 5. Сохранённые бизнес-контракты

Не изменены:

- `IteractionListEdit.java`;
- `IteractionListEditAccordionNavigation.java`;
- entity `IteractionList`;
- services;
- data containers;
- loaders;
- JPQL;
- views;
- component ID;
- `dataContainer` и `property`;
- picker actions;
- `invoke="callActionEntity"`;
- `invoke="onButtonSubscribeClick"`;
- `windowCommitAndClose`;
- `windowClose`;
- required/visible runtime logic;
- БД и Liquibase;
- SCSS семи тем.

## 6. Component integrity

Обязательные component ID сохранены:

- `candidateImage`;
- `projectLogoImage`;
- `iteractionCandidateNameLabel`;
- `iteractionVacancyNameLabel`;
- `statusOfVacansyLabel`;
- `alternativeVacancyLinkButton`;
- `trafficLighterImage`;
- `currentPriorityLabel`;
- `iteractionListNavigation`;
- `numberIteractionField`;
- `dateIteractionField`;
- `companyLabel`;
- `projectLabel`;
- `outstaffingCostHBox`;
- `ratingLabel`;
- `ratingImage`;
- все четыре рабочих GroupBox;
- все business fields и footer actions.

## 7. Data View Integrity

Поскольку getter-цепочки и bindings не изменялись, существующие views остаются контрактом:

- `iteractionList-edit-view`;
- `jobCandidate-iteraction-list-suggestion-view`;
- `openPosition-iteraction-list-picker-view`;
- `iteraction-list-type-view`.

Hermes обязан подтвердить `ScreenViewIntegrityTest 8/8 PASS` и отсутствие unfetched attribute при новом и существующем объекте.

## 8. Regression tests

### `IteractionListSidebarContextPanelTest`

Проверяет:

- порядок vacancy name → status/priority → navigation;
- наличие warning и priority image в новом summary;
- отсутствие status/priority в нижней vacancy card;
- сохранность identity bindings;
- фактические theme-local копии shared styles вместо удалённых symlink/common partial.

### `IteractionListEditAccordionLayoutTest`

Проверяет:

- XML parse;
- двухпанельный порядок;
- отсутствие TabSheet;
- первый GroupBox открыт;
- остальные три GroupBox закрыты;
- natural height;
- сохранность fields, actions, invoke и bindings;
- horizontal-scroll contract всех семи theme-local partial.

## 9. Runtime smoke

1. Открыть новый `IteractionListEdit`.
2. Убедиться, что раздел «Кандидат и вакансия» открыт сразу.
3. Выбрать кандидата и вакансию.
4. Проверить status и priority под названием вакансии.
5. Проверить, что status/priority находятся выше `РАЗДЕЛЫ ФОРМЫ`.
6. Проверить закрытую вакансию и warning alternatives.
7. Проверить раскрытие остальных GroupBox.
8. Проверить quick actions, dynamic fields, rating, comment, subscribe, save и cancel.
9. Проверить отсутствие горизонтальной прокрутки.
10. Повторить visual smoke во всех семи темах.

## 10. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Статус и приоритет перенесены после наименования вакансии перед label-навигацией. Первый рабочий аккордеон раскрыт декларативно. Java и бизнес-логика не изменены. |
