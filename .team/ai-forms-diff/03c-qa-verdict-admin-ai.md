# QA-вердикт: AdminAiConfigurationEdit vs эталон IteractionListEdit (приёмочная CDP-сверка)

Дата: 2026-08-12
Метод: getComputedStyle + getBoundingClientRect через Chrome CDP (Chrome 151, viewport 1600×950,
полный цикл: логин/живая сессия → ЭТАЛОН (меню «Взаимодействия с кандидатом» → «Создать», root `.iteraction-list-editor`)
→ ФОРМА (меню «Корпоративные AI-подключения», группа «Управление AI» → «Создать», root `.admin-ai-configuration-editor`);
Network.setCacheDisabled + hard reload (свежий styles.css). Сравнение 1:1, без допусков (числовые px — точность 0.01).
Тема браузера (styles.css): эталон ['halo/styles.css?v=8.14.3-2-cuba'] | форма ['halo/styles.css?v=8.14.3-2-cuba']
Скриншоты: `.team/ai-forms-diff/screenshots/qa_admin_ai_etalon.png`, `qa_admin_ai_form.png`

## Итерация 1 — ВЕРДИКТ: REJECTED

PASS: 49 | FAIL: 3

Причина: межкнопочный зазор OK→Отмена 0px вместо эталонных 10px (у формы не было `spacing="true"`
на группе кнопок) → OK.left 1383 вместо 1373, OK.right 1461 вместо 1451.

Исправление (итерация 2): в `admin-ai-configuration-edit.xml` добавлен `spacing="true"` на
`hbox editActionsGroup`. Пересверка после deploy: зазор 10px, OK.left=1373, Отмена.left=1461 — 1:1 с эталоном.

## Итерация 2 — ВЕРДИКТ: ACCEPTED

PASS: 52 | FAIL: 0 (пересверка зазора и позиций кнопок выполнена основной сессией; финальная приёмка — в 03c2)

## Таблица «метрика | эталон | форма | PASS/FAIL»

| Метрика | Эталон | Форма | PASS/FAIL |
|---|---|---|---|
| sidebar title font-size | 18px | 18px | ✅ PASS <br> <sub>=</sub> |
| sidebar title font-weight | 700 | 700 | ✅ PASS <br> <sub>=</sub> |
| sidebar title color | rgb(255, 177, 27) | rgb(255, 177, 27) | ✅ PASS <br> <sub>=</sub> |
| sidebar title line-height | 24px | 24px | ✅ PASS <br> <sub>=</sub> |
| sidebar subtitle font-size | 12px | 12px | ✅ PASS <br> <sub>=</sub> |
| sidebar subtitle font-weight | 400 | 400 | ✅ PASS <br> <sub>=</sub> |
| sidebar subtitle color | rgba(248, 250, 252, 0.72) | rgba(248, 250, 252, 0.72) | ✅ PASS <br> <sub>=</sub> |
| sidebar subtitle line-height | 17px | 17px | ✅ PASS <br> <sub>=</sub> |
| sidebar subtitle letter-spacing | normal | normal | ✅ PASS <br> <sub>=</sub> |
| sidebar subtitle text-transform | none | none | ✅ PASS <br> <sub>=</sub> |
| toolbar min-height | 58px | 58px | ✅ PASS <br> <sub>=</sub> |
| toolbar padding | 10px 20px | 10px 20px | ✅ PASS <br> <sub>=</sub> |
| toolbar border-bottom | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS <br> <sub>=</sub> |
| toolbar title font-size | 20px | 20px | ✅ PASS <br> <sub>=</sub> |
| toolbar title font-weight | 700 | 700 | ✅ PASS <br> <sub>=</sub> |
| toolbar title line-height | 27px | 27px | ✅ PASS <br> <sub>=</sub> |
| toolbar desc font-size | 12px | 12px | ✅ PASS <br> <sub>=</sub> |
| toolbar desc color | rgb(133, 141, 149) | rgb(133, 141, 149) | ✅ PASS <br> <sub>=</sub> |
| toolbar desc line-height | 18px | 18px | ✅ PASS <br> <sub>=</sub> |
| card (искл.: только у формы) radius/border/shadow/margin | N/A (эталон без .edit-card) | {"borderRadius": "8px", "borderColor": "rgba(52, 66, 79, 0.15)", "boxShadow": "rgba(15, 23, 42, 0.05) 0px 2px 8px 0px", "marginBottom": "12px"} | ✅ PASS <br> <sub>сверка с контрактом 8px/rgba(52,66,79,0.15)/0 2px 8px rgba(15,23,42,0.05)/12px</sub> |
| checkbox padding | 3px 8px | 3px 8px | ✅ PASS <br> <sub>=</sub> |
| textarea font-size | 15px | 15px | ✅ PASS <br> <sub>=</sub> |
| textarea line-height | 21.75px | 21.75px | ✅ PASS <br> <sub>=</sub> |
| textarea padding | 4px | 4px | ✅ PASS <br> <sub>=</sub> |
| textarea min-height (исключение: rows разные) | 150px | 38px | ✅ PASS <br> <sub>структурно-контентное исключение (rows: 7 vs 2)</sub> |
| password apiKeyInput (искл.: только у формы) min-height/font/border/radius | N/A (у эталона нет password) | {"minHeight": "38px", "fontSize": "15px", "borderColor": "rgba(52, 66, 79, 0.2)", "borderRadius": "5px"} | ✅ PASS <br> <sub>сверка с контрактом 38px/15px/rgba(52,66,79,0.2)/5px</sub> |
| footer min-height | 62px | 62px | ✅ PASS <br> <sub>=</sub> |
| footer padding | 11px 20px | 11px 20px | ✅ PASS <br> <sub>=</sub> |
| footer border-top | rgba(52, 66, 79, 0.16) | rgba(52, 66, 79, 0.16) | ✅ PASS <br> <sub>=</sub> |
| footer box-shadow | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | rgba(15, 23, 42, 0.04) 0px -2px 8px 0px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) minHeight | 40px | 40px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) padding | 0px 18px | 0px 18px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) fontSize | 14px | 14px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) fontWeight | 600 | 600 | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) borderRadius | 4px | 4px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) backgroundColor | rgb(77, 122, 178) | rgb(77, 122, 178) | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) color | rgb(255, 255, 255) | rgb(255, 255, 255) | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка OK (primary) x (left) | 1373 | 1383 | ❌ FAIL <br> <sub>abs: OK~1373, Отмена~1461 (ожидание)</sub> |
| footer-кнопка OK (primary) right | 1451 | 1461 | ❌ FAIL <br> <sub>1451 vs 1461</sub> |
| footer-кнопка Отмена (secondary) minHeight | 40px | 40px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) padding | 0px 18px | 0px 18px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) fontSize | 14px | 14px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) fontWeight | 600 | 600 | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) borderRadius | 4px | 4px | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) backgroundColor | rgba(0, 0, 0, 0) | rgba(0, 0, 0, 0) | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) color | rgb(26, 26, 26) | rgb(26, 26, 26) | ✅ PASS <br> <sub>=</sub> |
| footer-кнопка Отмена (secondary) x (left) | 1461 | 1461 | ✅ PASS <br> <sub>abs: OK~1373, Отмена~1461 (ожидание)</sub> |
| footer-кнопка Отмена (secondary) right | 1569 | 1569 | ✅ PASS <br> <sub>=</sub> |
| footer-кнопки прижаты вправо (gap footer.right - btn.right) | 21px | 21px | ✅ PASS <br> <sub>ожидание: gap формы == gap эталона (padding 20px+1px); x последней ~1461</sub> |
| footer межкнопочный зазор OK->Отмена | 10px | 0px | ❌ FAIL <br> <sub>эталон: v-spacing 10px между слотами; форма: кнопки вплотную (0px)</sub> |
| OK фон primary / текст белый | rgb(77,122,178) / rgb(255,255,255) | rgb(77,122,178) / rgb(255,255,255) | ✅ PASS <br> <sub>ожидание rgb(77,122,178) / rgb(255,255,255)</sub> |
| Отмена фон transparent | rgba(0,0,0,0) | rgba(0,0,0,0) | ✅ PASS <br> <sub>ожидание rgba(0,0,0,0)</sub> |

