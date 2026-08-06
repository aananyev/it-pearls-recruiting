# OpenPositionEdit — 06. Арбитраж UI-контракта

> Роль: Арбитр UI-контракта. Разрешены только формально зарегистрированные конфликты
> между референсом, CUBA-контрактом, запретом на изменение бизнес-логики и
> ограничениями XML/SCSS. Код арбитр не пишет. Дата: 2026-08-05.
> HEAD: 2ff1f129ec1378c043293a8d7ba30f77316e0988.

## Правила арбитража

- Изменение бизнес-логики, сущности, справочников и других форм арбитр разрешить не может.
- Вердикты: IMPLEMENT_WITH_XML_SCSS | KEEP_CURRENT_FUNCTIONAL_CONTRACT | SKIP_REFERENCE_ELEMENT | ALLOW_DECORATIVE_ONLY | RETURN_TO_UI_IMPLEMENTER | FUNCTIONAL_CHANGE_FORBIDDEN.

---

## DISPUTE-9-1: dialogMode 1100×800 vs полноэкранный рендер 1920×1080

- **Элемент референса:** форма занимает весь экран 1920×1080 (двухпанельная компоновка sidebar + workspace).
- **Текущий контракт:** `dialogMode height="800px" width="1100px"`; открытие из `OpenPositionBrowse` через `screenBuilders.editor(...).editEntity(entity).build().show()` (OpenMode.DIALOG, модальное окно).
- **Техническое ограничение:** размер окна диалога задаётся в XML `dialogMode`; способ открытия (DIALOG) и browse-вызовы менять запрещено (Other Screens Freeze).
- **Вариант A:** увеличить `dialogMode` до `1400×900` — двухпанельная компоновка с sidebar 270px даёт workspace ~1130px, вкладки читаемы; открытие остаётся модальным диалогом, browse не меняется.
- **Вариант B:** оставить 1100×800 — workspace ~830px, 12 вкладок требуют horizontal overflow tabcontainer (эталон preview), поля сжимаются.
- **Рекомендация:** Вариант A — `dialogMode height="900px" width="1400px"`. `width`/`height` формы входят в разрешённый перечень визуальных правок («ширину и высоту»); способ открытия (DIALOG) и browse не изменяются.
- **Последствия:** модальный диалог крупнее, но двухпанельная компоновка соответствует утверждённому визуальному языку Edit-форм (JobCandidateEdit — тоже диалог 1200×750 с sidebar).
- **VERDICT: IMPLEMENT_WITH_XML_SCSS** (только ширина/высота `dialogMode`; openMode DIALOG и browse-код не трогать).

## DISPUTE-9-2: видимая вкладка «Оплата и контакты» на рендере vs legacy `tabPayments visible="false"`

- **Элемент референса:** в списке вкладок присутствует «Оплата и контакты» (видимая).
- **Текущий контракт:** `tabPayments visible="false"`; видимостью управляет Java (`@Named("tabSheetOpenPosition.tabPayments") VBoxLayout tabPayments`, `disableEnableFields` по `commandCandidate`). Изменение `visible` запрещено.
- **Техническое ограничение:** смена `visible` = изменение условий видимости (запрещено); Java READ_ONLY.
- **Вариант A:** оставить `tabPayments` скрытой технической вкладкой (инвариант `@Named`), платёжные секции (`groupBoxPaymentsResearcher`, `groupBoxPaymentsRecrutier`, `groupBoxPaymentsDetail` + 3 колонки) визуально перенести внутрь `laborAgreementTab` (паттерн утверждённого preview: «Оплата компании → Оплата ресерчерам → Оплата рекрутерам»). Это перестановка существующих компонентов — разрешена.
- **Вариант B:** показывать `tabPayments` — запрещено (видимость управляется Java).
- **Рекомендация:** Вариант A.
- **VERDICT: KEEP_CURRENT_FUNCTIONAL_CONTRACT + перенос платёжных секций в laborAgreementTab** (перестановка существующих компонентов; tabPayments остаётся скрытой; Java, visible, actions не меняются). Точное воспроизведение рендера (видимая вкладка) требует функционального изменения — фиксируется как DESIGN_REQUIRES_FORBIDDEN_FUNCTIONAL_CHANGE.

