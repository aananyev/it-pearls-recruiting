# `hunttech_IteractionList.edit` — legacy-спецификация

> Каноническая UI-spec: [IteractionListEdit_Spec.md](../../ui/IteractionListEdit_Spec.md)  
> Screen ID: `hunttech_IteractionList.edit`  
> Presentation-controller: `IteractionListEditAccordionNavigation`  
> Descriptor: `iteraction-list-edit.xml`

## Назначение и бизнес-смысл (What & Why)

Экран регистрирует взаимодействие рекрутёра с кандидатом по вакансии. Аккордеонная компоновка уменьшает визуальную плотность и оставляет пользователю один активный рабочий контекст, не изменяя бизнес-правила создания и сохранения `IteractionList`.

Пять наиболее частых взаимодействий текущего пользователя являются постоянными быстрыми действиями. Они должны быть видимы внутри вкладки до первого рабочего аккордеона.

## UI Context & Navigation

Экран вызывается прежним screen ID из browse взаимодействий, карточки кандидата и связанных действий. Controller сохраняет все точки открытия, стандартные actions и lifecycle. В левой контекстной панели расположен индекс из пяти визуально LABEL-подобных borderless-кнопок.

## Behavior Summary

- открытие → блок пяти быстрых взаимодействий видим перед первым аккордеоном → «Кандидат и вакансия» раскрыт, остальные рабочие аккордеоны свёрнуты;
- клик по быстрой кнопке → точный `Iteraction` устанавливается в `iteractionTypeField` → штатный value-change handler обновляет зависимые поля;
- клик слева → выбранный presentation-раздел активируется → состояние рабочих аккордеонов и focus синхронизируются;
- раскрытие заголовком → `ExpandedStateChangeListener` синхронизирует левый индекс;
- выбор кандидата, вакансии или типа → выполняются прежние handlers базового controller;
- сохранение и отмена → выполняются прежние actions без вмешательства presentation-слоя.

## Компоновка

0. `mostPopularQuickActions` — постоянно видимый блок пяти частых взаимодействий внутри вкладки.
1. `participantsAccordion` — кандидат и вакансия.
2. `interactionAccordion` — тип и динамическое действие/значение.
3. `resultAccordion` — рейтинг, рекрутёр, способ связи.
4. `commentAccordion` — комментарий.
5. `popularAccordion` — невидимый compatibility-компонент для существующей Java-инъекции и пятого пункта sidebar.

Sidebar сохраняет `OvaFallbackImage candidateImage`, логотип проекта, служебные поля, компанию, проект, статус, приоритет, outstaffing и rating context.

## Сохранённые контракты

- data containers, loaders, JPQL, query conditions и views — без изменений;
- `IteractionListEdit.java` — без изменений;
- `mostPopularIteractionHBox` и `mostPopularHbox` — legacy ID сохранены;
- component ID, bindings, actions, `invoke`, captions существующих полей — без изменений;
- runtime `visible`, `required`, caption и source продолжают задаваться базовым controller;
- footer: subscribe → commit-and-close → cancel;
- entity, БД и Liquibase не изменяются.

## Проверки

Обязательны `IteractionListEditAccordionLayoutTest`, `IteractionListMostPopularInteractionTest`, `IteractionListAccordionNavigationTest`, `ScreenViewIntegrityTest 8/8`, SCSS build семи тем, `clean assemble`, local deploy, HTTP `/hrm/` = 200 и functional/visual smoke по точному HEAD SHA.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Блок пяти быстрых взаимодействий возвращён внутрь вкладки и размещён перед первым аккордеоном; Java-контракт сохранён |
| 2026-07-25 | Добавлены аккордеоны и кликабельная навигация в левой панели; активный раздел, header clicks и фокус синхронизированы без изменения бизнес-логики |
| 2026-07-25 | Профильное изображение кандидата переведено на `OvaFallbackImage` с fallback без изменения legacy-контракта |
| 2026-07-25 | Уточнена двухпанельная компоновка формы |
| 2026-07-25 | Выполнен первоначальный UI/UX-редизайн формы |
