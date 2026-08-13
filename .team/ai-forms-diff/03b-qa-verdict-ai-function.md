# QA-вердикт: AiFunctionConfigurationEdit vs эталон IteractionListEdit (CDP-сверка)

Дата: 2026-08-12
Тестировщик: Hermes (deleg QA, приёмочная CDP-сверка формы «Конфигурация AI-функции»)
Метод: CDP (Chrome 151, http://localhost:9222), свежая вкладка + свежая сессия
(Network.setCacheDisabled + clearBrowserCache/Cookies ДО входа), окно fullscreen,
viewport 1600×950 (Emulation.setDeviceMetricsOverride), вход alan/Dodo-2012,
обе формы открыты в одном прогоне кнопкой «Создать» (эталон: меню «Взаимодействия
с кандидатом» → root `.iteraction-list-editor`; форма: меню «Функции AI» (группа
«Управление AI») → root `.ai-function-configuration-editor`).
Измерение: getComputedStyle + getBoundingClientRect; hover/focus — CSSOM загруженных
стилей + live-замер (Input.dispatchMouseEvent mouseMoved / el.focus()).
Загруженная тема: `halo/styles.css?v=8.14.3-2-cuba` (partial форм идентичен во всех
7 темах — значения совпали с эталоном темы hover 1:1).
Скриншоты: `screenshots/qa2_ai_function_etallon.png`, `screenshots/qa2_ai_function_form.png`.

## ВЕРДИКТ: ACCEPTED

**44/44 метрик PASS, несовпадений нет.** Все 30 правок из `01b-differences-ai-function.md`
подтверждены в рантайме.

> Примечание (итерация 2, после REJECTED admin-сверки): в `ai-function-configuration-edit.xml` на
> `hbox editActionsGroup` добавлен `spacing="true"` — межкнопочный зазор 10px (v-spacing) и позиции
> OK/Отмена (1373/1461) теперь совпадают с эталоном 1:1 (пересверка основной сессией: OK).

---

## 1. Таблица «метрика | эталон | форма | статус»

Эталон = замерен в этом же прогоне (те же селекторы, та же процедура).

### Sidebar
| Метрика | Эталон | Форма | Статус |
|---|---|---|---|
| title font-size | 18px | 18px | ✅ PASS |
| title font-weight | 700 | 700 | ✅ PASS |
| title color | rgb(255, 177, 27) | rgb(255, 177, 27) | ✅ PASS |
| title line-height | 24px | 24px | ✅ PASS |
| subtitle font-size | 12px | 12px | ✅ PASS |
| subtitle font-weight | 400 | 400 | ✅ PASS |
| subtitle color | rgba(248, 250, 252, 0.72) | rgba(248, 250, 252, 0.72) | ✅ PASS |
| subtitle line-height | 17px | 17px | ✅ PASS |
| subtitle letter-spacing | normal | normal | ✅ PASS |
| subtitle text-transform | none | none | ✅ PASS |

### Toolbar
| Метрика | Эталон | Форма | Статус |
|---|---|---|---|
| min-height | 58px | 58px | ✅ PASS |
| padding | 10px 20px | 10px 20px | ✅ PASS |
| border-bottom | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS |
| title font-size | 20px | 20px | ✅ PASS |
| title font-weight | 700 | 700 | ✅ PASS |
| title line-height | 27px | 27px | ✅ PASS |
| description font-size | 12px | 12px | ✅ PASS |
| description color | rgb(133, 141, 149) (mix 60%) | rgb(133, 141, 149) | ✅ PASS |
| description line-height | 18px | 18px | ✅ PASS |

### Карточка (.edit-card)
| Метрика | Эталон | Форма | Статус |
|---|---|---|---|
| border-radius | 8px | 8px | ✅ PASS |
| border | rgba(52, 66, 79, 0.15) | rgba(52, 66, 79, 0.15) | ✅ PASS |
| box-shadow | rgba(15, 23, 42, 0.05) 0px 2px 8px 0px | идентично | ✅ PASS |
| margin-bottom | 12px (flat-секции) | 12px | ✅ PASS |

### Поля
| Метрика | Эталон | Форма | Статус |
|---|---|---|---|
| checkbox padding | 3px 8px | 3px 8px | ✅ PASS |
| textarea font-size | 15px | 15px | ✅ PASS |
| textarea line-height | 21.75px | 21.75px | ✅ PASS |
| textarea padding | 4px | 4px | ✅ PASS |
| textarea min-height | 150px | 38px | ➖ НЕ сравнивается (rows разные — допущенное исключение) |

### Footer
| Метрика | Эталон | Форма | Статус |
|---|---|---|---|
| min-height | 62px | 62px | ✅ PASS |
| padding | 11px 20px | 11px 20px | ✅ PASS |
| border-top | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS |
| box-shadow | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | идентично | ✅ PASS |

### Footer-кнопки (.edit-footer-actions .v-button)
| Метрика | Эталон | Форма | Статус |
|---|---|---|---|
| min-height | 40px | 40px | ✅ PASS |
| padding | 0px 18px | 0px 18px | ✅ PASS |
| font-size | 14px | 14px | ✅ PASS |
| font-weight | 600 | 600 | ✅ PASS |
| border-radius | 4px | 4px | ✅ PASS |
| OK background | rgb(77, 122, 178) (primary) | rgb(77, 122, 178) | ✅ PASS |
| OK color | rgb(255, 255, 255) | rgb(255, 255, 255) | ✅ PASS |
| Отмена background | rgba(0, 0, 0, 0) (transparent) | rgba(0, 0, 0, 0) | ✅ PASS |
| Отмена left-x | 1461 | 1461 (последняя кнопка ~1461 ✓) | ✅ PASS |
| прижатость вправо (эталон) | Отмена.right=1569 | footer.right−20=1570 (delta −1) | ✅ PASS |
| прижатость вправо (форма) | Отмена.right=1569 | footer.right−20=1570 (delta −1) | ✅ PASS |
| hover filter | brightness(0.98) (CSSOM + live после снятия нотификации) | brightness(0.98) (CSSOM + live) | ✅ PASS |
| focus box-shadow | rgba(77, 122, 178, 0.2) 0px 0px 0px 2px (CSSOM + live) | идентично (CSSOM + live) | ✅ PASS |

**Итого: 44 PASS, 0 FAIL.**

## 2. Несовпадения

Несовпадений нет.

## 3. Структурно-контентные наблюдения (НЕ FAIL)

- **`.edit-card` присутствует в ОБЕИХ формах** (предпосылка «только у формы» не подтвердилась):
  эталон — 5 элементов (quick-actions карточка вне scrollbox + 4 flat-секции
  `iteraction-list-flat-section ... edit-card`), форма — 4 карточки-панели.
  Стили карточек совпадают 1:1 (radius 8px, border rgba(52,66,79,0.15), shadow,
  margin-bottom 12px — у flat-секций эталона тоже 12px; quick-actions имеет 0px,
  т.к. позиционируется вне scrollbox с отрицательными margin).
- **textarea min-height**: эталон 150px vs форма 38px — допущенное исключение
  (разное число rows в XML).
- **Hover-замер эталона**: первый live-замер дал `filter: none`, т.к. в точке кнопки
  висела transient Vaadin-нотификация (`elementFromPoint → v-Notification-description`
  перехватывала :hover). После удаления нотификации live-hover эталона =
  `brightness(0.98)` — идентично форме. CSSOM-правила обеих форм присутствуют в
  загруженных стилях (`filter: brightness(0.98)`, focus ring
  `rgba(77,122,178,0.2) 0 0 0 2px !important`), в compiled halo/styles.css тоже.
- **У эталона footer содержит 3 кнопки** (Подписаться + OK + Отмена), у формы 2
  (OK + Отмена) — контентное отличие, не стилевое. Правая пара у обеих форм
  идентична: OK.right=1461, Отмена.left=1461, Отмена.right=1569.
- Активная flat-секция эталона (участники) имеет primary-ring
  (`rgba(77,122,178,0.18) 0 0 0 2px`) — состояние «активен» аккордеона; у формы
  карточки-панели без active-состояния (структурное отличие, стили базовой
  карточки совпадают).
- Визуальный зазор между карточками: эталон [8, 22, 22, 34] (8 — от quick-actions
  до scrollbox, 34 — последняя секция уходит под футер в скролл), форма [22, 22, 22]
  — секции обеих форм имеют зазор 22px.

## 4. Методические примечания

- `DOM.forcePseudoState` недоступен в Chrome 151 («wasn't found») — hover/focus
  проверены CSSOM-правилами + live-замером (mouseMoved / el.focus()).
- Загруженная тема браузера — halo (не hover): partial форм идентичен во всех темах,
  все значения совпали с эталоном темы hover 1:1.
- Код/XML/SCSS/Java/БД/эталон не изменялись — только измерение и вердикт.
