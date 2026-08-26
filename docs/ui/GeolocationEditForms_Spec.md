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
- sidebar: `edit-sidebar-visual` (круглая иллюстрация `ovalImage` 176×176, stylename `dictionary-logo-image`, theme-ресурс `icons/dictionaries/country.png`/`region.png`/`city.png` — без привязки к данным), `edit-sidebar-identity`, `edit-sidebar-title` (18px/700 `#ffb11b`, **по центру по горизонтали** — `text-align: center`, эталон ProjectEdit), `edit-sidebar-subtitle`, `edit-sidebar-summary`, `edit-sidebar-hint`; стандартные отступы контента sidebar от краёв — `padding: 14px 16px 12px`, правая граница `rgba(15,23,42,.78)` и внешняя тень `5px 0 20px rgba(15,23,42,.18)` (эталон ProjectEdit/IteractionListEdit); у всех трёх форм подпись типа записи в identity убрана — остаётся только название страны/региона/города (у `CityEdit` сохраняется сводка `edit-sidebar-summary` с подписью «Регион»);
- navigation: `label-navigation`, `label-nav-title geolocation-navigation-title` (полоса-заголовок «Разделы» с двумя inset-линиями, контракт §4.1), `label-nav-item`, `label-nav-item-active`; nav-пункты — ровно 27px (фикс высоты Vaadin-кнопки `.v-button-label-nav-item` + flex-wrap центрирование, эталон IteractionListEdit);
- контент: `edit-card` (карточки разделов «Основные данные» и дочерней коллекции, `showAsPanel="true"` — рендер Vaadin Panel, чтобы заголовок `.v-panel-caption` получал контрактный стиль);
- поля: `edit-form-control` на каждом `TextField`/`LookupPickerField` с caption;
- таблицы: captions колонок через mainMsg-ключи `msgRegionRuName`/`msgRegionCode`/`msgCityRuName`/`msgCityPhoneCode`;
- действия: `edit-footer-actions` — панель в правом нижнем углу (expand + `align="MIDDLE_RIGHT"`), кнопки 40px/14px/600/radius 4px; у всех трёх форм пара ОК/Отмена получает стили primary/secondary (`{form}-editor-primary-action` — заливка `$v-selection-color`, `{form}-editor-secondary-action` — прозрачная с рамкой; `country-editor-*`/`region-editor-*`/`city-editor-*`), как у формы Project (`project-editor-primary-action`/`project-editor-secondary-action`);
- локальный SCSS-слой: `geolocation-edit-forms.scss` (идентичная копия во всех 7 темах) — фирменная тёмная sidebar `#172638 → #132130 → #0f1b28`, каноническая label-навигация по эталону IteractionListEdit (hover — белый на `rgba(255,255,255,.08)`, active — `#ffb11b` на `rgba(255,177,27,.12)` с жёлтой левой границей), визуальный блок и круглый аватар 176×176 с `object-fit: contain`.

## Сохранённые CUBA-контракты

