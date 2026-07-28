# Стандарт документирования XML-экранов HRM HuntTech

> Платформа: CUBA Platform 7.3  
> Область: XML-дескрипторы экранов, окон и UI-фрагментов  
> Нормативное правило: `.cursor/rules/xml-screen-documentation.mdc`  
> Статус: обязательный стандарт

## Назначение и бизнес-смысл (What & Why)

XML-дескрипторы CUBA одновременно задают структуру экрана, привязки данных, loaders, JPQL-условия, actions и геометрию интерфейса. Без inline-документации назначение отдельных элементов приходится восстанавливать по Java-controller, views и runtime-поведению, что повышает риск случайно изменить business contract при визуальном рефакторинге.

Стандарт делает XML самодокументируемым: разработчик должен видеть не только тип компонента, но и его роль в бизнес-сценарии, источник данных, причину layout-группировки и ограничения CUBA 7.3.

## UI Context & Navigation

Стандарт применяется ко всем UI descriptor в `modules/web/src/**`, включая Browse, Edit, View, Lookup, Window, Fragment, login, main screen, settings и dashboard XML.

Inline-комментарии работают совместно с `docs/ui/{FormName}_Spec.md`:

- XML объясняет назначение каждого конкретного элемента рядом с кодом;
- UI Spec описывает экран целиком: входы, связи, lifecycle, business behavior, data binding и visual layout;
- entity documentation фиксирует место экрана в подсистеме сущности.

Ни один из этих уровней не заменяет другой.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| создать новый XML-экран | descriptor отсутствовал | каждый открывающий элемент сразу получает смысловой комментарий, создаётся UI Spec |
| изменить существующий XML | правится хотя бы один элемент или атрибут | проводится аудит всего descriptor, а не только diff |
| добавить loader/query/condition | экрану нужны данные или фильтр | комментарий объясняет источник, параметры и ограничение выборки |
| добавить field/action/button | пользователь вводит данные или запускает действие | комментарий фиксирует binding и пользовательский результат |
| выполнить comment-only задачу | структура экрана не должна измениться | XML после удаления comments структурно эквивалентен исходному |
| завершить задачу | comments и UI Spec синхронизированы | выполняются Data View Integrity, тесты, сборка и smoke Hermes |

## 1. Обязательный триггер

Правило срабатывает при любом создании, изменении, исправлении, рефакторинге или удалении:

- `*-browse.xml`;
- `*-edit.xml`;
- `*-view.xml`;
- `*-lookup.xml`;
- `*-window.xml`;
- `*-fragment.xml`;
- любого другого XML, который загружается CUBA как UI descriptor.

Первое изменение legacy descriptor означает аудит всего файла.

## 2. Обязательное покрытие комментариями

Комментарий размещается непосредственно перед каждым открывающим XML-элементом:

- root и data layer;
- containers/loaders/query conditions;
- layout hierarchy;
- rows/columns;
- fields, labels, images и tables;
- actions, buttons, validators и formatters;
- custom UI components.

Не требуют отдельного комментария XML declaration, closing tags, CDATA content, namespaces и attributes одного тега.

## 3. Требования к смыслу комментария

Комментарий должен объяснять хотя бы один аспект:

1. бизнес-задачу элемента;
2. binding и сохраняемое свойство;
3. источник options/data;
4. loader/query parameters;
5. пользовательское действие и результат;
6. причину layout-группировки;
7. CUBA limitation или предотвращаемый runtime-риск;
8. причину `required`, `visible`, `editable`, `enabled` или динамического состояния.

Рекомендуемый формат:

```xml
<!-- LookupPickerField выбирает вакансию из openPositionDc и сохраняет связь в vacancy текущего IteractionList. -->
<lookupPickerField id="vacancyField" ...>
```

## 4. Запрещённые формулировки

Не допускаются:

- `Элемент vbox`;
- `Поле label`;
- `Кнопка button`;
- описание только направления layout без причины;
- `TODO`, `TBD`, `потом описать`;
- комментарии, скопированные перед разными элементами;
- утверждения, которых нет в XML/Java;
- перевод или переименование legacy identifier.

## 5. Синхронизация внешней документации

В той же задаче обновляется `docs/ui/{FormName}_Spec.md`:

- What & Why;
- UI Context & Navigation;
- Behavior Summary;
- containers, loaders, views и JPQL;
- bindings и actions;
- layout и stylename;
- lifecycle, если XML влияет на поведение;
- история изменений, новая дата первой строкой.

Новый Spec регистрируется в `docs/ui/README.md` и профильном README.

## 6. Защита business contracts

Inline-документирование не является разрешением менять:

- component ID;
- порядок элементов;
- bindings;
- loaders и JPQL;
- views;
- actions и `invoke`;
- required/visible/editable/enabled;
- геометрию и stylename;
- Java-controller и lifecycle.

Comment-only diff проверяется нормализацией XML после удаления comments.

## 7. Проверки и Definition of Done

Для каждого затронутого descriptor обязательны:

- XML parse PASS;
- semantic comments coverage PASS;
- Data View Integrity PASS;
- обновлённый XML contract test;
- синхронизированный UI Spec и history;
- `ScreenViewIntegrityTest 8/8 PASS`;
- `BUILD SUCCESSFUL`;
- при SCSS-изменениях — сборка всех тем;
- Hermes local deploy, HTTP `/hrm/` = 200 и browser smoke.

XML-задача без полного аудита comments и UI Spec считается незавершённой.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Введён обязательный проектный стандарт документирования каждого элемента создаваемого или изменяемого XML-дескриптора. |
