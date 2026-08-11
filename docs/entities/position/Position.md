# Position — должность

> Справочник должностей с LOB-описаниями.
> Оптимизация: 2026-06-23. Фикс UNFETCHED ATTRIBUTE ACCESS: 2026-08-11.

## Назначение и Бизнес-смысл (What & Why)

Должность (`Position`) — справочник профессиональных позиций рекрутинга HRM HuntTech: названия на русском и английском (используются в вакансиях и резюме) плюс два LOB-поля со стандартным описанием должности (`standartDescription`) и описанием «кто это» (`whoIsThisGuy`) для подсказок рекрутеру. Справочник небольшой по объёму и часто читается из форм вакансий (`OpenPosition.positionType`).

## Связи в интерфейсе и Навигация (UI Context & Navigation)

- **PositionBrowse** (`hunttech_Position.browse`) — список должностей, открывается из меню справочников; в таблице два text-столбца (`positionRuName`, `positionEnName`) и две icon-колонки, показывающие наличие LOB-описаний.
- **PositionEdit** (`hunttech_Position.edit`) — диалог 600×800, открывается из Browse по кнопке edit/create; два текстовых поля названий и два `richTextArea` для LOB-описаний.
- Внешние потребители: `OpenPosition.positionType` (pick через `position-picker-view`), REST-публичные view.

## Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие Browse → загрузка списка через `position-browse-view` (без LOB) → в `PostLoad` LOB-поля подтягиваются одним scalar-запросом в кэш → icon-колонки показывают «текст есть/нет» без чтения TOAST.
- Открытие Edit → loader загружает должность с `position-edit-view`, расширенным inline LOB-полями (`standartDescription`, `whoIsThisGuy`) → richTextArea получают значения сразу из контейнера.
- Сохранение → стандартный commit `StandardEditor`; LOB сохраняются вместе с названиями.
- Заголовок формы (`textPositionName`) формируется как «en - ru» при открытии и при изменении любого из полей названий.

## 4. Представления

| View | Назначение |
|------|------------|
| `position-browse-view` | Browse без LOB (иконки наличия — через batch scalar-кэш в `PositionBrowse`) |
| `position-edit-view` | Edit-базовый view без LOB; на экране расширяется inline LOB-полями (`<view extends="position-edit-view">` + `standartDescription`, `whoIsThisGuy`) |
| `position-picker-view` | FK (`OpenPosition.positionType` и др.) |

## 5. Экраны

- **PositionBrowse:** batch-кэш `standartDescription` / `whoIsThisGuy` для иконок (scalar `loadValues` в `PostLoadEvent`), LOB не читаются с entity.
- **PositionEdit:** LOB-поля декларированы inline в view контейнера `positionDc`; `onBeforeShow` только формирует заголовок (reload+setter на detached удалён — ловушка UNFETCHED ATTRIBUTE ACCESS).

## 9. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-11 | Fix UNFETCHED ATTRIBUTE ACCESS: `PositionEdit.onBeforeShow` больше не вызывает `setStandartDescription`/`setWhoIsThisGuy` на detached-объекте; LOB-поля задекларированы inline в view контейнера `positionDc` (Localized In-Screen View Extension); добавлен регрессионный тест `PositionServiceTest.testEditViewLoadsLobFields` |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-23 | PositionEdit: исправлен lazy LOB reload — отдельные `.add()` для `standartDescription` и `whoIsThisGuy` |
| 2026-06-23 | browse/edit/picker views, batch LOB Browse, PositionServiceTest |
