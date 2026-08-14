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
| открыть форму | editor загружает штатный контейнер | отображаются прежние данные в новой композиции; sidebar показывает статичную круглую иллюстрацию страны/региона/города 176×176 (ovalImage, theme-ресурс `icons/dictionaries/*.png`) и живой title из свойства контейнера |
| нажать label-навигацию | выбран раздел текущей формы | фокус переводится к первому полю или таблице; entity и loaders не меняются |
| добавить/изменить/удалить регион или город | используется существующая composition-таблица | выполняются прежние table actions |
| сохранить или отменить | нажата штатная footer-кнопка | выполняются прежние `windowCommitAndClose` или `windowClose` |

## Визуальный контракт

- root namespaces: `country-editor`, `region-editor`, `city-editor`;
- общая композиция: `edit-screen-layout`, `edit-sidebar` (270px), `edit-workspace`, `edit-workspace-scroll`;
- toolbar: `edit-toolbar`, `edit-toolbar-title`, `edit-toolbar-description`;
- sidebar: `edit-sidebar-visual` (круглая иллюстрация `ovalImage` 176×176, stylename `dictionary-logo-image`, theme-ресурс `icons/dictionaries/country.png`/`region.png`/`city.png` — без привязки к данным), `edit-sidebar-identity`, `edit-sidebar-title`, `edit-sidebar-subtitle`, `edit-sidebar-summary`, `edit-sidebar-hint`;
- navigation: `label-navigation`, `label-nav-title geolocation-navigation-title` (полоса-заголовок «Разделы» с двумя inset-линиями, контракт §4.1), `label-nav-item`, `label-nav-item-active`;
- контент: `edit-card` (карточки разделов «Основные данные» и дочерней коллекции, `showAsPanel="true"` — рендер Vaadin Panel, чтобы заголовок `.v-panel-caption` получал контрактный стиль);
- поля: `edit-form-control` на каждом `TextField`/`LookupPickerField` с caption;
- таблицы: captions колонок через mainMsg-ключи `msgRegionRuName`/`msgRegionCode`/`msgCityRuName`/`msgCityPhoneCode`;
- действия: `edit-footer-actions`;
- локальный SCSS-слой: `geolocation-edit-forms.scss` (идентичная копия во всех 7 темах) — фирменная тёмная sidebar `#172638 → #132130 → #0f1b28`, каноническая label-навигация по эталону IteractionListEdit (hover — белый на `rgba(255,255,255,.08)`, active — `#ffb11b` на `rgba(255,177,27,.12)` с жёлтой левой границей), визуальный блок и круглый аватар 176×176 с `object-fit: contain`.

## Сохранённые CUBA-контракты

Не изменены entity, views, data containers, properties, options containers, loaders, JPQL, table actions, picker action, validators и save lifecycle. Java-код добавляет только presentation-методы фокусировки и active-state navigation. Контрактный тест `GeolocationEditFormsContractTest` защищает presentation-слой (включая идентичность локального SCSS во всех темах), `GeolocationEditFormsDetachedObjectTest` — detached-сценарии.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-13 | В sidebar country/city/region добавлен визуальный блок с круглой статичной иллюстрацией `ovalImage` 176×176 (theme-ресурс `icons/dictionaries/country.png`/`region.png`/`city.png`, stylename `dictionary-logo-image`) — эталон JobCandidateEdit; тексты identity выровнены по контракту: title 18px/700 `#ffb11b`, subtitle 12px/400 `rgba(248,250,252,.72)`; в `geolocation-edit-forms.scss` (7 тем) добавлены стили `.edit-sidebar-visual` и `.dictionary-logo-image`; контрактный тест `DictionaryEditFormIllustrationContractTest` проверяет наличие ovalImage 176px и theme-ассетов 200×200 во всех темах. |
| 2026-08-09 | Заголовок «Разделы» навигации оформлен полосой-заголовком с двумя горизонтальными inset-линиями (контракт §4.1, класс `geolocation-navigation-title` поверх `label-nav-title`, #ffb11b 15px/700, min-height 36px); карточки получили `showAsPanel="true"` — заголовок `.v-panel-caption` 17px/700/min-height 50px с фоном mix(68%) и разделителем (до этого CUBA-рендер `c-groupbox-caption` не матчил SCSS-правила, заголовок оставался вало-дефолтом 11px/400); проверки добавлены в `GeolocationEditFormsContractTest`. |
| 2026-07-31 | Правая рабочая область гео-форм приведена к эталону IteractionListEdit: карточки `.edit-card` (groupBox) — фон, рамка, радиус 8px, заголовок `.v-groupbox-caption`/`.v-panel-caption` 17px/700, поля 38px с рамкой и focus-кольцом `$v-selection-color`, подписи 13px/600, кнопки 38px/radius 5px; проверки добавлены в контрактный тест. |
| 2026-07-31 | Локальный SCSS-слой `geolocation-edit-forms.scss` во всех 7 темах: фирменная тёмная sidebar и каноническая label-навигация (эталон IteractionListEdit); подключение import+include в styles.scss каждой темы; проверка идентичности и канонических цветов в контрактном тесте. |
| 2026-07-31 | Второй проход контракта: sidebar 280px→270px; мёртвые классы `edit-section-card`/`edit-toolbar-subtitle` заменены на общие `edit-card`/`edit-toolbar-description`; всем полям добавлен `stylename="edit-form-control"` и captions (регион/город); колонки таблиц получили mainMsg-captions; добавлены ключи `msgRegionRuName`, `msgCityRuName`, `msgCityPhoneCode`, `msgCityRegion` в web messages; создан `GeolocationEditFormsContractTest`; XML-дескрипторы полностью inline-документированы. |
| 2026-07-28 | Страны, регионы и города приведены к общему контракту Edit-экранов без изменения бизнес-логики. |
