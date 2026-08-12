# Таблица отличий: IteractionListEdit (эталон) → AiFunctionConfigurationEdit (целевая форма)

Дата: 2026-08-12. Задача: приведение 4 Edit-форм AI-конфигурации к визуальному эталону IteractionListEdit.

Источники эталона:
- `modules/web/themes/hover/com.company.hunttech/iteraction-list-editor.scss` (642 строки)
- `modules/web/themes/hover/com.company.hunttech/iteraction-list-visual-alignment.scss` (662 строки)
- `modules/web/themes/hover/com.company.hunttech/iteraction-list-flat-layout.scss`, `-accordion-navigation.scss`, `-reference-finish.scss`
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`
- Эталонные метрики для CDP-сверки: `00-reference-metrics.md`

Целевая форма: `modules/web/src/com/company/hunttech/web/screens/aifunctionconfiguration/ai-function-configuration-edit.xml` (root `ai-function-configuration-editor`).

Аналогичные отличия применимы к `AdminAiConfigurationEdit`, `UserAiFunctionOverrideEdit` (частично совпадающая структура) и к `UserAiConfigurationEdit` (legacy-диалог 450px, отличия которого перечислены отдельно в конце).

---

## 1. Sidebar

| № | Аспект | Эталон (IteractionListEdit) | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|------------------------------|------------------------------------|---------------------|
| 1 | Локальный namespace sidebar | `.iteraction-list-sidebar` + `.edit-sidebar`, все правила scoped под `.iteraction-list-editor` | Только общий `.edit-sidebar`; локального SCSS для формы нет вообще | Создать локальный partial `.ai-function-configuration-editor` с собственными правилами sidebar |
| 2 | Фон sidebar | Тёмный градиент `#172638 0% → #132130 58% → #0f1b28 100%` (background-color + background-image раздельно) | Светлый shared-фон (тема), нет тёмной палитры | `background-color: #172638 !important` + `background-image: linear-gradient(...) !important` |
| 3 | Ширина sidebar (SCSS) | `272px !important` (слот `312px !important` — см. visual-alignment) | XML `width="312px"`, но shared `edit-sidebar` перекрывает до `270px !important` | Согласовать slot и sidebar локально (shared-контракт 270px, как у vacancy-prompt-template); слот и корень не должны расходиться |
| 4 | Внутренние отступы sidebar | `padding: 14px 12px` (flat-layout: `14px 16px 12px`) | Нет локального padding (shared не задаёт) | Локальный padding в partial формы |
| 5 | Цвет текста в sidebar | `#f8fafc` (наследуется контентом) | Наследует `$v-font-color` темы | Локальные цвета контента sidebar `#f8fafc` / `rgba(248,250,252,…)` |
| 6 | Правая граница и тень sidebar | `border-right: 1px solid rgba(15,23,42,0.78)` + `box-shadow: 5px 0 20px rgba(15,23,42,0.18)` | Нет | Локальные border/shadow |
| 7 | Типографика `edit-sidebar-title` | `#ffffff`, 19px/700, line-height 26px, перенос `overflow-wrap:anywhere` | Shared: `display:block`, без цвета/размера на тёмном фоне | Локальные правила title/subtitle/hint |
| 8 | Типографика `edit-sidebar-subtitle` | `rgba(248,250,252,0.62)`, 11px/700 uppercase, letter-spacing 0.5px | Shared: без тёмной адаптации | Локальные правила |
| 9 | Типографика `edit-sidebar-hint` | 12px, `rgba(248,250,252,0.55)`, line-height 18px | Shared: серый по светлой теме | Локальные правила |
| 10 | Вертикальный ритм sidebar | `> .v-slot { width:100%; min-width:0 }`, spacing контролируется | Shared: `width:100%` есть; тёмная тема не учитывается | Локальные правила ширины слотов |

