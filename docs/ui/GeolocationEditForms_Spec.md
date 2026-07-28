# Географические Edit-формы HRM HuntTech

> Экраны: `CountryEdit`, `RegionEdit`, `CityEdit`  
> Дата: 2026-07-28

## Назначение и бизнес-смысл (What & Why)

Формы поддерживают единый справочник геолокаций HRM HuntTech: страна содержит регионы, регион относится к стране и содержит города, город относится к региону. Единая Edit-композиция снижает ошибки при ведении адресных данных кандидатов, компаний и вакансий.

## UI Context & Navigation

Экраны открываются из соответствующих browse-справочников стран, регионов и городов. Каждая форма использует обязательную двухпанельную композицию: постоянную sidebar слева и рабочую область справа. `CountryEdit` и `RegionEdit` содержат два navigation-пункта — основные данные и дочернюю коллекцию. `CityEdit` содержит один пункт основных данных и показывает выбранный регион в sidebar.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть форму | editor загружает штатный контейнер | отображаются прежние данные в новой композиции |
| нажать label-навигацию | выбран раздел текущей формы | фокус переводится к первому полю или таблице; entity и loaders не меняются |
| добавить/изменить/удалить регион или город | используется существующая composition-таблица | выполняются прежние table actions |
| сохранить или отменить | нажата штатная footer-кнопка | выполняются прежние `windowCommitAndClose` или `windowClose` |

## Визуальный контракт

- root namespaces: `country-editor`, `region-editor`, `city-editor`;
- общая композиция: `edit-screen-layout`, `edit-sidebar`, `edit-workspace`, `edit-workspace-scroll`;
- toolbar: `edit-toolbar`, `edit-toolbar-title`, `edit-toolbar-subtitle`;
- sidebar: `edit-sidebar-identity`, `edit-sidebar-title`, `edit-sidebar-subtitle`, `edit-sidebar-summary`, `edit-sidebar-hint`;
- navigation: `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`;
- контент: `edit-section-card`;
- действия: `edit-footer-actions`.

## Сохранённые CUBA-контракты

Не изменены entity, views, data containers, properties, options containers, loaders, JPQL, table actions, picker action, validators и save lifecycle. Java-код добавляет только presentation-методы фокусировки и active-state navigation.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Страны, регионы и города приведены к общему контракту Edit-экранов без изменения бизнес-логики. |
