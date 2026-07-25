# IteractionListEdit — спецификация экранной формы

> Screen ID: `hunttech_IteractionList.edit`  
> Базовый controller: `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit`  
> Presentation-controller: `com.company.hunttech.web.screens.iteractionlist.IteractionListEditAccordionNavigation`  
> Активный XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit-accordion-navigation.xml`  
> Базовый XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: кандидата, вакансию, тип взаимодействия, дополнительное действие или значение, рейтинг, рекрутёра, способ коммуникации и комментарий. Запись участвует в истории кандидата, цепочках взаимодействий, подписках, уведомлениях, статусах процесса и связанных кадровых сценариях.

Аккордеонная компоновка от 2026-07-25 сокращает визуальную перегрузку диалога `1000 × 650`: пользователь работает с одним смысловым разделом за раз, а кликабельный индекс в левой панели позволяет перейти к нужному разделу без прокрутки. Решение повторяет подтверждённый паттерн `SettingsWindow`: статические `Label` служат fallback-разметкой, а controller заменяет их на визуально идентичные `borderless Button` с активным состоянием.

## UI Context & Navigation

- Экран открывается из browse взаимодействий, карточки кандидата и связанных сценариев создания или редактирования `IteractionList`.
- Picker кандидата сохраняет lookup и open для `JobCandidate`.
- Picker вакансии сохраняет lookup и open для `OpenPosition`.
- Выбор типа взаимодействия сохраняет управление `buttonCallAction`, `addString`, `addDate` и `addInteger` из базового controller.
- Кнопка подписки сохраняет `invoke="onButtonSubscribeClick"`.
- Сохранение выполняется стандартным action `windowCommitAndClose`, отмена — `windowClose`.
- Новый controller наследует базовый controller и использует тот же screen ID, поэтому существующие точки открытия экрана не меняются.

## Behavior Summary

- открытие формы → создаётся presentation-controller → первый раздел «Кандидат и вакансия» раскрывается, остальные разделы сворачиваются;
- клик по пункту слева → раскрывается выбранный `GroupBox` → остальные четыре `GroupBox` сворачиваются, активный пункт получает акцентный стиль, фокус переходит в первое штатное поле;
- клик по заголовку свёрнутого `GroupBox` → срабатывает `ExpandedStateChangeListener` → раскрытый раздел синхронизируется с индексом слева и становится единственным открытым;
- выбор кандидата → выполняются прежние проверки и обновление `OvaFallbackImage` → данные и fallback изображения работают как до изменения;
- выбор вакансии → выполняются прежние проверки закрытия, подписки, статуса, приоритета и логотипа → sidebar обновляется прежними методами;
- выбор типа взаимодействия → базовый controller меняет runtime `visible`, `required`, caption и действие дополнительного компонента → аккордеон не вмешивается в бизнес-логику;
- сохранение или отмена → выполняются прежние lifecycle handlers и стандартные actions → новая навигация не изменяет entity и DataContext;
- смена темы → подключается локальный mixin `iteraction-list-accordion-navigation-theme` → навигация и аккордеоны сохраняют одинаковую геометрию в семи темах.

## 1. Архитектура экрана

| Параметр | Значение |
|---|---|
| Screen ID | `hunttech_IteractionList.edit` |
| Базовый класс | `IteractionListEdit extends StandardEditor<IteractionList>` |
| Presentation-класс | `IteractionListEditAccordionNavigation extends IteractionListEdit` |
| `@EditedEntityContainer` | `iteractionListDc` |
| Загрузка | `@LoadDataBeforeShow` |
| Диалог | `width=1000`, `height=650`, `modal=true` |
| Root namespace | `.iteraction-list-editor` |
| Поддерживаемые темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

Presentation-controller содержит только:

- создание пяти navigation buttons через `UiComponents`;
- взаимоисключающее раскрытие `GroupBoxLayout`;
- синхронизацию активного пункта;
- перевод фокуса в первое поле раздела.

Он не использует `DataManager`, loaders, сервисы, `getEditedEntity()`, `setValue()` или commit API.

## 2. Data-контракты

| Контейнер / loader | View | Назначение | Статус |
|---|---|---|---|
| `iteractionListDc` / `iteractionListDl` | `iteractionList-edit-view` | редактируемое взаимодействие | без изменений |
| `iteractionTypesDc` / `iteractionTypesLc` | `iteraction-list-type-view` | типы взаимодействий | без изменений |
| `openPositionDc` / `openPositionsDl` | `openPosition-iteraction-list-picker-view` | доступные вакансии | без изменений |
| `usersDc` / `usersDl` | `_minimal` | активные пользователи | без изменений |

В активный descriptor дословно перенесены исходные JPQL, query conditions, параметры loaders, `cacheable`, views и `dialogMode`. Entity, поля, БД, Liquibase и `views.xml` не изменяются.

## 3. Компоновка

```text
main layout 100% × 100%
├─ sidebar 252 px, full height
│  ├─ context card
│  │  ├─ candidateImage: OvaFallbackImage 104 × 104
│  │  ├─ projectLogoImage: Image 76 × 76
│  │  ├─ number / date / closing date
│  │  ├─ company / project
│  │  ├─ vacancy status / priority / outstaffing
│  │  └─ rating context
│  └─ clickable section index
│     ├─ Кандидат и вакансия
│     ├─ Тип и действие
│     ├─ Оценка и коммуникация
│     ├─ Комментарий
│     └─ Частые взаимодействия
└─ workspace
   ├─ toolbar
   ├─ TabSheet + vertical ScrollBox
   │  ├─ participantsAccordion [expanded]
   │  ├─ interactionAccordion [collapsed]
   │  ├─ resultAccordion [collapsed]
   │  ├─ commentAccordion [collapsed]
   │  └─ popularAccordion [collapsed]
   └─ footer: subscribe → commit-and-close → cancel
