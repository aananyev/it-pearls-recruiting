# `hunttech_IteractionList.edit` — legacy-спецификация

> Каноническая спецификация: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)
> Общий UI-контракт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)
> Screen ID: `hunttech_IteractionList.edit`

## 1. Назначение и бизнес-смысл (What & Why)

Экран создаёт или изменяет один `IteractionList`: факт взаимодействия текущего рекрутёра с кандидатом по вакансии. Он сохраняет тип и результат контакта, дополнительные данные, оценку, канал коммуникации и комментарий, а также участвует в цепочке статусов, подписок, уведомлений и автоматических действий HRM HuntTech.

Визуальный рефакторинг не меняет entity, сервисы, БД, loaders, JPQL, views, bindings, validation или стандартный lifecycle CUBA Platform 7.3.

## 2. UI Context & Navigation

Экран продолжает открываться по legacy ID из browse взаимодействий, карточки кандидата, сценариев создания, редактирования и копирования.

Новая композиция:

```text
edit-sidebar
└── identity → label-navigation → context

edit-workspace
└── toolbar → five quick actions → accordion scroll → footer
```

В sidebar четыре navigation-пункта, соответствующие четырём реальным рабочим GroupBox. Пять быстрых действий находятся вне аккордеонов и всегда видимы.

## 3. Behavior Summary

- новый объект → номер, дата и рекрутёр задаются прежним controller → открыт раздел кандидата и вакансии;
- клик navigation → выбранный GroupBox раскрывается → focus переходит в первое поле;
- ручное раскрытие GroupBox → active-state navigation синхронизируется;
- статистика пользователя содержит менее пяти типов → оставшиеся позиции disabled `Нет данных`;
- клик активной кнопки → точный `Iteraction` устанавливается в `iteractionTypeField` → штатные dynamic handlers выполняются;
- save/cancel → стандартные CUBA actions сохраняются без самописного commit.

## 4. Неизменяемый контракт быстрых взаимодействий

Источник:

```java
InteractionService.getMostPolularIteraction(userSession.getUser(), 5)
```

Алгоритм реализации 2024 года:

```text
текущий рекрутёр
→ последний календарный месяц
→ group by iteractionType
→ count
→ order by count DESC
→ первые пять типов
```

Запрещены UI JPQL, `DataManager`-агрегация, caption parsing и повторный поиск типа. Listener активной кнопки замыкает точный `Iteraction`.

## 5. Рабочие разделы

| Раздел | Компоненты |
|---|---|
| Кандидат и вакансия | `candidateField`, `vacancyFiels`, `onlyMySubscribeCheckBox` |
| Тип и действие | `iteractionTypeField`, `buttonCallAction`, `addString`, `addDate`, `addInteger` |
| Результат | `ratingField`, `recrutierField`, `communicationMethodField` |
| Комментарий | `commentField` |

`popularAccordion` остаётся невидимым compatibility-компонентом только из-за legacy-инъекции базового controller. Он не является пользовательским разделом.

## 6. Component и action contracts

Сохранены:

- `iteractionListDc`, `iteractionTypesDc`, `openPositionDc`, `usersDc`;
- `iteractionListDl`, `iteractionTypesLc`, `openPositionsDl`, `usersDl`;
- `candidateImage`, `projectLogoImage`;
- все field ID и property bindings;
- picker lookup/open actions;
- `invoke="callActionEntity"`;
- `invoke="onButtonSubscribeClick"`;
- `action="windowCommitAndClose"`;
- `action="windowClose"`.

## 7. Data View Integrity

Обязательны:

- `iteractionList-edit-view`;
- `jobCandidate-iteraction-list-suggestion-view`;
- `openPosition-iteraction-list-picker-view`;
- `iteraction-list-type-view`;
- `ScreenViewIntegrityTest 8/8 PASS`;
- отсутствие `Cannot get unfetched attribute` и N+1.

## 8. Изображения

- `candidateImage`: `OvaFallbackImage`, 112 px, fallback `icons/no-programmer.jpeg`;
- `projectLogoImage`: `OvaFallbackImage`, 80 px, fallback `icons/no-company.png`;
- физические пути FileStorage не используются;
- project logo не конкурирует с фотографией кандидата.

## 9. Responsive и SCSS

- sidebar: `270px`;
- viewport `<=1366px`: `250px`;
- workspace: `min-width: 0`, `overflow-x: hidden`;
- GroupBox: `height=AUTO`;
- единственный source shared styles: `themes/common/edit-screen-shared-styles.scss`;
- семь theme-local импортов реализованы symbolic links на общий source;
- локальные правила ограничены `.iteraction-list-editor`.

## 10. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Зафиксирована новая from-scratch архитектура экрана, четыре navigation-раздела, пять постоянных quick-action позиций и единый shared SCSS source. |
| 2026-07-27 | Экран приведён к общему Edit-стандарту HRM HuntTech. |
