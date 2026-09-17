# IteractionListEdit — полное перепроектирование формы

> Проект: **HRM HuntTech**  
> Screen ID: `hunttech_IteractionList.edit`  
> Controller: `IteractionListEdit`  
> Active descriptor: `iteraction-list-edit.xml`  
> Archived descriptor: `iteraction-list-edit-old.xml`  
> Дата: `2026-09-17`

## 1. Цель

Активный `IteractionListEdit` спроектирован заново как Edit-форма HuntTech. Предыдущая компоновка с единым большим input-блоком не используется. Старый XML сохранён как `iteraction-list-edit-old.xml` только как архив и не подключён к screen ID.

Изменение относится к presentation layer. Entity, controller, data containers, loaders, JPQL, views, editor actions и commit lifecycle сохраняются.

## 2. Источники правил

### HuntTech

При проектировании применены:

- `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md`;
- `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`;
- `docs/architecture/XML_Screen_Documentation_Standard.md`;
- `docs/ui/IteractionListEdit_Spec.md`;
- существующие `edit-*` и `label-*` shared styles.

### CUBA Platform 7.2

Применены правила официальной документации CUBA:

- `layout` — корневой вертикальный container;
- `HBox/VBox + expand` — sidebar и workspace занимают предсказуемые области без ручной абсолютной геометрии;
- `GridLayout` — две равные колонки для связанных полей;
- `ScrollBox` имеет явные относительные `width/height`, а его вертикальный child использует `height=AUTO`;
- поля сохраняют declarative binding через `dataContainer/property`;
- стандартные editor actions `windowCommitAndClose` / `windowClose` не заменяются собственной save-логикой.

## 3. Новая архитектура

```text
IteractionListEdit
├── sidebar (270px, edit-sidebar)
│   ├── candidate/project visual identity
│   ├── candidate name + vacancy name
│   ├── label-navigation / индекс разделов
│   ├── service summary: номер + дата взаимодействия
│   ├── vacancy context
│   │   ├── status + priority
│   │   ├── closing date
│   │   ├── company / department
│   │   ├── project
│   │   └── outstaffing cost
│   └── spacer
└── workspace (edit-workspace)
    ├── toolbar
    ├── frequent interactions / five green buttons
    ├── vertical ScrollBox
    │   ├── participantsAccordion
    │   │   └── candidate | vacancy + subscription filter
    │   ├── interactionAccordion
    │   │   └── interaction type + dynamic action/add field
    │   ├── resultAccordion
    │   │   └── rating | recruiter + communication method
    │   └── commentAccordion
    │       └── comment
    └── fixed footer: subscribe / save / cancel
```

## 4. Сохранённая бизнес-логика

### 4.1 Candidate / Vacancy

Сохранены component IDs `candidateField` и `vacancyFiels`, их bindings, actions и options/data sources. Controller продолжает:

- проверять предыдущие взаимодействия кандидата;
- проверять соответствие позиции/локации кандидата вакансии;
- предупреждать о закрытой вакансии;
- управлять фильтром «только мои подписки»;
- для новой пары кандидат–вакансия ограничивать типы взаимодействий веткой `001`;
- обновлять sidebar status/priority/company/project/closing date/outstaffing cost.

### 4.2 Frequent interactions

`mostPopularHbox` сохранён. `InteractionService.getMostPolularIteraction(userSession.getUser(), 5)` остаётся единственным источником ranking. Controller по-прежнему создаёт ровно пять позиций, а доступная зелёная кнопка передаёт точный объект `Iteraction` в `iteractionTypeField`.

### 4.3 Dynamic fields

Сохранены `iteractionTypeField`, `buttonCallAction`, `addDate`, `addString`, `addInteger`.

Контракт controller-а остаётся прежним:

