# `hunttech_IteractionList.edit` — legacy-спецификация

> Каноническая UI-spec: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)  
> Общий Edit-стандарт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Presentation-controller: `IteractionListEditAccordionNavigation`

## Назначение и бизнес-смысл (What & Why)

Экран регистрирует взаимодействие рекрутёра с кандидатом по вакансии. Presentation-слой использует общий визуальный API Edit-экранов, но не меняет правила создания, валидации, сохранения, подписок, уведомлений и изменения статусов `IteractionList`.

Пять частых взаимодействий остаются персональным ускорителем. Фактические типы возвращает прежний `InteractionService`; недостающие позиции отображаются disabled.

## UI Context & Navigation

Sidebar сохраняет фотографию кандидата, логотип проекта, имя, вакансию и весь служебный контекст. Runtime-навигация создаётся как пять keyboard-доступных borderless-кнопок с классами `label-nav-item`; активное состояние добавляет только `label-nav-item-active`.

Правая область сохраняет toolbar, постоянную карточку быстрых действий, TabSheet, четыре рабочих аккордеона и footer-actions.

## Behavior Summary

- открытие → базовый контроллер выполняет прежний lifecycle → presentation-extension добавляет semantic stylename;
- клик navigation → раскрывается существующий GroupBox и переводится focus → entity и loaders не меняются;
- клик активной быстрой кнопки → точный `Iteraction` устанавливается в `iteractionTypeField` → прежний handler обновляет зависимые поля;
- отсутствие статистики → отображаются пять disabled-позиций `Нет данных`;
- сохранение и отмена → выполняются прежние CUBA actions.

## Неизменяемый бизнес-контракт

1. `InteractionService.getMostPolularIteraction(currentUser, 5)`.
2. Период — последний календарный месяц.
3. Фильтр — текущий рекрутёр.
4. Группировка по типу, сортировка `count DESC`.
5. Активная кнопка назначает точный `Iteraction`.
6. Entity, JPQL, loaders, views, bindings, validators, actions и `invoke` не изменяются.

## Общие semantic stylename

- `edit-screen-layout`;
- `edit-sidebar` и sidebar-роли;
- `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`;
- `edit-workspace`, `edit-workspace-scroll`, `edit-workspace-content`;
- `edit-toolbar`, `edit-toolbar-title`, `edit-toolbar-description`;
- `edit-card`, `edit-card-title`;
- `edit-tabs`;
- `edit-accordion-section`;
- `edit-footer-actions`.

Локальный namespace `.iteraction-list-editor` сохраняется. Общий SCSS находится в одном файле `modules/web/themes/common/edit-screen-shared-styles.scss` и подключается всеми семью темами.

## Специфика формы

- фотография кандидата остаётся главным образом;
- логотип проекта сохраняется как отдельный контекст размером `80 × 80`;
- карточка быстрых действий постоянно видима между toolbar и TabSheet;
- `participantsAccordion` раскрыт по умолчанию;
- `popularAccordion` остаётся скрытым compatibility-компонентом;
- horizontal form scroll запрещён.

## Проверки

Обязательны профильные contract tests, `ScreenViewIntegrityTest 8/8`, SCSS build семи тем, `clean assemble`, clean local deploy, HTTP `/hrm/` = 200, DOM/visual/functional smoke и проверка Tomcat logs по точному HEAD SHA.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | `IteractionListEdit` приведён к общему контракту Edit-экранов без изменения бизнес-логики и CUBA-контрактов |
| 2026-07-27 | Восстановлены пять постоянно видимых быстрых позиций и месячный контракт сервиса |
| 2026-07-25 | Выполнен первоначальный двухпанельный UI/UX-редизайн |