```

### 3.1 Кликабельные LABEL

В XML размещены пять `Label` как fallback для CUBA Studio и безопасной загрузки descriptor. После `InitEvent` controller:

1. сохраняет заголовок `msgAccordionNavigation`;
2. очищает только контейнер `iteractionListNavigation`;
3. создаёт пять `Button` с `stylename="borderless iteraction-list-nav-item"`;
4. назначает click handlers;
5. применяет `iteraction-list-nav-item-active` выбранному пункту.

Таким образом, пункты визуально воспринимаются как LABEL, но доступны для мыши и клавиатурного фокуса.

### 3.2 Аккордеоны

| ID | Содержимое | Первое поле для фокуса |
|---|---|---|
| `participantsAccordion` | `candidateField`, `vacancyFiels`, `onlyMySubscribeCheckBox` | `candidateField` |
| `interactionAccordion` | `iteractionTypeField`, `buttonCallAction`, `addString`, `addDate`, `addInteger` | `iteractionTypeField` |
| `resultAccordion` | `ratingField`, `recrutierField`, `communicationMethodField` | `ratingField` |
| `commentAccordion` | `commentField` | `commentField` |
| `popularAccordion` | `mostPopularHbox`, `mostPopularIteractionHBox` | без изменения фокуса |

Аккордеоны остаются штатными `GroupBoxLayout` с `collapsable="true"` и `showAsPanel="true"`. При переходе через левый индекс открыт ровно один раздел. Ручное раскрытие заголовком также синхронизируется через `ExpandedStateChangeListener`.

## 4. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `candidateField` | binding, required, lookup/open, исходный query |
| `vacancyFiels` | binding, optionsContainer, lookup/open, Java listeners и providers |
| `iteractionTypeField` | binding, required, lookup, Java value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime `visible` / `required` / caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer, option icon provider |
| `communicationMethodField` | binding, полноширинное размещение |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | `OvaFallbackImage`, legacy ID, binding, fallback, совместимая Java-инъекция `Image` |
| `projectLogoImage` | отдельный `Image`, прежний runtime source |
| `mostPopularHbox`, `mostPopularIteractionHBox` | отдельные XML-контейнеры |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | порядок subscribe → commit-and-close → cancel |

Component ID, типы бизнес-компонентов, bindings, actions, `invoke`, loaders, JPQL, views и captions существующих полей не переименовываются.

## 5. Локальный SCSS

Для каждой темы добавляется файл:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-accordion-navigation.scss
```

