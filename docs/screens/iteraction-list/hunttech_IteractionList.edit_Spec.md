# IteractionList Edit (`hunttech_IteractionList.edit`)

> Каноническая UI-спецификация: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)
> Сущность: [IteractionList.md](../../entities/iteraction-list/IteractionList.md)

## Назначение и бизнес-смысл (What & Why)

Экран фиксирует взаимодействия рекрутёра с кандидатом по вакансии и является транзакционной точкой рекрутинговой воронки HRM HuntTech. Он сохраняет тип взаимодействия, рейтинг, коммуникацию, дополнительное значение и комментарий, а также запускает существующие проверки, подписки, уведомления и обновление статусов.

## UI Context & Navigation

Экран открывается из browse взаимодействий, карточки кандидата и связанных create/edit сценариев. Из него доступны lookup/editor кандидата и вакансии, динамический экран действия, подписка на кандидата, стандартное сохранение и отмена. Подробная карта переходов и component ID поддерживается в канонической спецификации.

## Behavior Summary

- открытие → загрузка `iteractionList-edit-view` и справочников → отображение текущего или нового взаимодействия;
- выбор кандидата/вакансии → штатные listeners Java → прежние проверки и обновление контекста;
- выбор типа взаимодействия → `changeField()` → один из `buttonCallAction`, `addDate`, `addString`, `addInteger`;
- сохранение → BeforeCommit/AfterCommit/BeforeClose → цепочка, статусы, новости и email;
- редизайн 2026-07-25 → меняется только presentation → бизнес- и data-контракты остаются прежними.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_IteractionList.edit` |
| Java-класс | `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit` |
| XML-дескриптор | `iteraction-list-edit.xml` |
| Базовый класс | `StandardEditor<IteractionList>` |
| `@EditedEntityContainer` | `iteractionListDc` |
| Загрузка | `@LoadDataBeforeShow` |
| Диалог | `1000 × 650`, modal |
| Локальный namespace | `.iteraction-list-editor` |

## 2. Связь с моделью данных

| Артефакт | Значение |
|---|---|
| Entity | `IteractionList` |
| View | `iteractionList-edit-view` |
| Containers | `iteractionListDc`, `iteractionTypesDc`, `openPositionDc`, `usersDc` |
| Loaders | `iteractionListDl`, `iteractionTypesLc`, `openPositionsDl`, `usersDl` |
| Properties | `numberIteraction`, `dateIteraction`, `rating`, `candidate`, `vacancy`, `iteractionType`, `addString`, `addDate`, `addInteger`, `communicationMethod`, `recrutier`, `comment` |

JPQL и conditions loaders не изменены редизайном. `comment` остаётся LOB с отдельной загрузкой контроллером.

## 3. Иерархия и взаимосвязь форм

| Связь | Экран / компонент | Способ открытия |
|---|---|---|
| Родитель | `hunttech_IteractionList.browse`, карточка кандидата | create/edit |
| Кандидат | `candidateField` | `picker_lookup`, `picker_open` |
| Вакансия | `vacancyFiels` | `picker_lookup`, `picker_open` |
| Динамический экран | `buttonCallAction` | `invoke="callActionEntity"` |
| Подписка | `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| Сохранение | `windowCommitAndClose` | standard action |
| Отмена | `windowClose` | standard action |

## 4. Модель поведения и интерактивность

### 4.1 Жизненный цикл

- новая entity: номер, дата и рекрутёр заполняются контроллером;
- `BeforeShow`: инициализируются vacancy provider, фильтр подписок, popular interactions и dynamic field;
- `AfterShow`: назначается рекрутёр и lazy-загружается `comment`;
- `BeforeCommit`: сохраняются snapshot приоритета/статуса, chainInteraction и employee state;
- `AfterCommit`: выполняются подписка и news automation;
- `BeforeClose`: обновляется статус кандидата и запускаются прежние email-сценарии.

### 4.2 Динамические поля

`buttonCallAction`, `addString`, `addDate`, `addInteger` находятся в одном presentation-контейнере. XML не задаёт им новые `visible`, `required` и captions. Контроллер продолжает единолично переключать варианты.

### 4.3 Популярные взаимодействия

`mostPopularHbox` и `mostPopularIteractionHBox` сохранены отдельными component ID. Первый наполняется Java-контроллером; порядок и click-логика динамически созданных link buttons не менялись.

## 5. Логика действий

| ID / action | Контракт |
|---|---|
| `buttonCallAction` | `invoke="callActionEntity"` |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| `windowCommitAndClose` | стандартное сохранение и закрытие |
| `windowClose` | стандартная отмена |
| `candidateField.lookup/open` | прежние picker actions |
| `vacancyFiels.lookup/open` | прежние picker actions |
| `iteractionTypeField.lookup` | прежний picker action |

## 6. Визуальная компоновка

### До 2026-07-25

- верхний светлый collapsable groupBox со служебными данными и изображениями;
- отдельный groupBox популярных взаимодействий;
- один TabSheet;
- двухколоночный grid со статусом, кандидатом, вакансией, рейтингом и параметрами;
- комментарий под grid;
- footer действий.

### После 2026-07-25

- toolbar высотой не менее 58 px;
- тёмная контекстная sidebar с кандидатом, проектом, служебными сведениями, статусом, приоритетом и рейтингом;
- светлая workspace с сохранённым TabSheet высотой вкладки 48 px;
- карточка популярных взаимодействий;
- карточка основных полей с прежним `gridIterationData`;
- общий dynamic host для четырёх runtime-вариантов;
- полноширинная карточка комментария;
- footer существующих действий, доступный независимо от прокрутки;
- одинаковый локальный SCSS-контракт для семи тем.

Основные поля не помещены в свёрнутые секции. `dialogMode` не изменён. Общая концепция UI/UX не актуализировалась, поскольку отдельной команды на её изменение не было.

## 7. Контрактная проверка редизайна

| Проверка | Результат |
|---|---|
| Java изменён | NO |
| Бизнес-логика изменена | NO |
| Entity/БД/Liquibase изменены | NO |
| component ID изменены | NO |
| bindings изменены | NO |
| actions/invoke изменены | NO |
| loaders/JPQL/views изменены | NO |
| XML layout изменён | YES |
| Локальный SCSS добавлен | YES |
| Глобальный SCSS добавлен | NO |
| Документация обновлена | YES |
| ScreenViewIntegrityTest | NOT VERIFIED |
| Data View Integrity | NOT VERIFIED |
| SCSS build | NOT VERIFIED |
| clean assemble | NOT VERIFIED |
| visual smoke | NOT VERIFIED |
| Production | NOT CHANGED |
| Merge | NOT PERFORMED |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Форма адаптирована к UI/UX-концепции HRM HuntTech строго на presentation-уровне; добавлен cross-link на каноническую UI-spec, локальный namespace и карта сохранённых контрактов |
| 2026-06-30 | `setClosingDateLabel`: null-guard при сбросе `vacancyFiels` — устранён NPE |
| 2026-06-26 | Поведение Java описано простым языком |
| 2026-06-26 | Добавлен Business & Context Intro |
| 2026-06-26 | Первая версия UI Spec |
