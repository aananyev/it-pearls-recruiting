# `hunttech_IteractionList.edit` — legacy-спецификация

> Каноническая полная спецификация: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)  
> Общий UI-контракт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Entity: `hunttech_IteractionList`

## 1. Назначение и бизнес-смысл (What & Why)

Экран создаёт или изменяет один факт взаимодействия рекрутёра с кандидатом по вакансии. Запись хранит тип и результат контакта, дополнительные данные, оценку, способ коммуникации и комментарий.

Сохранение может:

- связать запись с предыдущим взаимодействием;
- сохранить снимок состояния вакансии;
- изменить статус кандидата;
- создать или обновить `Employee`;
- создать новость вакансии;
- отправить уведомление;
- открыть подготовку письма кандидату;
- создать подписку на кандидата.

Полная бизнес-логика, lifecycle, запросы и риски описаны в канонической спецификации.

## 2. Фактический runtime-controller

В текущем `master` только `IteractionListEdit` содержит `@UiController("hunttech_IteractionList.edit")` и является реальным screen-controller.

После commit Hermes `078ba63c4577c355142a49fcc31e5e775111a02f` класс `IteractionListEditAccordionNavigation` лишён screen-аннотаций. Он не зарегистрирован и не создаётся явно. Поэтому его four-item label-navigation и `Нет данных` placeholders автоматически не применяются.

Фактический runtime использует базовый navigation-код `IteractionListEdit`:

- четыре видимых рабочих аккордеона;
- пятый legacy-пункт для скрытого `popularAccordion`;
- только фактически найденные быстрые кнопки;
- при результате менее пяти отображается менее пяти кнопок.

## 3. UI Context & Navigation

```text
edit-sidebar
└── images → identity → navigation → service/vacancy context

edit-workspace
└── toolbar → quick actions → accordion scroll → footer
```

Рабочие разделы:

1. кандидат и вакансия;
2. тип взаимодействия;
3. результат;
4. комментарий.

`popularAccordion` скрыт и остаётся compatibility-компонентом.

## 4. Behavior Summary

- новый объект → номер, дата и текущий рекрутёр;
- кандидат → фото и проверка истории;
- вакансия → mismatch, закрытие, подписка и начало процесса;
- новая пара → фильтр типов `001`;
- тип → dynamic fields, required comment и action;
- quick action → точный `Iteraction`;
- before commit → snapshots, chain и Employee side effect;
- after commit → vacancy news;
- commit-and-close → candidate status, notifications и письмо;
- save/cancel → стандартные CUBA actions.

## 5. Быстрые взаимодействия

Источник:

```java
InteractionService.getMostPolularIteraction(userSession.getUser(), 5)
```

Алгоритм:

```text
текущий рекрутёр
→ последний календарный месяц
→ iteractionType is not null
→ group by iteractionType
→ order by count DESC
→ первые пять
```

Активная кнопка замыкает точный объект `Iteraction`. Caption parsing и повторный поиск запрещены.

В текущем runtime placeholders `Нет данных` не создаются, потому что helper с `normalizePopularButtons()` не зарегистрирован.

## 6. Основные поля

| Раздел | Компоненты |
|---|---|
| Кандидат и вакансия | `candidateField`, `vacancyFiels`, `onlyMySubscribeCheckBox` |
| Тип | `iteractionTypeField`, `buttonCallAction`, `addString`, `addDate`, `addInteger` |
| Результат | `ratingField`, `recrutierField`, `communicationMethodField` |
| Комментарий | `commentField` |
| Footer | `subscribeButton`, `windowCommitAndClose`, `windowClose` |

## 7. Кандидат и вакансия

### Кандидат

- фото из `candidate.fileImageFace`;
- возможное копирование предыдущей вакансии;
- warning о недавнем контакте другого рекрутёра.

### Вакансия

- проверка позиции и локации;
- предупреждение о закрытой вакансии;
- первая запись пары ограничивает типы группой `001`;
- Researcher без активной `RecrutiesTasks` получает предложение подписки;
- sidebar показывает project/company/closing/status/alternatives/priority/cost.

## 8. Dynamic fields

| Настройка `Iteraction` | Результат |
|---|---|
| `addFlag=true`, `addType=1` | required `addDate` |
| `addFlag=true`, `addType=2` | required `addString` |
| `addFlag=true`, `addType=3` | required `addInteger` |
| `callForm=true` | `buttonCallAction` |
| `setDateTime=true` | текущая дата в `addDate` |
| `signComment=true` | required comment |

Add-value дописывается в комментарий вместе с названием типа.

## 9. Commit lifecycle

### Before commit

- `comment=null` → `""`;
- `currentPriority` и `currentOpenClose`;
- `chainInteraction`;
- optional create/update `Employee`.

`Employee` коммитится отдельно через `DataManager` до завершения основного commit.

### After commit

`OpenPositionService.setOpenPositionNewsAutomatedMessage()` создаёт новость вакансии.

### Commit-and-close

- префикс `Iteraction.number` → `candidate.status`;
- email активному подписчику;
- `notificationType=6` → `UiNotificationEvent`;
- `needSendLetter=true` → `InternalEmailerEdit`.

## 10. Data и action contracts

Сохранены:

- `iteractionListDc`, `iteractionTypesDc`, `openPositionDc`, `usersDc`;
- `iteractionListDl`, `iteractionTypesLc`, `openPositionsDl`, `usersDl`;
- component ID и bindings;
- lookup/open actions;
- `invoke="callActionEntity"`;
- `invoke="onButtonSubscribeClick"`;
- `windowCommitAndClose`;
- `windowClose`.

## 11. Data View Integrity

Обязательны:

- `iteractionList-edit-view`;
- `jobCandidate-iteraction-list-suggestion-view`;
- `openPosition-iteraction-list-picker-view`;
- `iteraction-list-type-view`;
- `employee-view`;
- `subscribeCandidateAction-view`;
- `ScreenViewIntegrityTest 8/8 PASS`;
- отсутствие unfetched и N+1.

## 12. Технические особенности

- номер — `max + 1`;
- Employee — отдельный commit в `BeforeCommitChangesEvent`;
- notification types 0–5 — no-op;
- `setSubscribe()` и `setCurrentUserName()` — no-op;
- `Iteraction.number` требует числовой префикс;
- `callClass` зависит от runtime meta-class/screen;
- presentation-helper не зарегистрирован.

## 13. SCSS

После исправления Hermes `themes/common` и symlink не используются. Имеются семь реальных theme-local копий:

```text
modules/web/themes/<theme>/com.company.hunttech/edit-screen-shared-styles.scss
```

Они должны меняться синхронно.

- sidebar 270 px;
- viewport `<=1366px` — 250 px;
- workspace без horizontal scroll;
- GroupBox `height=AUTO`;
- local scope `.iteraction-list-editor`.

## 14. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Зафиксирован фактический runtime после `078ba63c...`: базовый `IteractionListEdit` — единственный screen-controller, helper неактивен, placeholders не создаются автоматически. |
| 2026-07-27 | Добавлено краткое описание полной бизнес-логики и lifecycle. |
| 2026-07-27 | Зафиксирована двухпанельная архитектура экрана. |