Mixin `iteraction-list-accordion-navigation-theme` подключается в `styles.scss` каждой темы. Все селекторы вложены в `.iteraction-list-editor`; глобальные `.v-button`, `.v-label`, `.v-tabsheet`, `.v-panel` и `.v-gridlayout` не изменяются.

SCSS задаёт:

- геометрию и active/hover/focus состояния кликабельного индекса;
- panel-caption и content аккордеонов;
- двухколоночную сетку без horizontal scroll;
- адаптивные padding при viewport до `1100 px`;
- совместимость со светлыми и тёмными темами через `$v-font-color`, `$v-panel-background-color`, `$v-app-background-color`.

## 6. Ограничения изменений

- существующий `IteractionListEdit.java` не изменяется;
- существующие business handlers, `@Subscribe`, `@Install`, сервисы и lifecycle не изменяются;
- entity, БД, Liquibase, views, JPQL и loaders не изменяются;
- новый controller не записывает значения entity и не вызывает commit;
- production не изменяется;
- merge допускается только после отчёта Hermes по точному HEAD SHA.

## 7. Проверки

| Проверка | Статус до Hermes |
|---|---|
| XML well-formed | PASS, локальная структурная проверка |
| required component ID / actions / invoke static audit | PASS |
| Java navigation business-state guard | PASS, статический тест |
| SCSS root namespace / imports семи тем | PASS, статический тест |
| `git diff --check` | NOT VERIFIED |
| compile / compileTestJava | NOT VERIFIED |
| `IteractionListAccordionNavigationTest` | NOT VERIFIED |
| `ScreenViewIntegrityTest` 8/8 | NOT VERIFIED |
| Data View Integrity | NOT VERIFIED |
| `buildScssThemes` | NOT VERIFIED |
| `clean assemble` | NOT VERIFIED |
| local deploy / HTTP 200 | NOT VERIFIED |
| functional and visual smoke | NOT VERIFIED |
| Tomcat logs / P1 / P2 | NOT VERIFIED |

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## 8. Обязательная проверка Hermes

1. Подтвердить branch HEAD, PR HEAD и переданный SHA; несовпадение — `HEAD_MISMATCH`.
2. Подтвердить base=`master`, conflicts=NONE.
3. Выполнить `git diff --check`.
4. Выполнить compile web и compile tests.
5. Выполнить `IteractionListAccordionNavigationTest`.
6. Выполнить `ScreenViewIntegrityTest` — ожидается `8/8 PASS`.
7. Выполнить Data View Integrity для `iteractionList-edit-view`.
8. Выполнить `:app-web:buildScssThemes` для семи тем.
9. Выполнить `clean assemble` — ожидается `BUILD SUCCESSFUL`.
10. Выполнить local deploy и проверить HTTP `/hrm/` = `200`.
11. Проверить клики по каждому пункту слева и по каждому заголовку аккордеона.
12. Проверить candidate/vacancy/type/dynamic fields/rating/recruiter/comment/popular interactions/subscription/save/cancel.
13. Проверить семь тем: active/hover/focus, captions, отсутствие horizontal scroll и пустых dynamic slots.
14. Проверить Tomcat logs: новых critical errors NONE; P1=0; P2=0.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Добавлены пять взаимоисключающих аккордеонов и кликабельный индекс в левой панели по паттерну `SettingsWindow`; business/data/lifecycle-контракты сохранены |
| 2026-07-25 | `candidateImage` в левой панели приведён к `OvaFallbackImage` 104 × 104 с fallback `icons/no-programmer.jpeg` при сохранении legacy ID и Java-инъекции `Image` |
| 2026-07-25 | Улучшена двухпанельная компоновка: sidebar сделан непрерывным, toolbar и footer перенесены в workspace, поля выстроены по сценарию рекрутёра |
| 2026-07-25 | Выполнена первоначальная визуальная адаптация `IteractionListEdit` к UI/UX-концепции HRM HuntTech |