## 2. Навигация (label-navigation)

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 11 | Полоса-заголовок «Разделы» | `iteraction-list-navigation-title` поверх `label-nav-title`: min-height 36px, padding `7px 11px`, `#ffb11b` 15px/700, bg `rgba(255,255,255,.045)`, `border-bottom rgba(255,255,255,.14)`, inset-линии `box-shadow: rgba(255,255,255,1) 0 1px 0 0 inset, rgba(244,244,244,1) 0 -1px 0 0 inset` (контракт §4.1) | Только общий `label-nav-title` (11px/700 uppercase, opacity .66) — полосы-заголовка нет | В XML: `stylename="label-nav-title ai-function-configuration-navigation-title"`; в SCSS: scoped-правило полосы |
| 12 | Высота пункта навигации | `min-height: 27px !important`, `height: auto`, padding `3px 10px` | Shared: `min-height: 24px`, padding `3px 10px` | Локально 27px (канон эталона) |
| 13 | Типографика пункта | 13px/600, `rgba(248,250,252,0.82)`, line-height 20px, `text-align:left` | Shared: 13px/600, `color:inherit`, opacity .78 | Локально цвет `rgba(248,250,252,0.82)` и полная геометрия |
| 14 | Hover пункта | Белый `#ffffff` на `rgba(255,255,255,0.08)` | Shared: `$v-selection-color` (синий) на `rgba($v-selection-color,.06)` | Локально белый на полупрозрачном белом |
| 15 | Active пункта | `#ffb11b` на `rgba(255,177,27,0.12)`, `border-left-color: #ffb11b` | Shared: `$v-selection-color` на `rgba($v-selection-color,.08)` | Локально жёлтый канон |
| 16 | Скругление пункта | `border-radius: 0 5px 5px 0` | Shared: `border-radius: 0` | Локально `0 5px 5px 0` |
| 17 | Левая граница-маркер | `border-left: 3px solid transparent` у всех пунктов, жёлтая у active | Shared: есть `border-left: 3px solid transparent` | Оставить (совпадает), активная — жёлтая |
| 18 | Контейнер навигации | padding `10px 0 4px`, `border-top: 1px solid rgba(255,255,255,0.16)`, `background: transparent` | Shared: padding `10px 0 4px`, `border-top: rgba($v-font-color,0.14)` | Локально белая border-top на тёмном фоне |
| 19 | Псевдоэлемент `:before` пункта (вало-трюк центрирования) | Скрыт: `display:none !important; content:none !important` | Не скрыт — при display:flex выталкивает caption вниз | Локально скрыть `:before` |
| 20 | Caption пункта-кнопки | `display:block; width:100%; color:inherit; text-align:left; white-space:normal` | Shared: есть (совпадает) | Оставить (совпадает) |
| 21 | Текст заголовка навигации | msg-ключ `msgAccordionNavigation` («Разделы формы») | Hardcoded «Разделы» | Оставить hardcoded (как в sibling-формах), стиль — полоса-заголовок |
| 22 | Active-пункт при открытии | Java `updateActiveNavigation`/`addStyleName` | Static `label-nav-item-active` в XML + Java `setActiveNavigation` (оба используют один класс — конфликта нет) | Не трогать Java; статичный класс оставить |

## 3. Toolbar

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 23 | Высота toolbar | `.iteraction-list-toolbar` min-height 52px, padding `9px 18px 8px` | Shared `edit-toolbar`: min-height 58px, padding `10px 20px` | Оставить shared (контракт 5.1; эталонные 52px — локальная геометрия IteractionListEdit) |
| 24 | Фон и разделитель toolbar | bg panel + `border-bottom rgba($v-font-color,.16)` + тень | Shared: bg panel + `border-bottom rgba(.15)` + тень | Совпадает (shared) |
| 25 | Заголовок toolbar | 19px/700, `mix($v-font-color, $v-panel-background-color, 92%)` | Shared `edit-toolbar-title` — идентично | Совпадает |
| 26 | Описание toolbar | 12px, mix 60% | Shared `edit-toolbar-description` mix 62% | Совпадает (допуск) |
| 27 | Контейнер заголовков | `iteraction-list-toolbar-title-box` + expand | `toolbarText` без `expand`, `width=100%` | Оставить (визуально эквивалентно) |

