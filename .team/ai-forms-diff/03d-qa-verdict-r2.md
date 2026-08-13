# QA-вердикт R2: AiFunctionConfigurationEdit + AdminAiConfigurationEdit vs эталон IteractionListEdit (CDP-сверка)

Дата: 2026-08-13 (итерация 2 — финальная приёмочная сверка)
Метод: Chrome CDP (Chrome 151, http://localhost:9222), свежая вкладка + свежая сессия
(Network.setCacheDisabled + clearBrowserCache/Cookies ДО входа), окно fullscreen,
viewport 1600×950 (Emulation.setDeviceMetricsOverride), вход alan/Dodo-2012, всё в одном
прогоне: ЭТАЛОН (меню «Взаимодействия с кандидатом» → «Создать», root `.iteraction-list-editor`)
→ ФОРМА A (меню «Функции AI», группа «Управление AI» → «Создать», root `.ai-function-configuration-editor`)
→ ФОРМА B (меню «Корпоративные AI-подключения», группа «Управление AI» → «Создать», root `.admin-ai-configuration-editor`).
Измерение: getComputedStyle + getBoundingClientRect; hover/focus — live-замер
(Input.dispatchMouseEvent mouseMoved / el.focus()) + CSSOM загруженных стилей.
Загруженные стили (stylesheets): `inline / halo/styles.css?v=8.14.3-2-cuba / brand-login-screen/login.css / export/export.css / inline / inline` | `inline / halo/styles.css?v=8.14.3-2-cuba / brand-login-screen/login.css / export/export.css / inline / inline` | `inline / halo/styles.css?v=8.14.3-2-cuba / brand-login-screen/login.css / export/export.css / inline / inline`
Скриншоты: `screenshots/qa_r2_etalon.png`, `qa_r2_ai_function.png`, `qa_r2_admin_ai.png`.

## ФОРМА A — «Конфигурация AI-функции» (.ai-function-configuration-editor)

**ВЕРДИКТ: ACCEPTED** — PASS 54 | FAIL 0 | исключения 1

| Метрика | Эталон | Форма | PASS/FAIL |
|---|---|---|---|
| sidebar title font-size | 18px | 18px | ✅ PASS |
| sidebar title font-weight | 700 | 700 | ✅ PASS |
| sidebar title color | rgb(255, 177, 27) | rgb(255, 177, 27) | ✅ PASS |
| sidebar title line-height | 24px | 24px | ✅ PASS |
| sidebar subtitle font-size | 12px | 12px | ✅ PASS |
| sidebar subtitle font-weight | 400 | 400 | ✅ PASS |
| sidebar subtitle color | rgba(248, 250, 252, 0.72) | rgba(248, 250, 252, 0.72) | ✅ PASS |
| sidebar subtitle line-height | 17px | 17px | ✅ PASS |
| sidebar subtitle letter-spacing | normal | normal | ✅ PASS |
| sidebar subtitle text-transform | none | none | ✅ PASS |
| toolbar min-height | 58px | 58px | ✅ PASS |
| toolbar padding | 10px 20px | 10px 20px | ✅ PASS |
| toolbar border-bottom color | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS |
| toolbar title font-size | 20px | 20px | ✅ PASS |
| toolbar title font-weight | 700 | 700 | ✅ PASS |
| toolbar title line-height | 27px | 27px | ✅ PASS |
| toolbar desc font-size | 12px | 12px | ✅ PASS |
| toolbar desc color | rgb(133, 141, 149) | rgb(133, 141, 149) | ✅ PASS |
| toolbar desc line-height | 18px | 18px | ✅ PASS |
| card border-radius | 8px | 8px | ✅ PASS |
| card border-top-color | rgba(52, 66, 79, 0.15) | rgba(52, 66, 79, 0.15) | ✅ PASS |
| card box-shadow | rgba(15, 23, 42, 0.05) 0px 2px 8px 0px | rgba(15, 23, 42, 0.05) 0px 2px 8px 0px | ✅ PASS |
| card margin-bottom | 12px | 12px | ✅ PASS |
| card присутствует (.edit-card) | 5 шт | 4 шт | ✅ PASS <sub>у эталона flat-секции аккордеонов</sub> |
| checkbox padding | 3px 8px | 3px 8px | ✅ PASS |
| textarea font-size | 15px | 15px | ✅ PASS |
| textarea line-height | 21.75px | 21.75px | ✅ PASS |
| textarea padding | 4px | 4px | ✅ PASS |
| textarea min-height (исключение: rows разные) | 150px | 38px | ➖ исключение (не сравнивается) <sub>min-height не сравнивается (rows разные)</sub> |
| footer min-height | 62px | 62px | ✅ PASS |
| footer padding | 11px 20px | 11px 20px | ✅ PASS |
| footer border-top color | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS |
| footer box-shadow | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | ✅ PASS |
| footer-кнопка OK (primary) min-height | 40px | 40px | ✅ PASS |
| footer-кнопка OK (primary) padding | 0px 18px | 0px 18px | ✅ PASS |
| footer-кнопка OK (primary) font-size | 14px | 14px | ✅ PASS |
| footer-кнопка OK (primary) font-weight | 600 | 600 | ✅ PASS |
| footer-кнопка OK (primary) border-radius | 4px | 4px | ✅ PASS |
| footer-кнопка OK (primary) background | rgb(77, 122, 178) | rgb(77, 122, 178) | ✅ PASS |
| footer-кнопка OK (primary) color | rgb(255, 255, 255) | rgb(255, 255, 255) | ✅ PASS |
| footer-кнопка OK x (left) [КРИТИЧНО] | 1373 | 1373 | ✅ PASS |
| footer-кнопка OK right [КРИТИЧНО] | 1451 | 1451 | ✅ PASS |
| footer-кнопка Отмена (secondary) min-height | 40px | 40px | ✅ PASS |
| footer-кнопка Отмена (secondary) padding | 0px 18px | 0px 18px | ✅ PASS |
| footer-кнопка Отмена (secondary) font-size | 14px | 14px | ✅ PASS |
| footer-кнопка Отмена (secondary) font-weight | 600 | 600 | ✅ PASS |
| footer-кнопка Отмена (secondary) border-radius | 4px | 4px | ✅ PASS |
| footer-кнопка Отмена (secondary) background | rgba(0, 0, 0, 0) | rgba(0, 0, 0, 0) | ✅ PASS |
| footer-кнопка Отмена (secondary) color | rgb(26, 26, 26) | rgb(26, 26, 26) | ✅ PASS |
| footer-кнопка Отмена x (left) [КРИТИЧНО] | 1461 | 1461 | ✅ PASS |
| footer-кнопка Отмена right | 1569 | 1569 | ✅ PASS |
| footer межкнопочный зазор OK->Отмена [КРИТИЧНО] | 10px | 10px | ✅ PASS <sub>эталон: v-spacing 10px; фикс итерации 2: spacing=true на hbox editActionsGroup</sub> |
| footer прижатость вправо (footer.right - Отмена.right) | 21px | 21px | ✅ PASS <sub>критерий: gap формы == gap эталона (padding 20px+1px), не gap≈0</sub> |
| footer-кнопка hover filter | brightness(0.98) | brightness(0.98) | ✅ PASS <sub>live mouseMoved + CSSOM brightness(0.98)</sub> |
| footer-кнопка focus box-shadow | rgba(77, 122, 178, 0.2) 0px 0px 0px 2px | rgba(77, 122, 178, 0.2) 0px 0px 0px 2px | ✅ PASS <sub>live el.focus() + CSSOM ring 2px</sub> |

### Несовпадения (Форма A)

Несовпадений нет.

## ФОРМА B — «Корпоративное AI-подключение» (.admin-ai-configuration-editor)

**ВЕРДИКТ: ACCEPTED** — PASS 58 | FAIL 0 | исключения 1

| Метрика | Эталон | Форма | PASS/FAIL |
|---|---|---|---|
| sidebar title font-size | 18px | 18px | ✅ PASS |
| sidebar title font-weight | 700 | 700 | ✅ PASS |
| sidebar title color | rgb(255, 177, 27) | rgb(255, 177, 27) | ✅ PASS |
| sidebar title line-height | 24px | 24px | ✅ PASS |
| sidebar subtitle font-size | 12px | 12px | ✅ PASS |
| sidebar subtitle font-weight | 400 | 400 | ✅ PASS |
| sidebar subtitle color | rgba(248, 250, 252, 0.72) | rgba(248, 250, 252, 0.72) | ✅ PASS |
| sidebar subtitle line-height | 17px | 17px | ✅ PASS |
| sidebar subtitle letter-spacing | normal | normal | ✅ PASS |
| sidebar subtitle text-transform | none | none | ✅ PASS |
| toolbar min-height | 58px | 58px | ✅ PASS |
| toolbar padding | 10px 20px | 10px 20px | ✅ PASS |
| toolbar border-bottom color | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS |
| toolbar title font-size | 20px | 20px | ✅ PASS |
| toolbar title font-weight | 700 | 700 | ✅ PASS |
| toolbar title line-height | 27px | 27px | ✅ PASS |
| toolbar desc font-size | 12px | 12px | ✅ PASS |
| toolbar desc color | rgb(133, 141, 149) | rgb(133, 141, 149) | ✅ PASS |
| toolbar desc line-height | 18px | 18px | ✅ PASS |
| card border-radius | 8px | 8px | ✅ PASS |
| card border-top-color | rgba(52, 66, 79, 0.15) | rgba(52, 66, 79, 0.15) | ✅ PASS |
| card box-shadow | rgba(15, 23, 42, 0.05) 0px 2px 8px 0px | rgba(15, 23, 42, 0.05) 0px 2px 8px 0px | ✅ PASS |
| card margin-bottom | 12px | 12px | ✅ PASS |
| card присутствует (.edit-card) | 5 шт | 3 шт | ✅ PASS <sub>у эталона flat-секции аккордеонов</sub> |
| checkbox padding | 3px 8px | 3px 8px | ✅ PASS |
| textarea font-size | 15px | 15px | ✅ PASS |
| textarea line-height | 21.75px | 21.75px | ✅ PASS |
| textarea padding | 4px | 4px | ✅ PASS |
| textarea min-height (исключение: rows разные) | 150px | 38px | ➖ исключение (не сравнивается) <sub>min-height не сравнивается (rows разные)</sub> |
| password min-height (искл.: только у формы) | контракт 38px | 38px | ✅ PASS |
| password font-size (искл.: только у формы) | контракт 15px | 15px | ✅ PASS |
| password border-color (искл.: только у формы) | контракт rgba(52,66,79,0.2) | rgba(52, 66, 79, 0.2) | ✅ PASS |
| password border-radius (искл.: только у формы) | контракт 5px | 5px | ✅ PASS |
| footer min-height | 62px | 62px | ✅ PASS |
| footer padding | 11px 20px | 11px 20px | ✅ PASS |
| footer border-top color | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS |
| footer box-shadow | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | ✅ PASS |
| footer-кнопка OK (primary) min-height | 40px | 40px | ✅ PASS |
| footer-кнопка OK (primary) padding | 0px 18px | 0px 18px | ✅ PASS |
| footer-кнопка OK (primary) font-size | 14px | 14px | ✅ PASS |
| footer-кнопка OK (primary) font-weight | 600 | 600 | ✅ PASS |
| footer-кнопка OK (primary) border-radius | 4px | 4px | ✅ PASS |
| footer-кнопка OK (primary) background | rgb(77, 122, 178) | rgb(77, 122, 178) | ✅ PASS |
| footer-кнопка OK (primary) color | rgb(255, 255, 255) | rgb(255, 255, 255) | ✅ PASS |
| footer-кнопка OK x (left) [КРИТИЧНО] | 1373 | 1373 | ✅ PASS |
| footer-кнопка OK right [КРИТИЧНО] | 1451 | 1451 | ✅ PASS |
| footer-кнопка Отмена (secondary) min-height | 40px | 40px | ✅ PASS |
| footer-кнопка Отмена (secondary) padding | 0px 18px | 0px 18px | ✅ PASS |
| footer-кнопка Отмена (secondary) font-size | 14px | 14px | ✅ PASS |
| footer-кнопка Отмена (secondary) font-weight | 600 | 600 | ✅ PASS |
| footer-кнопка Отмена (secondary) border-radius | 4px | 4px | ✅ PASS |
| footer-кнопка Отмена (secondary) background | rgba(0, 0, 0, 0) | rgba(0, 0, 0, 0) | ✅ PASS |
| footer-кнопка Отмена (secondary) color | rgb(26, 26, 26) | rgb(26, 26, 26) | ✅ PASS |
| footer-кнопка Отмена x (left) [КРИТИЧНО] | 1461 | 1461 | ✅ PASS |
| footer-кнопка Отмена right | 1569 | 1569 | ✅ PASS |
| footer межкнопочный зазор OK->Отмена [КРИТИЧНО] | 10px | 10px | ✅ PASS <sub>эталон: v-spacing 10px; фикс итерации 2: spacing=true на hbox editActionsGroup</sub> |
| footer прижатость вправо (footer.right - Отмена.right) | 21px | 21px | ✅ PASS <sub>критерий: gap формы == gap эталона (padding 20px+1px), не gap≈0</sub> |
| footer-кнопка hover filter | brightness(0.98) | brightness(0.98) | ✅ PASS <sub>live mouseMoved + CSSOM brightness(0.98)</sub> |
| footer-кнопка focus box-shadow | rgba(77, 122, 178, 0.2) 0px 0px 0px 2px | rgba(77, 122, 178, 0.2) 0px 0px 0px 2px | ✅ PASS <sub>live el.focus() + CSSOM ring 2px</sub> |

### Несовпадения (Форма B)

Несовпадений нет.

## Структурно-контентные исключения (разрешены, НЕ FAIL)

- **textarea min-height**: у эталона `150px`, у форм `38px`/`38px` — не сравнивается (rows разные в XML).
- **`.edit-card`**: формы — карточки-панели; эталон — flat-секции аккордеонов с тем же классом
  (`iteraction-list-flat-section edit-card`). Стили сверены 1:1 (radius 8px, border
  rgba(52,66,79,0.15), shadow 0 2px 8px rgba(15,23,42,0.05), margin-bottom 12px).
  Кол-во: эталон 5 шт, форма A 4 шт, форма B 3 шт.
- **password-поле** (только у формы B): min-height 38px, font-size 15px, border rgba(52, 66, 79, 0.2), radius 5px — сверено с контрактом (38px/15px/rgba(52,66,79,0.2)/5px).
- **Состав footer-кнопок**: эталон 3 шт (Подписаться + OK + Отмена), формы по 2 (OK + Отмена) —
  контентное отличие, не стилевое; правая пара OK→Отмена сравнивалась позиционно.
- **Активная flat-секция эталона** (участники) имеет primary-ring
  (`rgba(77,122,178,0.18) 0 0 0 2px` + border rgb(77,122,178)) — состояние «активен» аккордеона;
  неактивные flat-секции эталона совпадают с карточками форм 1:1 (см. 03b).

## Критичный фикс итерации 2 (межкнопочный зазор OK→Отмена = 10px)

Проверено в этом прогоне: зазор OK→Отмена — эталон 10px, форма A 10px, форма B 10px;
OK.left/right: эталон 1373/1451, форма A 1373/1451, форма B 1373/1451; Отмена.left: эталон 1461, форма A 1461, форма B 1461.
Прижатость вправо (footer.right − Отмена.right): эталон 21px, форма A 21px, форма B 21px.

## Итог

- Форма A: **ACCEPTED** (54 PASS, 0 FAIL).
- Форма B: **ACCEPTED** (58 PASS, 0 FAIL).
- Код/XML/SCSS/Java/entity/БД/эталон не изменялись — только измерение и вердикт.