## DISPUTE-9-3: ширина sidebar — рендер 312px vs Контракт 270/250px

- **Элемент референса:** sidebar ≈312px (как JobCandidateEdit).
- **Текущий контракт:** §4.2 — базовая 270px, при viewport ≤1366px — 250px; shared CSS фиксирует `270px !important`. JobCandidateEdit 312px — задокументированное исключение, не норматив.
- **Техническое ограничение:** Контракт §4.2 запрещает локальное переопределение ширины без зафиксированной причины; в диалоге 1100–1400px sidebar 312px оставит workspace слишком узким для 12 вкладок.
- **Вариант A:** 270px / 250px (≤1366px) по Контракту.
- **Вариант B:** 312px локально (sidebar + slot) — повторение исключения JobCandidateEdit.
- **Рекомендация:** Вариант A — следовать нормативному Контракту; отклонение рендера фиксируется в UI-спецификации.
- **VERDICT: IMPLEMENT_WITH_XML_SCSS** — ширина 270px (250px ≤1366px) из shared; локальных переопределений ширины не создавать.

## DISPUTE-9-4: статус «• позиция открыта» в toolbar

- **Элемент референса:** в toolbar-области справа индикатор «• позиция открыта».
- **Текущий контракт:** в legacy нет отдельного toolbar-компонента статуса; статусная информация есть в `signDraftLabel` («Черновик»), `closedVacancyInfoLabel` (обратный отсчёт) и в browse. Значение `openClose` существует в entity.
- **Техническое ограничение:** создание нового label с `property="openClose"` + formatter — новое binding/formatting (пограничный случай); Java не менять; новые бизнес-значения запрещены.
- **Вариант A:** реализовать только визуальный заголовок toolbar `edit-toolbar-title` («Редактирование открытой позиции» — статический label, не бизнес-значение); точный статус открытости не дублировать отдельным компонентом (он виден в browse и через существующие label).
- **Вариант B:** создать новый label статуса с биндингом на `openClose` — риск нового binding.
- **Рекомендация:** Вариант A.
- **VERDICT: SKIP_REFERENCE_ELEMENT** для «• позиция открыта» (декоративный элемент референса; существующие статусные компоненты сохраняются); заголовок toolbar — ALLOW_DECORATIVE_ONLY (статический label, не бизнес-значение).

## DISPUTE-9-5: двухколоночная группировка «Идентификаторы и статус» + «Команда / Вакансия»

- **Элемент референса:** две карточки в ряд.
- **Текущий контракт:** groupBox-секции вкладки — вертикальный поток в `mainTabScrollBox`.
- **Техническое ограничение:** нет; перестановка контейнеров разрешена; `collapsable/collapsed` сохраняются.
- **VERDICT: IMPLEMENT_WITH_XML_SCSS** — допустимая визуальная перегруппировка секций в ряды (flex-wrap), bindings не меняются.

## DISPUTE-9-6: дублирует 9-2 (вкладка «Оплата» в списке рендера)

- **VERDICT:** см. 9-2.

---

## Итог

- IMPLEMENT_WITH_XML_SCSS: 9-1 (dialogMode 1400×900), 9-3 (270/250px), 9-5.
- KEEP_CURRENT_FUNCTIONAL_CONTRACT: 9-2 (tabPayments скрыта; платёжные секции → laborAgreementTab).
- SKIP_REFERENCE_ELEMENT: 9-4 (статус в toolbar не дублируется).
- DESIGN_REQUIRES_FORBIDDEN_FUNCTIONAL_CHANGE: 9-2/9-6 (видимая вкладка «Оплата и контакты»).
- Изменение бизнес-логики, сущностей, справочников, других форм, Java, loaders, JPQL, views, actions, invoke, validators, required/visible/enabled — НЕ разрешено ни по одному пункту.
