# IteractionListEdit — XML-компоновка sidebar и плоских блоков

> Дополнение к [IteractionListEdit_Spec.md](IteractionListEdit_Spec.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Scope: XML layout, локальный SCSS семи тем и регрессионный UI-контракт

## 1. Назначение и бизнес-смысл (What & Why)

Экран фиксирует взаимодействие рекрутёра с кандидатом по конкретной вакансии. Пользователь должен видеть все обязательные блоки ввода одновременно и переходить между ними без раскрытия и сворачивания секций.

Аккордеонная модель исключена по следующим причинам:

- скрытые поля затрудняли контроль полноты заполнения;
- при переходе между разделами изменялась высота формы;
- пользователь терял контекст предыдущего блока;
- в узком viewport появлялась избыточная вертикальная навигация;
- быстрые взаимодействия ошибочно воспринимались как пятый раздел формы.

Новая модель представляет четыре постоянных вертикальных блока. Все поля остаются связанными с теми же атрибутами `IteractionList`.

## 2. UI Context & Navigation

### 2.1. Входы в экран

Экран открывается из существующих точек HRM HuntTech как editor `hunttech_IteractionList.edit`. Parent/child-контракты, ScreenOptions и сценарии открытия не менялись.

### 2.2. Sidebar

Порядок контекста слева:

```text
candidateImage + projectLogoImage
→ ФИО кандидата
→ наименование вакансии
→ статус вакансии
→ приоритет вакансии
→ label-navigation
→ номер и дата взаимодействия
→ компания / проект / стоимость / rating context
```

Статус и приоритет остаются после наименования вакансии и перед `label-navigation`.

На узкой sidebar статус и приоритет отображаются двумя полноширинными строками. Это исключает разрыв значения `ЗАКРЫТА` и подписи `Приоритет` на несколько строк.

### 2.3. Правая рабочая область

```text
toolbar
→ mostPopularQuickActions
→ scrollBox
   ├── participantsAccordion
   ├── interactionAccordion
   ├── resultAccordion
   └── commentAccordion
→ footer
```

Legacy component ID с суффиксом `Accordion` сохранены для совместимости с существующей Java injection. Визуально и поведенчески это постоянные блоки:

- `collapsable="false"`;
- `collapsed="false"`;
- содержимое всегда видимо;
- заголовок не реагирует на клик;
- отсутствует индикатор раскрытия;
- высота определяется содержимым.

`popularAccordion` остаётся невидимым compatibility-компонентом. Быстрые действия находятся в `mostPopularQuickActions` над scroll-area.

### 2.4. Label-navigation

Runtime controller сохраняет существующие кнопки и focus handlers:

| Пункт | Целевой блок | Первое поле |
|---|---|---|
| Кандидат и вакансия | `participantsAccordion` | `candidateField` |
| Тип и действие | `interactionAccordion` | `iteractionTypeField` |
| Оценка и коммуникация | `resultAccordion` | `ratingField` |
| Комментарий | `commentAccordion` | `commentField` |

Нажатие выполняет только presentation-действия:

1. меняет active-state пункта навигации;
2. переводит фокус в первое поле целевого блока;
3. браузер прокручивает focus target в видимую область;
4. CSS `:focus-within` подсвечивает весь блок.

Entity, loaders, services и commit при навигации не затрагиваются.

Legacy-пятый runtime-пункт `popularAccordionNav` скрыт локальным CSS. XML fallback по-прежнему содержит ровно четыре пункта.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| Открытие формы | новый или существующий объект | все четыре блока ввода видимы |
| Клик по label-navigation | выбран один из четырёх пунктов | фокус переходит в первое поле, блок получает accent highlight |
| Клик по заголовку блока | всегда | состояние блока не меняется |
| Выбор типа взаимодействия | business-controller меняет dynamic fields | нужные поля появляются внутри постоянного блока «Тип и действие» |
| Выбор вакансии | controller рассчитывает status/priority | значения обновляются в sidebar |
| Сохранение | стандартная validation и lifecycle | выполняется прежний бизнес-сценарий |
| Отмена | стандартная action | экран закрывается без нового presentation side effect |

## 4. Визуальная схема блоков

### 4.1. Кандидат и вакансия

Две равные колонки:

- `candidateField`;
- `vacancyFiels`.

Ниже отдельной строкой:

- `onlyMySubscribeCheckBox`.

### 4.2. Тип и действие

Вертикально:

- `iteractionTypeField`;
- `buttonCallAction`;
- `addString`;
- `addDate`;
- `addInteger`.

Видимость, caption и required динамических полей определяет существующий controller.

### 4.3. Оценка и коммуникация

Первая строка, две равные колонки:

- `ratingField`;
- `recrutierField`.

Вторая строка:

- `communicationMethodField` на две колонки.

### 4.4. Комментарий

- `commentField`, полноширинный `TextArea`.

## 5. Локальный SCSS-контракт

Каждая из семи тем содержит реальную копию:

`com.company.hunttech/iteraction-list-flat-layout.scss`

Поддерживаемые темы:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Mixin подключается после `iteraction-list-accordion-navigation-theme`, чтобы локально переопределить прежнюю accordion-геометрию.

Корневой namespace:

```scss
.iteraction-list-editor
```

Глобальные `.v-panel`, `.v-label`, `.v-button`, `.v-tabsheet` не изменяются.

## 6. Сохранённые бизнес-контракты

Не изменены:

- `IteractionListEdit.java`;
- `IteractionListEditAccordionNavigation.java`;
- entity `IteractionList`;
- `InteractionService`;
- data containers;
- loaders;
- JPQL;
- views;
- picker actions;
- `dataContainer` и `property`;
- `required` business rules;
- dynamic visibility logic;
- `invoke="callActionEntity"`;
- `invoke="onButtonSubscribeClick"`;
- `windowCommitAndClose`;
- `windowClose`;
- lifecycle сохранения;
- БД и Liquibase.

## 7. Data View Integrity

Сохраняются прежние views:

- `iteractionList-edit-view`;
- `jobCandidate-iteraction-list-suggestion-view`;
- `openPosition-iteraction-list-picker-view`;
- `iteraction-list-type-view`.

Изменение не добавляет новых getter-цепочек и не читает detached associations.

## 8. Regression tests

`IteractionListEditAccordionLayoutTest` проверяет:

- XML parse;
- порядок sidebar/workspace/quick actions/blocks/footer;
- четыре `iteraction-list-flat-section`;
- отсутствие `collapsable="true"`;
- постоянное состояние всех рабочих блоков;
- сохранность bindings, actions и invoke;
- наличие theme-local partial во всех семи темах;
- import/include mixin после прежнего accordion mixin;
- `:focus-within` highlight;
- скрытие шестого runtime slot навигации;
- отсутствие глобальных Vaadin-селекторов.

Остальные профильные тесты должны подтвердить неизменность популярных взаимодействий, sidebar bindings и Data View Integrity.

## 9. Runtime smoke

1. Открыть новый `IteractionListEdit`.
2. Убедиться, что справа одновременно видны четыре блока.
3. Проверить отсутствие стрелок и реакции заголовков на клик.
4. Нажать каждый пункт label-navigation.
5. Убедиться, что целевой блок прокручивается в видимую область.
6. Убедиться, что целевой блок получает accent border/shadow.
7. Проверить, что «Частые взаимодействия» отсутствует в label-navigation.
8. Проверить, что quick actions остаются над блоками.
9. Проверить dynamic fields, rating, comment, subscribe, save/cancel.
10. Повторить в семи темах и при viewport `<=1366px`.
11. Проверить отсутствие horizontal scroll.
12. Проверить закрытую вакансию: статус и priority не ломаются на узкие колонки.

## 10. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Аккордеонная модель заменена четырьмя постоянными вертикальными блоками. Label-navigation переводит фокус и подсвечивает целевой блок. Sidebar уплотнена, status/priority отображаются полноширинными строками. Бизнес-логика не изменена. |
| 2026-07-27 | Статус и приоритет перенесены после наименования вакансии перед label-навигацией. Первый рабочий аккордеон раскрыт декларативно. Java и бизнес-логика не изменены. |