## 4. Карточки (edit-card)

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 28 | Структура карточки | VBox-секции `iteraction-list-flat-section` + header (46px) + body | `groupBox showAsPanel="true"` `edit-card` | Структура валидна (showAsPanel=true есть); нужен только SCSS |
| 29 | Внутренние отступы карточки | Секции `padding: 0` (header/body управляют отступами) | Shared `edit-card`: `padding: 16px 20px` — caption «плавает» внутри | Локально `padding: 0 !important; overflow: hidden` |
| 30 | Radius карточки | 8–9px (`flat-section` 8px, `card` 9px) | Shared 8px | Совпадает (8px); при необходимости 9px |
| 31 | Рамка и тень карточки | `border: 1px solid rgba($v-font-color,0.15)` + `box-shadow: 0 2px 8px rgba(15,23,42,0.05)` | Shared — идентично | Совпадает |
| 32 | Заголовок карточки (v-panel-caption) | min-height 50px, padding `12px 16px`, 17px/700, bg `mix($v-app-background-color,$v-panel-background-color,68%)`, border-bottom | Shared `.edit-card` НЕ стилизует `.v-panel-caption` (стилизован только `.edit-accordion-section`) → заголовок по-вало | Локально `.edit-card .v-panel-caption` (и `.v-groupbox-caption`) |
| 33 | Контент карточки (v-panel-content) | bg panel, `border: 0`, без лишних отступов | Shared `.edit-card` padding 16px 20px (нежелательно при caption) | Локально `background: $v-panel-background-color !important; border: 0` |
| 34 | Интервал между карточками | `margin: 0 0 12px` между секциями | `spacing="true"` на `sections` (Vaadin v-spacing 10px) | Совпадает по духу; локальный margin не требуется |
| 35 | Активная секция | `iteraction-list-flat-section-active`/`:focus-within`: border `$v-selection-color` + ring | Нет аналога | Не требуется (нет аккордеон-логики) |
| 36 | GridLayout внутри карточек | slot padding `6px 8px` / `8px 10px`, `vertical-align:top` | Форма не использует grid (вертикальный поток в groupBox) | Не требуется |
| 37 | Заголовок карточки «Основное» | — | `caption="Основное"` hardcoded | Оставить (атрибут caption groupBox) |

## 5. Поля (edit-form-control)

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 38 | Покрытие `edit-form-control` | 7/7 полей (включая textArea commentField) | 11/15: descriptionField (textArea), adminConfigurationField (lookupPickerField), systemPromptField, promptTemplateField — БЕЗ класса | Добавить `edit-form-control` на все 4 |
| 39 | Высота полей | 38px (`min-height: 38px !important`, line-height 38px) | Поля без `edit-form-control` — дефолт вало 28px | Класс + shared-правила |
| 40 | Шрифт полей | 15px | Без класса — дефолт 13px | Класс + shared-правила |
| 41 | Рамка полей | `border: 1px solid rgba($v-font-color,0.20)`, radius 5px, `box-shadow:none` | Без класса — дефолт вало | Класс + shared-правила |
| 42 | Подписи (caption) полей | 13px/600, `mix($v-font-color,$v-panel-background-color,72%)` | У 15 полей caption НЕТ вообще (атрибут отсутствует) | Добавить `caption="msg://…"` каждому полю + ключи в messages |
| 43 | Focus-кольцо | `border-color: $v-selection-color` + `box-shadow: 0 0 0 2px rgba($v-selection-color,0.18–0.20)` | Без класса — дефолт вало | Класс + shared-правила; локально ring `.20` |
| 44 | Hover полей | `border-color: rgba($v-font-color,0.42)` | Без класса — дефолт | Класс + shared-правила |
| 45 | Readonly-состояние | bg `mix($v-app-background-color,$v-panel-background-color,62–72%)`, opacity 1 | Без класса — дефолт | Класс + shared-правила |
| 46 | Error-состояние | `border-color: #d9534f` + ring rgba(217,83,79,.14) | Без класса — дефолт | Класс + shared-правила |
| 47 | Кнопка picker (lookup) | 38×38px (`width/min/max 38px`, padding 0) | adminConfigurationField без класса — кнопка дефолт 28px | Класс + shared-правила |
| 48 | Область icon picker | icon 20×20 absolute + `padding-left: 40px` у input | Без класса — нет резерва | Класс + shared-правила |
| 49 | `required` индикатор | `#d9534f` | Есть required у полей; цвет дефолт | Локально цвет `#d9534f` |
| 50 | Строки полей в карточке | caption → поле вертикально, зазор 0 (без margin-bottom) | После добавления caption — shared-геометрия | Следить, чтобы не появился margin-bottom (эталон: его нет) |

