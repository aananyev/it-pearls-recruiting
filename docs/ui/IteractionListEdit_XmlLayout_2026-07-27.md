# IteractionListEdit — точный XML/CSS render-контракт

> Дополнение к [IteractionListEdit_Spec.md](IteractionListEdit_Spec.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Дата актуализации: 2026-07-28

## 1. Назначение и бизнес-смысл (What & Why)

Документ фиксирует визуальный контракт формы после сопоставления:

- XML-дескриптора;
- активного controller;
- локального SCSS;
- runtime-скриншота;
- ранее подготовленного дизайн-концепта;
- render-review для viewport 1700×950 и 1366×768.

Цель — исключить ситуацию, при которой XML показывает постоянные блоки, Java продолжает управлять expanded state, а CSS маскирует расхождение.

## 2. UI Context & Navigation

### 2.1. Геометрия

```text
312px sidebar | flexible workspace
```

Адаптация:

- ширина больше 1366 px → sidebar 312 px;
- до 1366 px → 296 px;
- до 1100 px → 284 px.

### 2.2. Sidebar

Порядок:

```text
candidateImage + projectLogoImage
→ candidate full name
→ vacancy name
→ status
→ priority
→ label-navigation
→ number/date
→ vacancy context
```

Статус и приоритет отображаются отдельными полноширинными строками. Значение `ЗАКРЫТА` не должно переноситься на две строки.

### 2.3. Workspace

```text
toolbar
→ quick actions 5 × 20%
→ scrollBox
   ├── participantsAccordion : VBox
   ├── interactionAccordion : VBox
   ├── resultAccordion : VBox
   └── commentAccordion : VBox
→ footer
```

`groupBox`, `collapsable`, `collapsed`, `showAsPanel` и скрытый `popularAccordion` отсутствуют.

### 2.4. Структура блока

Каждый раздел имеет структуру:

```xml
<vbox id="...Accordion"
      stylename="iteraction-list-flat-section ...">
    <vbox stylename="iteraction-list-flat-section-header">
        <label stylename="iteraction-list-flat-section-title edit-card-title"/>
    </vbox>
    <vbox stylename="iteraction-list-flat-section-body">
        <!-- business fields -->
    </vbox>
</vbox>
```

Legacy ID сохранены, тип компонента — `VBoxLayout`.

## 3. Behavior Summary

| Действие | Результат |
|---|---|
| Открытие формы | все четыре блока видимы |
| Клик по заголовку | ничего не сворачивается |
| Клик navigation | active-state переходит на пункт и блок |
| После navigation | focus переводится в первое поле блока |
| Focus target вне viewport | ScrollBox прокручивает блок |
| Переход между блоками | предыдущий блок остаётся видимым |
| Недостаточно популярных типов | ряд дополняется disabled `Нет данных` |

## 4. Runtime-согласованность

Активный `IteractionListEdit` инъецирует:

```java
VBoxLayout participantsAccordion;
VBoxLayout interactionAccordion;
VBoxLayout resultAccordion;
VBoxLayout commentAccordion;
```

Запрещены в navigation-участке:

- `GroupBoxLayout`;
- `setExpanded()`;
- `addExpandedStateChangeListener()`;
- `popularAccordionNav`;
- loaders;
- `DataManager`;
- запись entity;
- commit.

Выбранный раздел получает `iteraction-list-flat-section-active`. Остальные теряют этот класс.

## 5. Quick actions

Controller вызывает:

```java
InteractionService.getMostPolularIteraction(currentUser, 5)
```

и выполняет цикл ровно по пяти индексам.

Реальная кнопка:

- caption = `interaction.iterationName`;
- description = то же значение;
- listener устанавливает точный объект `Iteraction`;
- после установки выполняется focus поля типа.

Placeholder:

- caption `Нет данных`;
- disabled;
- listener отсутствует;
- не изменяет DataContext.

## 6. SCSS

Partial:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-flat-layout.scss
```

обязан быть идентичным для семи тем.

### 6.1. Локальность

Все правила находятся внутри:

```scss
.iteraction-list-editor { ... }
```

Запрещены глобальные переопределения `.v-panel`, `.v-label`, `.v-button`, `.v-tabsheet`.

### 6.2. Карточка

- фон `$v-panel-background-color`;
- border `rgba($v-font-color, 0.15)`;
- radius 8 px;
- базовая тень `0 2px 8px`;
- header с отдельным фоном и нижней границей;
- body с внутренним отступом 14/16 px;
- active border `$v-selection-color`;
- active ring 2 px;
- controls минимум 38 px.

### 6.3. Удалённые компенсации

В актуальном CSS отсутствуют:

- `.v-panel-collapsed` forced visibility;
- `.v-panel-caption` как источник заголовка;
- `.v-panel-content` как body;
- `nth-child(6)` для скрытия legacy navigation;
- принудительное отображение свёрнутого GroupBox.

## 7. Сохранённые бизнес-контракты

Не изменены:

- entity и БД;
- Liquibase;
- containers/loaders;
- JPQL;
- views;
- field bindings;
- picker actions;
- dynamic field rules;
- subscription flow;
- save/cancel;
- Employee side effects;
- vacancy news;
- email/notifications;
- алгоритм ранжирования `InteractionService`.

Изменена только presentation-реализация уже согласованного поведения.

## 8. Проверка точного соответствия

### 8.1. Статика

- XML parse PASS;
- `groupBox` count = 0;
- рабочих VBox-блоков = 4;
- navigation buttons = 4;
- quick-action positions = 5;
- `setExpanded` count в активном controller = 0;
- SCSS-копии семи тем идентичны;
- business component ID сохранены.

### 8.2. Viewport 1700×950

- sidebar и workspace помещаются без horizontal scroll;
- quick actions видимы над формой;
- первая часть всех блоков доступна через vertical scroll;
- footer постоянный;
- active card визуально соответствует navigation.

### 8.3. Viewport 1366×768

- sidebar 296 px;
- status/priority без разрыва значений;
- рабочая область сохраняет полезную ширину;
- footer не перекрывает поля;
- scroll только вертикальный.

### 8.4. Runtime smoke

1. Открыть новую и существующую запись.
2. Проверить четыре постоянно видимых блока.
3. Нажать каждый пункт navigation.
4. Проверить active nav, active card, focus и scroll.
5. Проверить 0/1–4/5+ популярных типов.
6. Проверить dynamic fields.
7. Проверить save/cancel/subscribe.
8. Повторить во всех семи темах.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Устранено расхождение XML/Java/CSS: GroupBox заменён VBox-блоками, expanded state удалён, active-state сделан явным, quick actions стабилизированы до пяти позиций. |
| 2026-07-27 | Статус и приоритет перенесены перед label-navigation; введена плоская двухпанельная компоновка. |
