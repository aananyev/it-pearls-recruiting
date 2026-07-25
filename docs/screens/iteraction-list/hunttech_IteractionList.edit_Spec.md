# `hunttech_IteractionList.edit` — legacy-спецификация

> Каноническая UI-spec: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Presentation-controller: `IteractionListEditAccordionNavigation`  
> Descriptor: `iteraction-list-edit-accordion-navigation.xml`

## Назначение и бизнес-смысл (What & Why)

Экран регистрирует взаимодействие рекрутёра с кандидатом по вакансии. Аккордеонная компоновка уменьшает визуальную плотность и оставляет пользователю один активный рабочий контекст, не изменяя бизнес-правила создания и сохранения `IteractionList`.

## UI Context & Navigation

Экран вызывается прежним screen ID из browse взаимодействий, карточки кандидата и связанных действий. Новый controller наследует `IteractionListEdit`, поэтому все точки открытия, стандартные actions и lifecycle сохраняются. В левой контекстной панели расположен индекс из пяти визуально LABEL-подобных borderless-кнопок.

## Behavior Summary

- открытие → раскрывается «Кандидат и вакансия» → остальные аккордеоны свёрнуты;
- клик слева → выбранный раздел раскрывается → остальные сворачиваются, active style и фокус синхронизируются;
- раскрытие заголовком → `ExpandedStateChangeListener` синхронизирует левый индекс;
- выбор кандидата, вакансии или типа → выполняются прежние handlers базового controller;
- сохранение и отмена → выполняются прежние actions без вмешательства presentation-controller.

## Компоновка

1. `participantsAccordion` — кандидат и вакансия.
2. `interactionAccordion` — тип и динамическое действие/значение.
3. `resultAccordion` — рейтинг, рекрутёр, способ связи.
4. `commentAccordion` — комментарий.
5. `popularAccordion` — частые взаимодействия.

Sidebar сохраняет `OvaFallbackImage candidateImage`, логотип проекта, служебные поля, компанию, проект, статус, приоритет, outstaffing и rating context.

## Сохранённые контракты

- data containers, loaders, JPQL, query conditions и views — без изменений;
- `IteractionListEdit.java` — без изменений;
- component ID, bindings, actions, `invoke`, captions существующих полей — без изменений;
- runtime `visible`, `required`, caption и source продолжают задаваться базовым controller;
- footer: subscribe → commit-and-close → cancel;
- entity, БД и Liquibase не изменяются.

## Проверки

Обязательны `IteractionListAccordionNavigationTest`, `ScreenViewIntegrityTest 8/8`, Data View Integrity, SCSS build семи тем, `clean assemble`, local deploy, HTTP `/hrm/` = 200 и functional/visual smoke по точному HEAD SHA.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Добавлены аккордеоны и кликабельная навигация в левой панели; активный раздел, header clicks и фокус синхронизированы без изменения бизнес-логики |
| 2026-07-25 | Профильное изображение кандидата переведено на `OvaFallbackImage` с fallback без изменения legacy-контракта |
| 2026-07-25 | Уточнена двухпанельная компоновка формы |
| 2026-07-25 | Выполнен первоначальный UI/UX-редизайн формы |