## 6. Кнопки

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 51 | Высота кнопок форм | min-height 38px, padding `0 14–16px`, 14px | Shared `edit-workspace .v-button` min-height 38px | Совпадает (shared) |
| 52 | Radius кнопок | 5px | Shared valo | Локально 5px для `.edit-card .v-button` |
| 53 | Primary-кнопка сохранения | `iteraction-list-primary-action`: белый текст на `$v-selection-color` | Стандартная (без stylename) | Решено: shared-стиль + min-height 38px; цветовое выделение primary не входит в объём задачи (XML-инструкция перечисляет только navigation-title/edit-form-control/caption) |
| 54 | Secondary-кнопка отмены | `iteraction-list-secondary-action`: прозрачный фон | Стандартная | Решено: shared-стиль + min-height 38px (аналогично п.53) |
| 55 | Focus-кольцо кнопок | `0 0 0 2px rgba($v-selection-color,0.20)` | Дефолт | Локально |
| 56 | Nav-кнопки sidebar | `borderless label-nav-item` (27px) | `borderless label-nav-item` (shared 24px) | Локально 27px (см. п.12) |

## 7. Footer

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 57 | Контейнер footer | `.iteraction-list-footer` min-height 54px, padding `8px 16px`, bg panel, border-top, тень `0 -2px 8px` | Shared `edit-footer-actions` min-height 58px, padding `10px 18px`, без тени | Оставить shared (контракт) |
| 58 | Структура footer | spacer + group + subscribe/primary/secondary | `editActions` без spacer, align MIDDLE_RIGHT, 2 кнопки | Оставить (структура валидна; контракт 5.1) |
| 59 | Кнопки footer | min-height 38px, padding `0 16px`, 14px | Shared 38px | Совпадает |
| 60 | Закрепление footer | Вне scrollBox, всегда виден | Вне scrollBox (в workspace после scrollBox) | Совпадает |

## 8. Чекбоксы

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 61 | Кастомный чекбокс-фильтр | `.iteraction-list-subscription-filter`: input absolute 18px, label padding-left 28px, line-height 20px | Нет аналога (обычные checkBox) | Не требуется (специфика эталона) |
| 62 | Caption чекбокса | `caption="msg://msgOnlyMySubscribe"` | `activeField`, `allowModelOverrideField` — БЕЗ caption | Добавить `caption="msg://…"` |
| 63 | Типографика label чекбокса | 13–14px, `mix($v-font-color,$v-panel-background-color,78–82%)`, line-height 1.4 | Дефолт вало | Локально `.edit-card .v-checkbox label` |
| 64 | Вертикальный padding чекбокса | `padding: 3px 0` | Дефолт | Локально |
| 65 | `edit-form-control` на чекбоксе | НЕ назначается (контракт: поля ввода, не checkbox) | Не назначен | Не назначать (корректно) |

