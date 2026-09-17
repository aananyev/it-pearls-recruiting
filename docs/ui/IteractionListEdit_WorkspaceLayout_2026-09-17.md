# IteractionListEdit — контракт компоновки правой рабочей области

> Проект: **HRM HuntTech**  
> Экран: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Область изменения: только визуальная геометрия правой рабочей области  
> Платформа: CUBA Platform 7.3-SNAPSHOT

## 1. Назначение и бизнес-смысл (What & Why)

Правая рабочая область `IteractionListEdit` предназначена для последовательной фиксации взаимодействия рекрутёра с кандидатом: выбора кандидата и вакансии, типа взаимодействия, результата, способа связи и комментария.

Поля должны читаться как единая форма сверху вниз. Рабочая область обязана подстраиваться под доступную ширину браузера без изменения размеров sidebar, без горизонтального переполнения и без смещения caption относительно собственного поля. Поэтому адаптивная геометрия правой части является частью UI-контракта Edit-форм HRM HuntTech, хотя бизнес-логика сущности при этом не меняется.

Цель текущей доводки:

- сохранить исправленное верхнее выравнивание workspace относительно sidebar;
- не изменять размеры, breakpoints и внутреннюю геометрию sidebar;
- сделать правую рабочую область динамической по фактически доступной ширине;
- обеспечить адаптивное двухколоночное размещение полей с безопасным переходом в одну колонку на узком viewport;
- расположить caption каждого поля непосредственно над соответствующим control и выровнять по его левому краю;
- закрепить белую жирную подпись активных зелёных кнопок «Частые взаимодействия»;
- сохранить существующий вертикальный ритм и отсутствие наложений.

## 2. UI Context & Navigation

Экран открывается как editor `hunttech_IteractionList.edit` из реестра взаимодействий и связанных сценариев карточки кандидата. Визуальная структура текущего экрана:

```text
IteractionListEdit
├── sidebar — существующий контекст кандидата/вакансии, не изменяется
└── workspace — динамически занимает всё оставшееся место
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
| Изменение ширины окна | ширина sidebar определяется существующим экранным контрактом | workspace через flex занимает всё фактически оставшееся пространство без `calc(width - sidebar)` |
| Широкий/средний viewport | полезной ширины достаточно | `candidate/vacancy` и `rating/recruiter` расположены в двух равных адаптивных колонках |
| Узкий viewport `<=960px` | две колонки становятся слишком узкими | только правая форма переходит в одну колонку; sidebar не изменяется этим правилом |
| Отрисовка caption | поле имеет подпись | caption находится строго над собственным control и совпадает с ним по левому краю |
| Отрисовка активной quick-action кнопки | кнопка имеет зелёный фон и не disabled | caption белый `#ffffff` и `font-weight: 700` |
| Отрисовка соседних строк единой карточки | `iteractionMainInfoBody` использует `spacing="true"` | между строками сохраняется единый вертикальный ритм `12px` |
| Сохранение или отмена | пользователь завершает работу | используются существующие editor actions без изменения бизнес-логики |

## 4. Причина визуальной регрессии и ограничения предыдущего исправления

Предыдущий merge-коммит `ffc3e1486113e6d940ef369d33cf665d7cf5b636` устранил критическое смещение workspace вниз и наложение строк. Для этого Vaadin-slot правой части получил ширину через `calc(100% - <sidebar>)`, а рабочие `GridLayout` были переведены в CSS Grid normal flow.

Такой подход стабилизировал форму, но оставил три presentation-ограничения:

1. ширина workspace всё ещё зависела от жёсткого знания конкретной ширины sidebar вместо реальной оставшейся ширины HBox;
2. caption CUBA/Vaadin сохранял собственные offset/position правила и визуально мог смещаться к левому краю карточки, а не своего поля;
3. активные зелёные quick-action кнопки использовали `font-weight: 600`, тогда как целевой контракт требует явную жирность `700`.

Новая доводка не требует изменения XML или Java: проблема остаётся в финальном локальном SCSS.

## 5. Реализация

### 5.1. Финальный локальный слой

Используется существующий partial:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-workspace-layout.scss
```

Он подключается **после** `iteraction-list-visual-alignment.scss` и ограничен корневым namespace `.iteraction-list-editor`.

Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` не вводятся.

### 5.2. Динамическая геометрия workspace

`iteraction-list-main-layout` и его возможная Vaadin-обёртка `.v-expand` работают как flex-контейнер. Sidebar остаётся существующим flex-item `0 0 auto`, а workspace получает:

```scss
flex: 1 1 0%;
width: auto;
min-width: 0;
max-width: none;
```

Это означает, что правый блок автоматически занимает фактически оставшуюся ширину браузера и не знает о числовом размере sidebar.

В финальном partial запрещены жёсткие вычисления:

```text
calc(100% - 312px)
calc(100% - 252px)
```

Размеры sidebar, его media-breakpoints и внутренние отступы не изменяются.

