# IteractionListEdit — контракт компоновки правой рабочей области

> Проект: **HRM HuntTech**  
> Экран: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Область изменения: только визуальная геометрия правой рабочей области  
> Платформа: CUBA Platform 7.3-SNAPSHOT

## 1. Назначение и бизнес-смысл (What & Why)

Правая рабочая область `IteractionListEdit` предназначена для последовательной фиксации взаимодействия рекрутёра с кандидатом: выбора кандидата и вакансии, типа взаимодействия, результата, способа связи и комментария.

Поля должны читаться как единая форма сверху вниз. Смещение workspace ниже sidebar или наложение соседних строк нарушает порядок ввода, затрудняет визуальную проверку данных и повышает риск заполнения не того поля. Поэтому геометрия правой части является частью UI-контракта Edit-форм HRM HuntTech, хотя сама бизнес-логика сущности при этом не меняется.

Цель текущего изменения:

- вернуть правую рабочую область к верхней границе двухпанельной формы;
- сохранить фиксированный sidebar без изменений;
- обеспечить устойчивую двухколоночную раскладку полей;
- добавить читаемый вертикальный интервал между строками ввода;
- исключить горизонтальное и вертикальное наложение компонентов при изменении ширины окна.

## 2. UI Context & Navigation

Экран открывается как editor `hunttech_IteractionList.edit` из реестра взаимодействий и связанных сценариев карточки кандидата. Визуальная структура текущего экрана:

```text
IteractionListEdit
├── sidebar — существующий контекст кандидата/вакансии, не изменяется
└── workspace
    ├── toolbar
    ├── «Частые взаимодействия»
    ├── scroll-area
    │   └── iteractionMainInfoCard
    │       └── iteractionMainInfoBody
    │           ├── Кандидат | Вакансия
    │           ├── Только мои подписки
    │           ├── Тип взаимодействия
    │           ├── Динамические поля действия
    │           ├── Рейтинг | Рекрутер
    │           ├── Способ связи
    │           └── Комментарий
    └── footer actions
```

Переходы, actions, `dataContainer`, `property`, loaders, component ID и lifecycle контроллера остаются прежними. Текущая задача не меняет sidebar и не добавляет новую навигацию.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| Открытие `IteractionListEdit` | экран размещён в стандартной рабочей области CUBA | sidebar и workspace начинаются от одной верхней линии |
| Изменение ширины окна | sidebar имеет фиксированную ширину `312px` | workspace занимает только оставшуюся ширину и не переносится вниз |
| Узкое окно до `1100px` | существующий sidebar сжимается до `252px` | ширина workspace пересчитывается относительно `252px` |
| Отрисовка `gridIterationData` | кандидат и вакансия видимы | поля располагаются в двух равных колонках без пересечения |
| Отрисовка `resultAccordionGrid` | видимы рейтинг, рекрутёр и способ связи | рейтинг/рекрутёр находятся в первой строке, способ связи — отдельной полноширинной строкой |
| Отрисовка соседних строк единой карточки | `iteractionMainInfoBody` использует `spacing="true"` | между строками сохраняется единый вертикальный ритм `12px` |
| Сохранение или отмена | пользователь завершает работу | используются существующие editor actions без изменения бизнес-логики |

## 4. Причина визуальной регрессии

В финальном локальном SCSS-слое `IteractionListEdit` одновременно действовали два несовместимых подхода к геометрии CUBA/Vaadin:

1. Vaadin-slot расширяемого workspace принудительно получал `width: 100% !important`. При наличии фиксированного sidebar `312px` сумма ширин дочерних slot-ов становилась больше доступной ширины `HorizontalLayout`, поэтому правая часть могла смещаться/переноситься ниже левой панели.
2. `GridLayout` CUBA исторически рендерит ячейки как позиционируемые `.v-gridlayout-slot`, а общий Edit-слой нормализует их для обычного потока. Локальный SCSS продолжал рассчитывать размещение через `left/top` и фиксированную высоту. После объединения полей в одну карточку это приводило к наложению строк.