## 9. textArea

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 66 | Класс textArea | `edit-form-control` на commentField + height 170px rows 7 | descriptionField (rows 3), systemPromptField (rows 6), promptTemplateField (rows 10) — БЕЗ класса | Добавить `edit-form-control` всем трём |
| 67 | Геометрия textArea | min-height 150px (LOB), font 15px, line-height 1.45, `resize: vertical` | Дефолт вало (нет единого контракта) | Класс + shared-правила `.edit-form-control .v-textarea` |
| 68 | Внутренние отступы textArea | padding `8px 10px` | Дефолт | Класс + shared-правила |
| 69 | Рамка/radius textArea | `border: 1px solid rgba($v-font-color,0.20)`, radius 5px | Дефолт | Класс + shared-правила |
| 70 | Focus textArea | ring `rgba($v-selection-color,.20)` | Дефолт | Класс + shared-правила |

## 10. Общие

| № | Аспект | Эталон | AiFunctionConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|------------------------------------|---------------------|
| 71 | Локальный SCSS | 5 partial (642+662+348+671+410 строк) во всех 7 темах, sha256 идентичны | НЕТ НИ ОДНОГО partial (`grep` по themes пусто) | Создать `ai-function-configuration-editor.scss` ×7 тем |
| 72 | Подключение partial | import + include в styles.scss всех 7 тем | Отсутствует | Добавить import+include во все 7 тем |
| 73 | Root-класс формы | `.iteraction-list-editor` — единый namespace | `.ai-function-configuration-editor` есть в XML | Использовать его в partial |
| 74 | dialogMode | 100%×100% modal | 100%×100% modal | Совпадает |
| 75 | Фон workspace | `mix($v-app-background-color,$v-panel-background-color,82%)` | Shared edit-workspace — идентично | Совпадает |
| 76 | ScrollBox | `overflow-x: hidden`, без горизонтального скролла | Shared edit-workspace-scroll | Совпадает |
| 77 | Комментарии XML | Русские смысловые перед каждым элементом (xml-screen-documentation.mdc) | Комментарии на английском, частично отсутствуют | Переписать на русский перед каждым элементом |
| 78 | Caption-ключи | msg-ключи для подписей и заголовков | В messages только browseCaption/editorCaption | Добавить caption-ключи в messages.properties/messages_ru.properties |
| 79 | Тёмная адаптация sidebar-текстов | sidebar-caption 10.5px/700 uppercase rgba(.62), value 13px/500 #f8fafc | Нет sidebar-блоков значений (только identity+hint) | Стилизовать identity/hint на тёмном фоне |
| 80 | Быстрые действия | Карточка quick-actions с 5 кнопками | Нет (не применимо) | Не требуется |

---

## Отдельно: UserAiConfigurationEdit (legacy-диалог 450px)

| № | Аспект | Эталон | UserAiConfigurationEdit (факт) | Требуемое изменение |
|---|--------|--------|--------------------------------|---------------------|
| 81 | Композиция | Двухпанельная sidebar+workspace | Однооконный `<form>` 450px | Полная перестройка по образцу vacancy-prompt-template-edit.xml |
| 82 | dialogMode | 100%×100% modal | `450 × AUTO, forceDialog="true"` | 100%×100% modal |
| 83 | Sidebar | Тёмный, identity+nav+hint | Нет | Создать (270px, как у sample) |
| 84 | label-навигация | Полоса-заголовок «Разделы» + пункты | Нет | Создать `user-ai-configuration-navigation-title` |
| 85 | Карточки | edit-card showAsPanel="true" | Нет (поля прямо в form) | 2 карточки «Основное» / «Безопасность» |
| 86 | edit-form-control | На всех полях | 0 полей с классом | Добавить всем 4 полям |
| 87 | Caption полей | msg-ключи | Нет caption | Добавить msg-ключи |
| 88 | Toolbar | edit-toolbar с заголовком | Нет | Создать |
| 89 | Footer | edit-footer-actions | Простой hbox кнопок | edit-footer-actions |
| 90 | Стили | Локальный partial ×7 тем | Нет | Создать `user-ai-configuration-editor.scss` ×7 тем |

Итого: **90 пунктов** (80 по AiFunctionConfigurationEdit + 10 по UserAiConfigurationEdit), из них по AiFunctionConfigurationEdit — 80 ≥ 50 требуемых.