### 5.3. Адаптивные строки ввода

Только два локальных `GridLayout` используют стабильный CSS Grid flow:

- `gridIterationData` / `.iteraction-list-participants-grid`;
- `resultAccordionGrid` / `.iteraction-list-result-grid`.

На обычной ширине:

```text
колонки: repeat(2, minmax(0, 1fr))
межколоночный gap: 16px
вертикальный gap: 12px
candidate | vacancy
rating    | recruiter
communicationMethod — 100% второй строки result
```

Для прямых `.v-gridlayout-slot` используется `position: static !important`, чтобы inline/legacy `left/top` CUBA не управляли итоговой раскладкой.

На viewport `<=960px` меняется только правая форма:

```text
candidate
vacancy

rating
recruiter
communicationMethod
```

Sidebar этим media rule не затрагивается.

### 5.4. Вертикальный ритм

Для непосредственных spacing-элементов `iteractionMainInfoBody` сохраняется единый интервал `12px`. Это разделяет последовательные строки формы без добавления новых XML-контейнеров и без изменения component ID.

### 5.5. Caption каждого поля

В пределах `.iteraction-list-unified-body` контейнер `.v-has-caption` переводится в вертикальный flex-flow. Его `.v-caption` получает:

- `position: static`;
- `width: 100%`;
- `margin: 0 0 5px`;
- `padding: 0`;
- `text-align: left`;
- сброс `top/right/bottom/left`.

В результате caption находится непосредственно над своим control и использует тот же левый край, что и input/picker/textarea. Для самого control сохраняются `width: 100%`, `min-width: 0`, `max-width: 100%` и `margin-left: 0`.

### 5.6. Кнопки «Частые взаимодействия»

Только активные зелёные quick-action кнопки (не `disabled`) получают в финальном слое:

```scss
color: #ffffff !important;
font-weight: 700 !important;
```

Правило применяется к самой кнопке, `.v-button-wrap` и `.v-button-caption`. Disabled-placeholder `Нет данных` сохраняет прежний нейтральный стиль.

### 5.7. Темы

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
- размеры, breakpoints, содержимое и внешний вид sidebar;
- entity и поля `IteractionList`;
- `dataContainer`, `property` и options containers;
- loaders и JPQL;
- views;
- actions и `invoke`;
- validators и required-правила;
- сервисы;
- DataContext и lifecycle;
- БД и Liquibase;
- другие экраны.

## 7. Автоматический контракт

`IteractionListWorkspaceLayoutContractTest` защищает следующие инварианты:

1. финальный SCSS partial идентичен во всех семи темах;
2. partial импортируется и включается после `iteraction-list-visual-alignment`;
3. workspace использует flex и фактически оставшуюся ширину без жёстких `312px/252px` вычислений;
4. финальный слой не переопределяет размеры sidebar;
5. рабочие GridLayout используют CSS Grid normal flow и adaptive single-column breakpoint;
6. `communicationMethodField` остаётся отдельной полноширинной строкой;
7. caption расположен над своим control и выровнен по левому краю;
8. активные зелёные quick-action кнопки используют белый текст и `font-weight: 700`;
9. вертикальный интервал между строками составляет `12px`.

## 8. Проверка Hermes

После checkout точного HEAD необходимо выполнить:

```bash
git diff --check
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListWorkspaceLayoutContractTest' \
  --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime/visual smoke минимум в `halo`, `hover`, `hunttech-modern-dark`:

- sidebar имеет те же размеры и визуальную геометрию, что в базовом master;
- верх workspace совпадает с верхом sidebar;
- при изменении ширины браузера workspace плавно занимает оставшееся место;
- на обычной ширине candidate/vacancy и rating/recruiter находятся в строках 50/50;
- на узком viewport правая форма корректно переходит в одну колонку без изменения sidebar;
- каждый caption находится непосредственно над своим полем и совпадает с ним по левому краю;
- активные зелёные quick-action кнопки имеют белый жирный caption;
- `Способ связи` и `Комментарий` не перекрываются предыдущими полями;
- footer доступен;
- горизонтального scroll/overflow нет;
- `/hrm/` отвечает HTTP 200;
- в Tomcat logs отсутствуют новые ошибки изменённого сценария.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-09-17 | Правый workspace переведён с жёсткого `calc(width - sidebar)` на адаптивный flex-контракт без изменения размеров sidebar; рабочие сетки получили безопасный single-column reflow на узком viewport; caption каждого поля закреплён непосредственно над своим control и выровнен по его левому краю; активные зелёные quick-action кнопки получили белый `font-weight: 700`. |
| 2026-09-17 | Зафиксирован отдельный финальный SCSS-контракт правой рабочей области: устранено смещение workspace вниз, GridLayout переведены в устойчивый CSS Grid flow, между строками введён единый вертикальный ритм `12px`; XML, Java, sidebar и бизнес-логика не изменяются. |
