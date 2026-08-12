# QA-вердикт: Раунд 4 (финальная приёмка) 4 Edit-форм AI

Дата: 2026-08-12
Тестировщик: Hermes (deleg QA round 4), UI/UX QA через CDP (Chrome 151, viewport 1600×950, тема halo,
свежая вкладка после рестарта Tomcat + Network.setCacheDisabled (свежий styles.css), сессия alan жива,
открытие edit кнопкой «Создать»)
Формы: `AiFunctionConfigurationEdit`, `AdminAiConfigurationEdit`, `UserAiConfigurationEdit`, `UserAiFunctionOverrideEdit`
Эталон: `IteractionListEdit` (замерен в этом же прогоне, та же процедура)
Метод: getComputedStyle + getBoundingClientRect (CSS-координаты) + попиксельный анализ скриншотов
(Page.captureScreenshot 2×, modal-curtain удалён перед съёмкой; скриншоты — `.team/ai-forms-diff/screenshots/r4_*.png`)
Проверка compiled CSS (halo/styles.css): у всех 4 форм `.v-slot-edit-sidebar{box-sizing:border-box;width:312px!important;
min-width:312px!important;max-width:312px!important}` (плюс отдельный блок «только фон»), у эталона
`.v-slot-iteraction-list-sidebar{width:312px!important}` — 1:1; media-тиры 296px (≤1366) / 284px (≤1100) присутствуют.

## ВЕРДИКТ: ACCEPTED

**Корневой FAIL раунда 3 (ширина слота 270px → workspace перекрывал border/shadow) — УСТРАНЁН у всех 4 форм.**

---

## 1. Ключевые проверки раунда 4 — PASS (все 4 формы 1:1 с эталоном)

| # | Метрика | Ожидание (эталон, замерен в прогоне) | Факт (все 4 формы) | Статус |
|---|---|---|---|---|
| 1 | `.v-slot-edit-sidebar` computed width | 312px | **312px** (было 270px) | ✅ PASS |
| 2 | `.v-slot-edit-sidebar` padding / border-right / shadow | 0px / 0px none / none (только фон) | 0px / 0px none / none | ✅ PASS |
| 3 | `.v-slot-edit-sidebar` фон | rgb(23,38,56) | rgb(23, 38, 56) | ✅ PASS |
| 4 | `.edit-sidebar` width | 312px | 312px | ✅ PASS |
| 5 | `.edit-sidebar` padding | 14px 16px 12px | 14px 16px 12px | ✅ PASS |
| 6 | `.edit-sidebar` border-right | 1px solid rgba(15,23,42,0.78) | 1px solid rgba(15,23,42,0.78) | ✅ PASS |
| 7 | `.edit-sidebar` box-shadow | 5px 0 20px rgba(15,23,42,0.18) | rgba(15,23,42,0.18) 5px 0px 20px 0px | ✅ PASS |
| 8 | Геометрия: sb vs slot (l/t/b/r) | дельты = 0 | **0 / 0 / 0 / 0** у всех 4 форм (sb=slot=260..572×56..941) | ✅ PASS |
| 9 | Sidebar на всю высоту | sb.bottom == root.bottom | 941 == 941; тёмный фон до y≈930 на x=400 | ✅ PASS |
| 10 | Правый край sidebar (визуально) | тёмный фон до x=571, линия на 571 | **dark_end=572 у всех 4 форм** (было 529) | ✅ PASS |
| 11 | **border-right ВИДНА** | пиксель x=571 тёмная линия (эталон: (19,26,44)) | x=571 = (19,26,44) у ВСЕХ 4 форм — идентично эталону | ✅ PASS |
| 12 | workspace не перекрывает | ws.l == sb.r (572) | ws.l=572, wsL_sbR=0 (AiFunction, Admin); у UserAi/Override подтверждено slot.r=572 + dark_end=572 (ws.l=260 — артефакт подъёма до root в скрипте, см. наблюдения) | ✅ PASS |
| 13 | Тёмная sidebar визуально 312px | фон до 571 (260+312) | у всех 4 форм фон до 571-572 на y=400/600/820 (было 529 = 270px) | ✅ PASS |

## 2. Сверка остальных метрик с эталоном — PASS (без регрессий)