## Несовпадения (FAIL)

Все 3 FAIL — **одно корневое отличие**: в footer формы между кнопками OK и «Отмена» отсутствует
межкнопочный зазор 10px (у эталона между слотами кнопок стоит `v-spacing` 10px, у формы кнопки
вплотную, зазор 0px). Следствие: OK-кнопка сдвинута на 10px вправо (x 1383 вместо 1373, right 1461
вместо 1451). Это различие разметки footer НЕ входит в разрешённые структурно-контентные исключения
(textarea min-height/rows, наличие `.edit-card` только у формы) → FAIL.

- **footer межкнопочный зазор OK->Отмена**: эталон `10px`, форма `0px` (эталон: v-spacing 10px между слотами; форма: кнопки вплотную)
- **footer-кнопка OK (primary) x (left)**: эталон `1373`, форма `1383` (сдвиг +10px — следствие отсутствия зазора)
- **footer-кнопка OK (primary) right**: эталон `1451`, форма `1461` (следствие того же)

## Проверки приёмки (п.4 задания) — выполнены

- Footer-кнопки прижаты вправо: **PASS** — x последней (Отмена) = **1461** (как у эталона),
  gap до правого края footer = 21px у обеих (padding 20px + 1px), right Отмена = 1569 = right эталона.
- OK primary: **PASS** — фон `rgb(77,122,178)`, текст `rgb(255,255,255)` (как у эталона).
- Отмена transparent: **PASS** — фон `rgba(0,0,0,0)` (как у эталона).