Не изменены entity, views, data containers, properties, options containers, loaders, JPQL, table actions, picker action, validators и save lifecycle. Java-код добавляет только presentation-методы фокусировки и active-state navigation. Контрактный тест `GeolocationEditFormsContractTest` защищает presentation-слой (включая идентичность локального SCSS во всех темах), `GeolocationEditFormsDetachedObjectTest` — detached-сценарии.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-22 | CountryEdit: Изображение флага переведено в прямоугольный формат (200×120px, `.country-flag-rectangular-image`), проверено отсутствие лишних кнопок улучшения |
| 2026-08-14 | Серия гео-форм завершена: RegionEdit и CityEdit получили те же правки sidebar, что и CountryEdit — подпись типа записи (`msg://msgRegion`/`msg://msgCity`) убрана из identity, название региона/города по центру по горизонтали (общий `text-align: center` уже действовал), footer-кнопки ОК/Отмена прижаты к правому нижнему углу (expand + `MIDDLE_RIGHT`) со стилем primary/secondary (`region-editor-primary-action`/`region-editor-secondary-action`, `city-editor-primary-action`/`city-editor-secondary-action`; SCSS-селекторы объединены для трёх форм); у CityEdit сводка `edit-sidebar-summary` с подписью «Регион» сохранена; проверки добавлены в `GeolocationEditFormsContractTest` |
| 2026-08-14 | CountryEdit sidebar доведён до эталона ProjectEdit: стандартные отступы контента sidebar (`padding: 14px 16px 12px` + правая граница + внешняя тень — на `.edit-sidebar`, слот остаётся только с шириной), подпись типа записи «Страна» (`edit-sidebar-subtitle`) удалена, название страны `edit-sidebar-title` выровнено по центру по горизонтали (`text-align: center`), footer-кнопки ОК/Отмена прижаты к правому нижнему углу (expand + `MIDDLE_RIGHT`) и стилизованы как у формы Project (`country-editor-primary-action`/`country-editor-secondary-action`, 40px/14px/600/radius 4px); правки в общем `geolocation-edit-forms.scss` (7 тем, md5=1) + проверки в `GeolocationEditFormsContractTest` |
| 2026-08-14 | Sidebar гео-форм (Country/Region/CityEdit) скорректирован по контракту Edit-экранов (эталон IteractionListEdit): nav-пункты зафиксированы ровно 27px (фикс высоты Vaadin-кнопки `.v-button-label-nav-item` + flex-wrap `.v-button-wrap` + отключение `:before` halo-трюка в общем `geolocation-edit-forms.scss`; до фикса пункты раздувались до 46px), подписи identity переведены на msg-ключи (`msg://msgCountry`/`msg://msgRegion`/`msg://msgCity`, у города также подпись сводки `msg://msgRegion`), spacer получил `width/height 100%`; проверки добавлены в `GeolocationEditFormsContractTest` (фикс высоты + локальные msg-ключи) |
| 2026-08-13 | В sidebar country/city/region добавлен визуальный блок с круглой статичной иллюстрацией `ovalImage` 176×176 (theme-ресурс `icons/dictionaries/country.png`/`region.png`/`city.png`, stylename `dictionary-logo-image`) — эталон JobCandidateEdit; тексты identity выровнены по контракту: title 18px/700 `#ffb11b`, subtitle 12px/400 `rgba(248,250,252,.72)`; в `geolocation-edit-forms.scss` (7 тем) добавлены стили `.edit-sidebar-visual` и `.dictionary-logo-image`; контрактный тест `DictionaryEditFormIllustrationContractTest` проверяет наличие ovalImage 176px и theme-ассетов 200×200 во всех темах. |
| 2026-08-09 | Заголовок «Разделы» навигации оформлен полосой-заголовком с двумя горизонтальными inset-линиями (контракт §4.1, класс `geolocation-navigation-title` поверх `label-nav-title`, #ffb11b 15px/700, min-height 36px); карточки получили `showAsPanel="true"` — заголовок `.v-panel-caption` 17px/700/min-height 50px с фоном mix(68%) и разделителем (до этого CUBA-рендер `c-groupbox-caption` не матчил SCSS-правила, заголовок оставался вало-дефолтом 11px/400); проверки добавлены в `GeolocationEditFormsContractTest`. |
| 2026-07-31 | Правая рабочая область гео-форм приведена к эталону IteractionListEdit: карточки `.edit-card` (groupBox) — фон, рамка, радиус 8px, заголовок `.v-groupbox-caption`/`.v-panel-caption` 17px/700, поля 38px с рамкой и focus-кольцом `$v-selection-color`, подписи 13px/600, кнопки 38px/radius 5px; проверки добавлены в контрактный тест. |
| 2026-07-31 | Локальный SCSS-слой `geolocation-edit-forms.scss` во всех 7 темах: фирменная тёмная sidebar и каноническая label-навигация (эталон IteractionListEdit); подключение import+include в styles.scss каждой темы; проверка идентичности и канонических цветов в контрактном тесте. |
| 2026-07-31 | Второй проход контракта: sidebar 280px→270px; мёртвые классы `edit-section-card`/`edit-toolbar-subtitle` заменены на общие `edit-card`/`edit-toolbar-description`; всем полям добавлен `stylename="edit-form-control"` и captions (регион/город); колонки таблиц получили mainMsg-captions; добавлены ключи `msgRegionRuName`, `msgCityRuName`, `msgCityPhoneCode`, `msgCityRegion` в web messages; создан `GeolocationEditFormsContractTest`; XML-дескрипторы полностью inline-документированы. |
| 2026-07-28 | Страны, регионы и города приведены к общему контракту Edit-экранов без изменения бизнес-логики. |
