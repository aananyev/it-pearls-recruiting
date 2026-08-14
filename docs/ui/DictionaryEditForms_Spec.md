# Справочные Edit-формы HRM HuntTech (серия Dictionary)

> Экраны: `FileTypeEdit`, `SocialNetworkTypeEdit`, `GradeEdit`, `CurrencyEdit`, `OutstaffingRatesEdit`, `EmployeeWorkStatusEdit`, `SignIconsEdit`, `SpecialisationEdit`, `OwnershupEdit`, `PositionEdit`
> Дата: 2026-08-13

## Назначение и бизнес-смысл (What & Why)

Серия из десяти Edit-форм обслуживает базовые справочники HRM HuntTech: типы файлов (категоризация документов и вложений), типы социальных сетей (название, URL и логотип контактов кандидатов), грейды (уровни квалификации и вилки оплаты), валюты (зарплатные вилки и финансовые расчёты), рейты по аутстафу (тарифная шкала ступеней с маржинальностью на уровне БД), статусы сотрудника (принадлежность к штату), иконки признаков (font-маркеры типов признаков в профилях), специализации кандидатов (направление работы и связанные кандидаты), формы собственности (организационно-правовой статус компаний) и должности (ru/en-наименования с LOB-описаниями для вакансий). Единая композиция форм снижает когнитивную нагрузку рекрутера при ведении справочников и соответствует общему контракту Edit-экранов HRM HuntTech.

## UI Context & Navigation

Все формы открываются из соответствующих browse-справочников (create/edit). Каждая использует обязательную двухпанельную композицию: постоянная sidebar 270px слева и рабочая область справа. `FileType`, `SocialNetworkType`, `Grade`, `Currency`, `EmployeeWorkStatus` — одна карточка «Основные данные» и один пункт навигации. `OutstaffingRatesEdit` — две карточки («Ставки», «Комментарий») и два пункта навигации; `SignIconsEdit` — две карточки («Иконка», «Описание») и два пункта навигации; `PositionEdit` — две карточки («Наименование», «Описание») и два пункта навигации; `SpecialisationEdit` — TabSheet с двумя вкладками («Специализация», «Кандидаты») и навигация по вкладкам. Единственная форма с загрузкой изображения — `SocialNetworkTypeEdit` (upload логотипа в sidebar); остальные девять показывают статичную круглую иллюстрацию формы в `OvalImage` 176×176 (theme-ресурс `icons/dictionaries/{form}.png`).

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть форму | editor загружает штатный контейнер | отображаются прежние данные в новой композиции; sidebar показывает статичную круглую иллюстрацию формы 176×176 (ovalImage, theme-ресурс `icons/dictionaries/{form}.png`) и живой title из свойства контейнера |
| нажать label-навигацию | выбран раздел текущей формы | фокус переводится к первому полю секции; активный пункт получает `label-nav-item-active`; entity и loaders не меняются; у `SpecialisationEdit` пункты переключают вкладки TabSheet |
| загрузить логотип соцсети | файл выбран в `snLogoFileUpload` | файл сохраняется в `logo` (IMMEDIATE) и сразу показывается в `snLogo`; при отсутствии файла — fallback-логотип |
| сохранить или отменить | нажата штатная footer-кнопка | выполняются прежние `windowCommitAndClose` или `windowClose`; для статуса сотрудника — защита `inStaff` от null; для иконки — сохранение текущего пользователя и транслитерация `titleEnd` |

## Визуальный контракт