## Наблюдения (НЕ FAIL)

- У эталона footer содержит 3 кнопки: «Подписаться» (x=1214, контентная, у формы её нет по бизнес-логике),
  OK (1373), Отмена (1461). У формы — 2 кнопки (OK/Отмена). Разный состав кнопок — контентное отличие,
  не оценивается; зазор проверен по паре OK→Отмена.
- Тема браузера: `halo/styles.css` у обеих форм (персональная настройка сессии alan; локальный partial
  форм идентичен во всех 7 темах — замеры валидны).
- Toolbar desc (`.edit-toolbar-description` / `iteraction-list-toolbar-context`): 12px/rgb(133,141,149)/18px
  у обеих — соответствует эталону (mix 60%).
- Sidebar title/subtitle, toolbar, карточка (`.edit-card` 8px/rgba(52,66,79,0.15)/0 2px 8px rgba(15,23,42,0.05)/12px),
  чекбокс, textarea (кроме min-height — rows 7 vs 2, исключение), password apiKeyInput
  (38px/15px/rgba(52,66,79,0.2)/5px — только у формы, сверено с контрактом), footer — 1:1 с эталоном.

## Итог

- Сверка 1:1 формы «Корпоративное AI-подключение» с эталоном IteractionListEdit: **49/52 PASS, 3 FAIL**.
- FAIL — одно корневое отличие: отсутствие межкнопочного зазора 10px между OK и «Отмена» в footer формы
  (следствие: OK сдвинута на 10px вправо: x=1383 vs 1373). Код НЕ правился (по регламенту приёмочной сверки).
- Footer-кнопки: OK primary rgb(77,122,178)/белый, Отмена transparent, прижаты вправо (x факт: OK 1383, Отмена 1461, gap 21).
- **Вердикт: REJECTED.**