- `addFlag=true`, `addType=1` → показать и потребовать `addDate`;
- `addFlag=true`, `addType=2` → показать и потребовать `addString`;
- `addFlag=true`, `addType=3` → показать и потребовать `addInteger`;
- `callForm=true` → показать `buttonCallAction` и вызвать заданный editor;
- `signComment=true` → сделать comment обязательным;
- `setDateTime=true` → подставить текущее время в `addDate`, если оно пустое;
- изменения дополнительных полей продолжают дописываться в comment.

### 4.4 Calendar integration

В форме нет отдельного calendar widget: интеграция является data-driven.

`addDate` сохранён как поле `IteractionList.addDate` и в новой форме явно отображает дату и время (`resolution=MIN`, `dd.MM.yyyy HH:mm`). Календарные экраны продолжают выбирать `IteractionList` с непустым `addDate` для типов, где `Iteraction.calendarItem=true`. Поэтому перепроектирование layout не разрывает существующий calendar contract.

### 4.5 Save lifecycle

Не изменяются:

- snapshot priority/openClose перед commit;
- `chainInteraction`;
- employee start/end side effects;
- vacancy news after commit;
- candidate status;
- notification/email logic;
- subscribe action;
- стандартные CUBA commit/close actions.

## 5. Component ID compatibility

Новая форма сохраняет controller-critical IDs:

`candidateField`, `vacancyFiels`, `iteractionTypeField`, `buttonCallAction`, `addDate`, `addString`, `addInteger`, `ratingField`, `recrutierField`, `communicationMethodField`, `commentField`, `mostPopularHbox`, `onlyMySubscribeCheckBox`, `numberIteractionField`, `dateIteractionField`, `candidateImage`, `projectLogoImage`, `statusOfVacansyLabel`, `currentPriorityLabel`, `trafficLighterImage`, `alternativeVacancyLinkButton`, `closingDateVacancyLabel`, `companyLabel`, `projectLabel`, `outstaffingCostHBox`, `ratingLabel`, `ratingImage`.

## 6. Архив старой формы

Предыдущий active descriptor сохранён байт-в-байт как:

```text
modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit-old.xml
```

`IteractionListEdit` по-прежнему использует `@UiDescriptor("iteraction-list-edit.xml")`, поэтому при открытии/редактировании `hunttech_IteractionList` вызывается новая форма.

## 7. Regression tests

Обновлены профильные tests, которые раньше намеренно закрепляли единую карточку:

- `IteractionListEditAccordionLayoutTest`;
- `IteractionListBoxIdContractTest`;
- `IteractionListMostPopularInteractionTest`;
- `IteractionListVisualAlignmentTest`;
- `IteractionListSidebarContextPanelTest`;
- `IteractionListXmlSemanticCommentsTest`.

Tests теперь проверяют новую структуру и отдельно сохраняют бизнес-инварианты dynamic fields, calendar date binding, frequent interactions, sidebar bindings и editor actions.

## 8. Проверка перед merge/deploy

На машине с проектом выполнить:

```bash
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListBoxIdContractTest' \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.IteractionListXmlSemanticCommentsTest' \
  --tests 'com.company.hunttech.core.IteractionListWorkspaceLayoutContractTest' \
  --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

После сборки выполнить visual smoke `IteractionListEdit` минимум в `halo`, `hover`, `hunttech-modern-dark` и проверить dynamic types, зелёные quick-actions, calendar-type interaction, save/cancel и Tomcat logs.

## 9. Acceptance criteria

- active screen `hunttech_IteractionList.edit` открывает новый descriptor;
- old descriptor существует отдельно и не вызывается;
- sidebar фиксирован, workspace занимает оставшееся место;
- справа четыре независимых business sections в правильном порядке;
- поля не накладываются, caption относится к своему control;
- five quick-actions сохраняют исторический зелёный стиль и exact-object selection;
- dynamic add-fields переключаются controller-ом без изменения business logic;
- calendar interaction сохраняет `addDate` с датой и временем;
- save lifecycle и side effects работают без регрессий;
- профильные tests, Screen View Integrity, SCSS build и clean assemble проходят;
- production меняется только после успешной проверки и merge.