- root namespaces: `file-type-editor`, `social-network-type-editor`, `grade-editor`, `currency-editor`, `outstaffing-rates-editor`, `employee-work-status-editor`, `sign-icons-editor`, `specialisation-editor`, `ownershup-editor`, `position-editor`;
- общая композиция: `edit-screen-layout`, `edit-sidebar` (270px), `edit-workspace`, `edit-workspace-scroll`;
- toolbar: `edit-toolbar`, `edit-toolbar-title`, `edit-toolbar-description`;
- sidebar: `edit-sidebar-visual`, `edit-sidebar-identity`, `edit-sidebar-title`, `edit-sidebar-subtitle`, `edit-sidebar-hint`;
- иллюстрация: `ovalImage` 176×176 (`ovalWidth/ovalHeight=176px`, `scaleMode=SCALE_DOWN`, `<theme path="icons/dictionaries/{form}.png"/>`, stylename `dictionary-logo-image`) — у девяти форм без загрузки; у соцсети — `ovaFallbackImage` с `dataContainer=socialNetworkTypeDc property=logo` и upload `snLogoFileUpload`;
- navigation: `label-navigation`, `label-nav-title dictionary-navigation-title` (полоса-заголовок «Разделы» с двумя inset-линиями, контракт §4.1), `label-nav-item`, `label-nav-item-active`;
- контент: `edit-card` (карточки разделов, `showAsPanel="true"` — рендер Vaadin Panel, чтобы заголовок `.v-panel-caption` получал контрактный стиль); у `SpecialisationEdit` — `tabSheet` со stylename `edit-tabs` внутри карточки;
- поля: `edit-form-control` на каждом `TextField`/`LookupPickerField`/`TextArea` с caption; captions через msg-ключи локального messages-пакета формы;
- действия: `edit-footer-actions`;
- режим: `dialogMode height="100%" width="100%" modal="true"` (полноэкранный модальный редактор справочника);
- локальный SCSS-слой: `dictionary-edit-forms.scss` (идентичная копия во всех 7 темах) — фирменная тёмная sidebar `#172638 → #132130 → #0f1b28`, каноническая label-навигация по эталону IteractionListEdit (hover — белый на `rgba(255,255,255,.08)`, active — `#ffb11b` на `rgba(255,177,27,.12)` с жёлтой левой границей), круглый аватар 176×176 с `object-fit: contain` и кнопки upload соцсети.

## Сохранённые CUBA-контракты

Не изменены entity, views, data containers (`fileTypeDc`, `socialNetworkTypeDc`, `gradeDc`, `currencyDc`, `outstaffingRatesDc` + `currenciesDc`, `employeeWorkStatusDc`, `signIconsDc`), properties, options containers, loaders, JPQL (`select e from hunttech_Currency e`), валидатор `StringValidator` иконки, invoke `selectIconButtonInvoke`, обработчики контроллеров и save lifecycle (`BeforeCommitChanges` статуса и иконки). Java-код добавляет только presentation-методы фокусировки (`focusMainSection`/`focusRatesSection`/`focusCommentSection`/`focusIconSection`/`focusDescriptionSection`) и active-state navigation. Контрактный тест `DictionaryEditFormsContractTest` защищает presentation-слой (включая идентичность локального SCSS во всех темах и отсутствие upload вне соцсети), `DictionaryEditFormsDetachedObjectTest` — detached-сценарии с штатными view форм.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-14 | `SpecialisationEdit` («Карточка специализации»): sidebar приведён к канону серии 2026-08-14 — удалена подпись типа записи из identity (осталось живое название по центру, как в ProjectEdit/гео-формах); контент sidebar получил отступы 14/16/12 + border-right `rgba(15,23,42,0.78)` + тень `5px 0 20px rgba(15,23,42,0.18)`; название по центру (`text-align: center`); тонкий скроллбар при переполнении (`scrollbar-width: thin` + webkit-стили, эталон ProjectEdit/OpenPositionEdit); SCSS точечно под `.specialisation-editor` в `dictionary-edit-forms.scss` (7 тем, md5=1); контрактный тест `DictionaryEditSidebarRedesignContractTest` дополнен ассертами (нет subtitle, SCSS-канон) |
| 2026-08-13 | Чекбоксы 7 форм серии (в т.ч. «В штате» `inStaffField` у EmployeeWorkStatusEdit) переведены на общие стили темы CUBA Platform (Valo): из локального партиала `dictionary-edit-forms.scss` (7 тем) удалена кастомная стилизация квадратика/подписи (`padding: 3px 0` + подпись 14px/1.4) — штатные отступы темы исключают наезд чекбокса на элементы под ним; добавлен контрактный тест `DictionaryEditFormsCheckboxContractTest` |
| 2026-08-13 | Создание Spec серии: 7 справочных Edit-форм приведены к общему контракту Edit-экранов (sidebar 270px + edit-card + label-nav с полосой-заголовком `dictionary-navigation-title`, `showAsPanel=true`); добавлена штатная заглушка-логотип `OvaFallbackImage` 176×176 с fallback `icons/hunttech-logo.png` (для `SocialNetworkTypeEdit` — реальный логотип с upload); локальный SCSS `dictionary-edit-forms.scss` во всех 7 темах; caption-ключи в локальных messages-пакетах; тесты `DictionaryEditFormsContractTest` и `DictionaryEditFormsDetachedObjectTest`. |