Исправление не требует изменения XML или Java: проблема находится в финальной геометрии локального SCSS.

## 5. Реализация

### 5.1. Финальный локальный слой

Добавлен partial:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-workspace-layout.scss
```

Он подключается **после** `iteraction-list-visual-alignment.scss` и ограничен корневым namespace `.iteraction-list-editor`.

Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` не вводятся.

### 5.2. Геометрия workspace

Для Vaadin-slot правой части используется оставшаяся ширина двухпанельной формы:

```scss
width: calc(100% - 312px);
max-width: calc(100% - 312px);
```

При существующем breakpoint `max-width: 1100px`, где sidebar уже имеет ширину `252px`, workspace синхронно использует:

```scss
width: calc(100% - 252px);
max-width: calc(100% - 252px);
```

Дополнительно фиксируется верхнее выравнивание (`margin-top: 0`, `padding-top: 0`, `vertical-align: top`).

### 5.3. Двухколоночные строки

Только два локальных `GridLayout` переводятся в стабильный CSS Grid flow:

- `gridIterationData` / `.iteraction-list-participants-grid`;
- `resultAccordionGrid` / `.iteraction-list-result-grid`.

Контракт:

```text
колонки: minmax(0, 1fr) + minmax(0, 1fr)
вертикальный gap: 12px
первая строка result: rating | recruiter
вторая строка result: communicationMethod на 2 колонки
```

Для прямых `.v-gridlayout-slot` используется `position: static !important`, чтобы inline/legacy `left/top` CUBA не управляли итоговой раскладкой.

### 5.4. Вертикальный ритм

Для непосредственных spacing-элементов `iteractionMainInfoBody` задаётся единый интервал `12px`. Это разделяет последовательные строки формы без добавления новых XML-контейнеров и без изменения component ID.

### 5.5. Темы

Partial должен быть побайтово идентичен во всех семи темах:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

## 6. Неизменяемые контракты

Текущая задача не изменяет:

- `IteractionListEdit.java`;
- `iteraction-list-edit.xml`;
- sidebar и его component ID;
- entity и поля `IteractionList`;
- `dataContainer`, `property` и options containers;
- loaders и JPQL;
- views;
- actions и `invoke`;
- validators и required-правила;
- сервисы;
- DataContext и lifecycle;
- БД и Liquibase.

## 7. Автоматический контракт

`IteractionListWorkspaceLayoutContractTest` защищает следующие инварианты:

1. новый SCSS partial идентичен во всех семи темах;
2. partial импортируется и включается после `iteraction-list-visual-alignment`;
3. workspace использует оставшуюся ширину `312px/252px` sidebar и верхнее выравнивание;
4. рабочие GridLayout используют CSS Grid normal flow;
5. `communicationMethodField` остаётся отдельной полноширинной строкой;
6. вертикальный интервал между строками составляет `12px`.

## 8. Проверка Hermes

После checkout точного HEAD необходимо выполнить:

```bash
git diff --check
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListWorkspaceLayoutContractTest' \
  --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime/visual smoke минимум в `halo`, `hover`, `hunttech-modern-dark`:

- sidebar не изменился;
- верх workspace совпадает с верхом sidebar;
- кандидат/вакансия находятся в одной строке 50/50;
- между соседними строками ввода есть визуально одинаковый интервал;
- `Тип взаимодействия` не перекрывает `Рекрутер`;
- `Рейтинг кандидата` не перекрывает `Способ связи`;
- `Способ связи` находится отдельной строкой;
- комментарий не перекрывается предыдущими полями;
- footer доступен;
- горизонтального scroll/overflow нет;
- `/hrm/` отвечает HTTP 200;
- в Tomcat logs отсутствуют новые ошибки изменённого сценария.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-09-17 | Зафиксирован отдельный финальный SCSS-контракт правой рабочей области: устранено смещение workspace вниз, GridLayout переведены в устойчивый CSS Grid flow, между строками введён единый вертикальный ритм `12px`; XML, Java, sidebar и бизнес-логика не изменяются. |
