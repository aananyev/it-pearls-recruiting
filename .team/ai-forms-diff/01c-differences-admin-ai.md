# AdminAiConfigurationEdit vs IteractionListEdit — таблица отличий (30)

Дата: 2026-08-12. Метод: CDP-замеры computed-стилей обеих форм (Chrome 151, viewport 1600×950, тема hover).

Сравнивался фактический рендер формы «Корпоративное AI-подключение»
(`admin-ai-configuration-edit.xml` + `admin-ai-configuration-editor.scss`)
с эталоном `IteractionListEdit`.

Партиал формы до правок был побайтово идентичен партиалу AiFunctionConfigurationEdit до правок
(отличие только в namespace `admin-ai-configuration-*`), поэтому найденные отличия — тот же набор из 30 пунктов,
что задокументирован в `01b-differences-ai-function.md`:

1. Sidebar title font-size 18px vs 19px
2. Sidebar title color #ffb11b vs #ffffff
3. Sidebar title line-height 24px vs 26px
4. Sidebar subtitle font-size 12px vs 11px
5. Sidebar subtitle font-weight 400 vs 700
6. Sidebar subtitle color rgba(248,250,252,0.72) vs rgba(248,250,252,0.62)
7. Sidebar subtitle line-height 17px vs 16px
8. Sidebar subtitle letter-spacing normal vs 0.5px
9. Sidebar subtitle text-transform none vs uppercase
10. Toolbar title font-size 20px vs 19px
11. Toolbar desc color mix 60% vs mix 62%
12. Toolbar desc line-height 18px vs 20px
13. Toolbar border-bottom rgba(52,66,79,0.16) vs rgba(52,66,79,0.15)
14. Toolbar высота 66px vs 68px (следствие №10)
15. Карточка margin-bottom 12px vs 10px
16. Checkbox padding 3px 8px vs 3px 0
17. Textarea line-height 21.75px vs normal
18. Textarea padding 4px vs 4px 10px
19. Footer padding 11px 20px vs 10px 18px
20. Footer min-height 62px vs 58px
21. Footer border-top rgba(52,66,79,0.16) vs rgba(52,66,79,0.15)
22. Footer box-shadow 0 -2px 8px rgba(15,23,42,0.04) vs none
23. Footer-кнопки min-height 40px vs 38px
24. Footer-кнопки padding 0 18px vs 0 16px
25. Footer-кнопки border-radius 4px vs 5px
26. Footer-кнопки font-weight 600 vs 400
27. OK-кнопка фон primary rgb(77,122,178)/белый текст vs белый фон
28. Отмена-кнопка фон transparent vs белый
29. Выравнивание footer-кнопок: правый нижний угол (x~1461) vs слева (x~590)
30. Hover/focus кнопок footer (brightness 0.98 / focus ring 2px) отсутствовали

Исправлено в XML (`admin-ai-configuration-edit.xml`): footer-паттерн `expand=editActionsSpacer` + `editActionsGroup`
(AUTO, MIDDLE_RIGHT), stylename `admin-ai-configuration-primary-action` / `admin-ai-configuration-secondary-action`.
Исправлено в SCSS (`admin-ai-configuration-editor.scss`, 7 тем, sha256-идентичен): все 30 значений — по эталону
(см. `01b-differences-ai-function.md` для значений; namespace заменён на `admin-ai-configuration-*`).

CDP-сверка после деплоя: 9/9 групп PASS, footer-кнопки идентичны эталону (40px/0 18px/600/4px, OK primary,
Отмена transparent, x=1383/1461), password-поле `apiKeyInput` стилизовано как поле эталона (38px/15px/radius 5px).

Не изменялось: бизнес-логика (AiCredentialService middleware encryption), Java-контроллер, entity, БД, эталон.
