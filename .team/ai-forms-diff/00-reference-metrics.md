# Эталонные метрики IteractionListEdit для CDP-сверки (computed-стили)

Источник: `modules/web/themes/hover/com.company.hunttech/iteraction-list-editor.scss` и `iteraction-list-visual-alignment.scss` (компилируются во все 7 тем).

## Sidebar
- фон: `rgb(23, 38, 56)` → градиент 180deg `#172638 0% → #132130 58% → #0f1b28 100%`
- ширина в SCSS: `272px !important` (XML 312px — перекрывается SCSS)
- padding: `14px 12px`; border-right `rgba(15,23,42,0.78)`; box-shadow `5px 0 20px rgba(15,23,42,0.18)`
- overflow-y: auto

## Label-навигация (visual-alignment)
- пункт: `min-height 27px`, padding `3px 10px`, font 13px/600, color `rgba(248,250,252,0.82)`, border-left 3px transparent, radius `0 5px 5px 0`
- hover: белый `#ffffff` на `rgba(255,255,255,0.08)`
- active: `#ffb11b` на `rgba(255,177,27,0.12)`, border-left-color `#ffb11b`
- полоса-заголовок (navigation-title): min-height 36px, padding `7px 11px`, color `#ffb11b`, 15px/700, bg `rgba(255,255,255,0.045)`, border-bottom `rgba(255,255,255,0.14)`, box-shadow `rgba(255,255,255,1) 0 1px 0 0 inset, rgba(244,244,244,1) 0 -1px 0 0 inset`
- caption внутри пункта: `display:block; width:100%; color:inherit`

## Toolbar
- min-height 52px, padding `9px 18px 8px`, bg panel, border-bottom `rgba($v-font-color,0.16)`
- title: 19px/700, `mix($v-font-color, $v-panel-background-color, 92%)`
- description/context: 12px, `mix(... 60%)`

## Карточки (edit-card / popular-card)
- radius 9px, border `rgba($v-font-color,0.15)`, bg panel, shadow `0 2px 8px rgba(15,23,42,0.05)`
- caption: min-height 50px (эталон edit-card §5.3), padding `12px 16px`, 17px/700, bg `mix($v-app-background-color, $v-panel-background-color, 68%)`, border-bottom
- content padding `11-17px`

## Поля (edit-form-control)
- textfield/textarea/filterselect: min-height 38px, font 15px, bg panel, border `rgba($v-font-color,0.24)`, radius `$v-border-radius` (~4-5px), box-shadow none
- hover border `rgba($v-font-color,0.42)`; focus border `$v-selection-color` + `0 0 0 2px rgba($v-selection-color,0.18-0.20)`
- caption (подпись поля): 13px/600, `mix($v-font-color, $v-panel-background-color, 72-82%)`, line-height 17.55px

## Кнопки
- min-height 38px, padding `0 14-16px`, font 14px, radius 5px (эталон кнопок форм)

## Прочее
- caption sidebar-секций: 10.5px/700 uppercase, `rgba(248,250,252,0.62)`, letter-spacing 0.04em
- sidebar value: 13px/500 `#f8fafc`, line-height 18px

## CDP-критерии (приемлемый допуск ±1px)
1. `.edit-sidebar` computed background-color = rgb(23,38,56); width ≈ 272px
2. `.label-nav-item` computed min-height = 27px; font-size 13px; font-weight 600
3. `.label-nav-item-active` color = rgb(255,177,27); background ≈ rgba(255,177,27,0.12)
4. полоса `.label-navigation .{form}-navigation-title` min-height 36px, color rgb(255,177,27), font-size 15px, font-weight 700
5. `.edit-card .v-panel-caption` font-size 17px, font-weight 700, min-height 50px
6. `.edit-card .v-textfield` min-height 38px, font-size 15px
7. `.edit-card .v-caption .v-captiontext` font-size 13px, font-weight 600
8. footer-кнопки: min-height 38px, font-size 14px