| Метрика | Ожидание (эталон) | Факт (все 4 формы) | Статус |
|---|---|---|---|
| полоса «Разделы» (`.label-navigation .v-label[class*=navigation-title]`) | min-height 36px, #ffb11b, 15px/700, inset-линии, bg rgba(255,255,255,0.043), border-bottom rgba(255,255,255,0.14), padding 7px 11px | идентично | ✅ PASS |
| label-nav-item | 27px/27px/13px/600 rgba(248,250,252,0.82), padding 3px 10px, radius 0 5px 5px 0 | идентично | ✅ PASS |
| label-nav-item-active | #ffb11b, rgba(255,177,27,0.12), border-left 3px #ffb11b | идентично | ✅ PASS |
| nav-item `::before` | none / none (контракт; эталон legacy block) | none / none | ✅ PASS |
| caption карточек (.edit-card) | 17px/700/min-height 50px, padding 12px 16px | идентично (у эталона нет .edit-card — flat-секции; сверено с контрактом) | ✅ PASS |
| поля (.edit-card) | min-height 38px, 15px, radius 5px, border rgba(52,66,79,0.2), shadow none | идентично | ✅ PASS |
| подписи полей | 13px/600 rgb(108,118,128) | идентично | ✅ PASS |
| чекбоксы | 14px rgb(96,107,117) | идентично | ✅ PASS |
| footer-кнопки | контракт форм 38px/14px/0 16px (эталон 40px/18px — др. include, обе легитимны) | 38px/14px/0 16px/radius 5px | ✅ PASS |
| counts | AiFunction: 13 полей / 4 карточки; Admin: 8 / 3; UserAi: 3+1 чекбокс / 2; Override: 3+1 чекбокс / 2 (по XML) | 13/4; 8/3; 3/2; 3/2 (+1 чекбокс) | ✅ PASS |

## 3. UserAiConfigurationEdit (полная перестройка из legacy) — ОТКРЫВАЕТСЯ

| Проверка | Результат | Статус |
|---|---|---|
| форма открывается (Создать, меню «Мониторинг ключей пользователей») | root `.user-ai-configuration-editor` present | ✅ PASS |
| sidebar computed | 312px / padding 14px 16px 12px / border / shadow — как эталон | ✅ PASS |
| слот | 312px, padding 0px, sb=slot (дельты 0) | ✅ PASS |
| поля на месте | 3 поля ввода (.edit-card) + 1 чекбокс = 4 поля | ✅ PASS |
| footer | `.edit-footer-actions` кнопки 38px/14px | ✅ PASS |
| sidebar на всю высоту | 941 == 941 | ✅ PASS |

## 4. Наблюдения (НЕ FAIL)

- **box-shadow: свойство 1:1 с эталоном у всех форм; видимость ограничена контентом workspace.** У эталона
  слабый градиент тени виден на ПУСТОЙ зоне контента (x=585..592: 232→225 на фоне #e8edf3; контент-панель
  эталона начинается с x=593, отступ 21px от границы). У форм контент workspace (панель #e8edf3, белые
  карточки) прижат вплотную к границе x=572 — тень просвечивает только на участках, где контент допускает
  (напр. AiFunction на белом: x=583..584=(215,217,220), x=596=(212,215,218) — затемнение в зоне тени 577..597),
  на панельных участках скрыта непрозрачным контентом (UserAi/Override/Admin@820: ровный фон). Позиция
  workspace-СЛОТА (прозрачен, начинается на 572) НЕ перекрывает тень — это отличие КОНТЕНТА форм от эталона
  (прижатые карточки), не дефект sidebar/слота. Требование раунда 3 «workspace не перекрывает sidebar»
  выполнено: ws.l == sb.r == 572, dark_end=572, линия x=571 видна.
- `bgImage: none` у всех форм И у эталона — известный баг сборки Sass с `linear-gradient` (не расхождение;
  в compiled CSS градиент у всех: `background-image:linear,180deg,...`).
- AiFunctionConfigurationEdit: первое поле при открытии имеет border rgb(77,122,178) + focus-ring —
  required-поле в состоянии валидации (у остальных форм border rgba(52,66,79,0.2) = эталон; было и в раунде 3).
- footer эталона 40px/0 18px (iteraction-list-accordion-navigation) vs форм 38px/0 16px — обе легитимны (разные include).
- У эталона `navItemBefore` = block (legacy), у форм none/none — формы соответствуют контракту (не FAIL).
- Сырые menu-ключи пунктов sidemenu AI-форм — косметика меню, вне объёма.
- `ws.l` у UserAi/UserAiFunctionOverride в скрипте вернул 260 (подъём elementFromPoint дошёл до root, а не до
  workspace-контейнера — артефакт измерения, не рендера); перекрытие исключено: sb.r == slot.r == 572 (дельты 0),
  dark_end=572, элемент под точкой (572,600) — прозрачный c-scrollbox workspace (workspace начинается с 572).
- Media-тиры 296/284px при viewport 1600×950 не активируются — не проверялись (правила в compiled CSS присутствуют).

## 5. Итог

- **Раунд 3 закрыт полностью**: слот `.v-slot-edit-sidebar` = 312px (computed + rect 260..572) у всех 4 форм —
  1:1 с `.v-slot-iteraction-list-sidebar` эталона; sidebar == слот (дельты геометрии 0), на всю высоту (941==941);
  border-right ВИДНА (тёмная линия x=571=(19,26,44), идентична эталону); workspace начинается на x=572 и не
  перекрывает sidebar; тёмный фон визуально 312px (dark_end=572 на y=400/600/820, было 529).
- Все остальные метрики (полоса, nav, карточки, поля, подписи, footer, чекбоксы, counts) — PASS без регрессий.
- UserAiConfigurationEdit открывается и соответствует эталону (312px sidebar, 4 поля, footer).
- **Вердикт: ACCEPTED.**
