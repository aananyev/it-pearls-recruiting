# AiFunctionConfigurationEdit vs IteractionListEdit — таблица отличий (30)

Дата: 2026-08-12 (повторный детальный дифф по запросу: «найди 30 отличий, приведи в соответствие»).
Метод: CDP-замеры computed-стилей и геометрии обеих форм (Chrome 151, viewport 1600×950, тема hover).

Сравнивался фактический рендер формы «Конфигурация AI-функции»
(`ai-function-configuration-edit.xml` + `ai-function-configuration-editor.scss`)
с эталоном `IteractionListEdit` (`iteraction-list-edit.xml`, `iteraction-list-*.scss`, accordion-navigation/flat-layout/reference-finish).

Стиль: `<эталон> → <форма>`.

| # | Элемент | Эталон (computed) | Форма (было) | Правка |
|---|---------|-------------------|--------------|--------|
| 1 | Sidebar title font-size | 18px | 19px | 18px |
| 2 | Sidebar title color | #ffb11b (жёлтый) | #ffffff (белый) | #ffb11b |
| 3 | Sidebar title line-height | 24px | 26px | 24px |
| 4 | Sidebar subtitle font-size | 12px | 11px | 12px |
| 5 | Sidebar subtitle font-weight | 400 | 700 | 400 |
| 6 | Sidebar subtitle color | rgba(248,250,252,0.72) | rgba(248,250,252,0.62) | 0.72 |
| 7 | Sidebar subtitle line-height | 17px | 16px | 17px |
| 8 | Sidebar subtitle letter-spacing | normal | 0.5px | normal |
| 9 | Sidebar subtitle text-transform | none | uppercase | none |
| 10 | Toolbar title font-size | 20px (accordion-navigation:363) | 19px (shared) | 20px |
| 11 | Toolbar desc color | mix 60% → rgb(133,141,149) | mix 62% → rgb(129,137,145) | mix 60% |
| 12 | Toolbar desc line-height | 18px (accordion-navigation:368) | 20px (shared) | 18px |
| 13 | Toolbar border-bottom | rgba(52,66,79,0.16) | rgba(52,66,79,0.15) | 0.16 |
| 14 | Toolbar высота | 66px | 68px | следствие №10 |
| 15 | Карточка margin-bottom | 12px | 10px | 12px |
| 16 | Checkbox padding | 3px 8px | 3px 0 | 3px 8px |
| 17 | Textarea line-height | 21.75px (valo 1.45×15px) | normal | 21.75px |
| 18 | Textarea padding | 4px | 4px 10px | 4px |
| 19 | Footer padding | 11px 20px (accordion-navigation:610) | 10px 18px (shared) | 11px 20px |
| 20 | Footer min-height | 62px | 58px | 62px |
| 21 | Footer border-top | rgba(52,66,79,0.16) | rgba(52,66,79,0.15) | 0.16 |
| 22 | Footer box-shadow | 0 -2px 8px rgba(15,23,42,0.04) | none | тень вверх |
| 23 | Footer-кнопки min-height | 40px (accordion-navigation:615) | 38px | 40px |
| 24 | Footer-кнопки padding | 0 18px | 0 16px | 0 18px |
| 25 | Footer-кнопки border-radius | 4px (valo $v-border-radius) | 5px | 4px |
| 26 | Footer-кнопки font-weight | 600 (accordion-navigation:618) | 400 (default) | 600 |
| 27 | OK-кнопка фон | primary rgb(77,122,178), текст #fff | белый фон, тёмный текст (c-primary-action не стилизован) | primary-action |
| 28 | Отмена-кнопка фон | transparent | белый фон | secondary-action (transparent) |
| 29 | Выравнивание кнопок footer | правый нижний угол (x=1373/1461) | слева (x=590) | expand+spacer+group → вправо |
| 30 | Hover/focus кнопок footer | hover filter brightness(0.98); focus ring 2px rgba(selection,.2) | нет | добавлены |

Идентичные блоки (PASS, не входили в diff): sidebar 312px/padding/фон/тень; полоса «Разделы» (15px/700/#ffb11b/36px/inset-линии); nav-пункты (27px/13px/600, active жёлтый); поля 38px/15px/radius 5px; подписи полей 13px/600; caption карточек 17px/700/min-height 50px; workspace фон rgb(232,237,243).

Не изменялось: бизнес-логика, Java-контроллер, entity, БД, эталон. Раскладка полей в карточках (столбик) сохранена намеренно (принята ранее; не входит в «оформление»).
