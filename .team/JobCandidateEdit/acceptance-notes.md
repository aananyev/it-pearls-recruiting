# Приёмка редизайна компоновки JobCandidateEdit

> Дата: 2026-08-03
> Роль: UI/UX-приёмка (по фактам из браузера + файлам правок; код НЕ изменялся)
> Форма: `hunttech_JobCandidate.edit` — `job-candidate-edit.xml` + `JobCandidateEdit.java` (не менялся)
> Базис: дизайн-ревью `.team/JobCandidateEdit/design-notes.md` от 2026-08-03 (P1-1…P1-4, P2-5…P2-12, P3-13…P3-14)
> Браузер: тема **halo**, форма открыта (localhost:8080/hrm, пользователь alan), CSS развёрнут (md5 deployed `deploy/tomcat/webapps/hrm/VAADIN/themes/halo/styles.css` == `build/themes-tmp/.../halo/styles.css`)

---

## 1. Сводный вердикт

| Блок | Вердикт |
|---|---|
| P1-1 Навигация 27px (правила применены) | ✅ **ЗАКРЫТО 2026-08-03** — фикс `:before { display:none !important }` применён в 7 темах; браузер: все 7 пунктов h=27px (min-height 27px, padding 3px 10px, line-height 20px); сборка + контракт-тест зелёные. |
| P1-2 Вкладка «Основное» — вертикальный стек | ✅ ПРИНЯТО |
| P1-3 Toolbar «заголовок + описание + Еще» | ✅ ПРИНЯТО |
| P1-4 Маркер ▼ убран | ✅ ПРИНЯТО |
| P2-5 Sidebar 312/296/284 | ✅ ПРИНЯТО |
| P2-6 Порядок слоёв SCSS (shared → screen) | ✅ ПРИНЯТО |
| P2-7 Подписи вкладок 14px, theme-aware active | ✅ ПРИНЯТО |
| P2-8 Поля 15px | ✅ ПРИНЯТО |
| P2-9 Секция соцсетей height AUTO + min-height | ✅ ПРИНЯТО |
| P2-10 Мёртвые классы удалены | ✅ ПРИНЯТО |
| P2-11 Ключи сообщений (msg:///mainMsg://) | ✅ ПРИНЯТО (1 замечание P3, см. §5) |
| P2-12 Captions колонок | ✅ ПРИНЯТО |
| P3-14 Мелочи (width, large, well) | ✅ ПРИНЯТО |

**Итог: редизайн принимается с одним блокирующим дефектом P1-1 (высота навигации 46px).**
После выполнения фикса (§4) и перепроверки по п. 4.5 — пункт закрыть.

---

## 2. Приёмка по пунктам (доказательства)

### P1-1. Label-навигация: правила 27px/3px/20px применены, НО фактическая высота 46px — НЕ ПРИНЯТО

Что сделано верно:
- SCSS (`job-candidate-editor.scss`, 7 идентичных копий, md5 совпадают): `min-height: 27px !important; height: auto !important; padding: 3px 10px !important; line-height: 20px !important` — на месте (строки ~382–405).
- Собранный CSS halo: `.halo .job-candidate-editor .label-nav-item {... min-height:27px!important;height:auto!important;padding:3px 10px!important;line-height:20px!important ...}` — подтверждено.
- Контракт-тест `JobCandidateEditLayoutContractTest` обновлён: ассертит `min-height: 27px !important` и `font-size: 14px !important` — согласовано с реализацией.
- Браузер (halo): computed-значения применяются, НО `getBoundingClientRect().height` = **46px**, контент (`wrap`) = 20px.

Вердикт: правила объявлены корректно, но визуальная цель не достигнута — пункт не принимается. Причина и фикс: §3–4.

### P1-2. Вкладка «Основное»: карточки вертикально, 100% — ПРИНЯТО
- SCSS: из правила `display: flex` исключён селектор `.job-candidate-accordion-open .job-candidate-accordion-content`; flex и `width: calc(50% - 8px)` остались только у `.job-candidate-card-row`.
- Собранный CSS halo: у `.job-candidate-accordion-open .job-candidate-accordion-content` `display:flex` отсутствует; у `.job-candidate-card-row` — присутствует.
- Браузер: «Персональные данные» и «Профессиональные данные» — одна над другой на 100% ширины ✓.

### P1-3. Toolbar: заголовок + описание слева, «Еще» справа — ПРИНЯТО
- XML: в `jobCandidateTopBar` добавлен `jobCandidateToolbarTitleBox` (label `msg://editorCaption` со `stylename="edit-toolbar-title"` + label `mainMsg://msgCandidate` со `stylename="edit-toolbar-description"`), `expand` переведён на него; `moreActionsPopUpButton` остался справа (`edit-toolbar-actions`).
- Собранный CSS: классы `.edit-toolbar-title`/`.edit-toolbar-description` присутствуют (shared); локальный `.job-candidate-top-bar`: `min-height:58px; padding:12px 22px 10px` → 27px (title) + 20px (desc) + 22px padding ≈ 69–70px — согласуется с замером браузера **h=70px** ✓.

### P1-4. Фальшивый «аккордеон»: маркер ▼ убран — ПРИНЯТО
- SCSS: правило `.job-candidate-accordion-header:before { content:"\25BC" ... }` удалено; в собранном CSS halo `\25BC` отсутствует. Заголовки остаются flat-заголовками (без сворачивания), как у эталона.

### P2-5. Sidebar 312/296/284 — ПРИНЯТО
- SCSS/собранный CSS: базовая 312px; `@media(max-width:1366px)` — `width:296px!important` у `.v-slot-job-candidate-sidebar` и `.job-candidate-sidebar` (286px удалён); добавлен тир `@media(max-width:1100px)` — 284px. Браузер: sidebar w=312 ✓.

### P2-6. Порядок слоёв SCSS (контракт 6.4) — ПРИНЯТО
- Все 7 `styles.scss`: `@import`/`@include` `edit-screen-shared-styles` **до** `job-candidate-editor` (проверено построчно).
- `halo-ext.scss` (и аналоги) очищены от дублирующих `@import "job-candidate-editor.scss"` / `@include job-candidate-editor-theme`.
- Собранный CSS halo: shared `.halo .label-nav-item` (24px) стоит раньше локального `.halo .job-candidate-editor .label-nav-item` (27px) — порядок восстановлен.

### P2-7. Подписи вкладок 14px, active theme-aware — ПРИНЯТО
- SCSS: `.job-candidate-tabs .v-caption` `font-size: 12px → 14px !important`; active: `#0b63b6 → $v-selection-color`.
- Собранный CSS halo: `font-size:14px!important`; active `color:#4d7ab2!important; border-bottom:3px solid #4d7ab2!important` (= `$v-selection-color` halo). Отклонение от брендового `#ffb11b` — осознанное theme-aware решение, зафиксировать в спеке (P3-13).

### P2-8. Поля 15px — ПРИНЯТО
- SCSS/собранный CSS: `.job-candidate-form-grid`/`.job-candidate-form-row` подписи и inputs — `font-size:15px !important` (было 16px); высота 38px сохранена.

### P2-9. Секция «Социальные сети» — ПРИНЯТО
- XML: `contactSocialNetworksSection height="560px" → "AUTO"`.
- SCSS/собранный CSS: `#socialNetworkTable { min-height: 320px !important }` — сетка не схлопывается.

### P2-10. Мёртвые классы — ПРИНЯТО
- XML: удалены stylename `job-candidate-half-card`, `job-candidate-contact-card`, `job-candidate-positions-layout`, `job-candidate-table-comments`, `job-candidate-info-grid`, `job-candidate-sidebar-grid`, `job-candidate-name-row` (проверено по diff).
- SCSS/собранный CSS: `.job-candidate-audit-box`/`.job-candidate-audit-label` удалены (в собранном CSS отсутствуют).

### P2-11. Ключи сообщений — ПРИНЯТО
- XML переведён на `msg://`/`mainMsg://` (подписи навигации, карточек, вкладок, кнопок, колонок и т.д.).
- Аудит: из XML используется **85 ключей**; все разрешаются в ru+en (локальный пакет `screens/jobcandidate/messages*.properties` + `web/messages*.properties`) — кроме `msgLastProject` (см. §5, P3-2; унаследованный, вне diff редизайна).
- Новые ключи добавлены в оба файла синхронно.

### P2-12. Captions колонок — ПРИНЯТО
- `networkName`, `vacancy`, `iteractionType`, `recrutier` получили `caption="msg://..."`; ключи есть в ru/en.

### P3-14. Мелочи — ПРИНЯТО
- `birdhDateField`: `width="AUTO"` убран; у label строк контактов убран `width="100px"` (управление из SCSS, `150px → 100px`); у `chatMessageTextField`/`vacancyPopupPickerField` убран `large`; `groupBox well` → `job-candidate-card edit-card`. Всё по diff.

---

## 3. Дефект 46px: точная причина (доказано)

### 3.1. Механизм

Пункты навигации JobCandidateEdit — это **`<button>`** (Vaadin `v-button`), а у эталона IteractionList — **`<label>`**. Разница в высоте порождается внутренней структурой кнопки halo:

1. **Базовая halo-кнопка** (собранный CSS, все halo-базовые темы):
   `.halo .v-button:before { content:""; display:inline-block; width:0; height:100%; vertical-align:middle }`
   — пустой inline-block псевдоэлемент (штатный механизм вертикального центрирования кнопок halo).
2. **Shared label-nav правила** (`edit-screen-shared-styles`):
   `.halo .v-button-label-nav-item .v-button-wrap { display:flex; align-items:center; width:100%; ... }` и
   `.halo .v-button-label-nav-item .v-button-caption { display:block; ... }` — контент кнопки становится **блоковым**.
3. Итог: внутри кнопки формируются **два сложенных line box**:
   - фантомный line box от inline-level `:before` — высотой `line-height` = **20px**;
   - блоковый `wrap`/`caption` — ещё **20px**;
   - плюс вертикальный padding **3px + 3px**.
   **20 + 20 + 6 = 46px** — точно совпадает с замером из браузера (46px; контент wrap 20px).
   `min-height:27px !important` / `height:auto !important` не помогают: высота определяется содержимым (двумя line box), а не min-height.

4. У **label**-навигации эталона нет ни `:before`, ни `wrap` — одна строка 20px + 6px padding = 26px → `min-height:27px` → **27px** ✓.

### 3.2. Эмпирическое доказательство

Headless Chrome (точные правила из собранного halo `styles.css`, те же селекторы и порядок):

| Случай | Высота |
|---|---|
| `<button class="... label-nav-item ...">` (текущее) | **46px** (wrap=20, caption=20) |
| `<label class="label-nav-item">` (эталон) | **27px** |
| button + `:before { display:none !important }` (**фикс A**) | **27px** ✅ |
| button + `.v-button-wrap { display:block !important }` (фикс B) | 46px ❌ (не помогает) |

### 3.3. Область поражения

- Дефект есть **только в halo-базовых темах**: **halo, havana, helium, hover** (в их собранном CSS присутствует `.v-button:before { display:inline-block }`).
- В **hunttech-modern / -light / -dark** правила `.v-button:before` нет (база тем другая) → навигация там уже 27px.
- Эталон IteractionList и `open-position-preview` (тоже использует `label-nav-item` на кнопках) — **не затронуты** исправлением при скоупе на `.job-candidate-editor`.

---

## 4. Точное задание на фикс высоты навигации (46px → 27px)

### 4.1. Файлы

7 идентичных копий (контракт: копии обязаны оставаться md5-идентичными, критерий приёмки design-notes §3.1.1):

```
modules/web/themes/halo/com.company.hunttech/job-candidate-editor.scss
modules/web/themes/havana/com.company.hunttech/job-candidate-editor.scss
modules/web/themes/helium/com.company.hunttech/job-candidate-editor.scss
modules/web/themes/hover/com.company.hunttech/job-candidate-editor.scss
modules/web/themes/hunttech-modern/com.company.hunttech/job-candidate-editor.scss
modules/web/themes/hunttech-modern-light/com.company.hunttech/job-candidate-editor.scss
modules/web/themes/hunttech-modern-dark/com.company.hunttech/job-candidate-editor.scss
```

### 4.2. Правило

Вставить **сразу после** блока геометрии `.label-nav-item` (в файле halo — после строки `opacity: 1 !important;` закрывающей правило, ~строка 405, **до** правила `.label-nav-item .v-button-caption` на ~407), внутри вложенности `.job-candidate-editor`:

```scss
    /* Фикс высоты пункта 46px → 27px (приёмка 2026-08-03):
       базовая halo-кнопка (.halo .v-button:before) вставляет пустой inline-block
       псевдоэлемент; из-за блочного .v-button-wrap/.v-button-caption он образует
       второй line box (20px) и растягивает кнопку до 46px. Скрываем его —
       высота пункта = 20px + 6px padding = 26px → min-height 27px. */
    .label-nav-item:before,
    .job-candidate-nav-item:before,
    .v-button-label-nav-item:before {
      display: none !important;
    }
```

### 4.3. Обоснование выбора

- `!important` **обязателен**: специфичность `.job-candidate-editor .label-nav-item:before` = (0,2,1) равна базовой `.halo .v-button:before` = (0,2,1); `!important` гарантирует победу независимо от порядка правил.
- Селекторы покрывают кнопку навигации (у неё одновременно классы `label-nav-item`, `job-candidate-nav-item` и автогенерируемый Vaadin `v-button-label-nav-item`).
- Вложенность в `.job-candidate-editor` изолирует фикс: IteractionListEdit и `open-position-preview` не затронуты.
- Вариант «переопределить `.v-button-wrap` на `display:block`» — **не работает** (проверено эмпирически, 46px сохраняется) — не использовать.
- Вариант «перевести навигацию на `<label>` как в эталоне» — требует изменения Java (`invoke` → `@Subscribe` click) и **запрещён** ограничениями ревью (presentation-only). Не использовать.
- В 3 modern-темах правило — no-op (там нет `:before`), но добавляется для сохранения идентичности 7 копий.

### 4.4. Шаги и верификация

1. Внести правило в одну копию (halo), синхронно — в остальные 6 (или скопировать файл).
2. `./gradlew :app-web:buildScssThemes --no-daemon` — успех.
3. Проверить собранный CSS (минимум halo + hover): присутствует `.job-candidate-editor .label-nav-item:before { display:none !important }`; правило геометрии 27/3/20 не изменено.
4. `./gradlew :app-web:deploy -x test` + полный restart Tomcat (shutdown → startup), hard reload браузера (кеш off).
5. Браузер (halo, alan): `getBoundingClientRect().height` == **27px** для всех 7 пунктов; текст по центру; hover/active не меняют высоту; переключение пунктов не сдвигает соседние.
6. Регресс: IteractionListEdit и форма open-position-preview выглядят как раньше.
7. `./gradlew :app-core:test --tests '*JobCandidateEditLayoutContractTest*'` — зелёный (тест ассертит 27px/14px, фикс их не трогает).
8. `md5` всех 7 копий `job-candidate-editor.scss` — идентичны.

---

## 5. Замечания P3 (не блокирующие, из приёмки)

1. **Toolbar: дублирование текста.** Заголовок и описание оба рендерят «Кандидат» (`editorCaption=Кандидат`, `mainMsg://msgCandidate=Кандидат`). Описание логично заменить (например, «Карточка кандидата») — контент-решение, требует согласования.
2. **`msgLastProject` не определён ни в одном messages-файле.** Используется на скрытой legacy-заглушке `lastProjects` (`visible="false"`, 1×1px, @Inject-контракт) — caption не рендерится; дефект предшествует редизайну (был в HEAD). Опционально: добавить ключ или убрать `caption`.
3. **`editorCaption` в EN-файле = «Кандидат» (кириллица)** — EN-перевод отсутствует (унаследованный ключ, вне diff редизайна).
4. **`edit-screen-layout` на двух уровнях** (root `<layout>` + `jobCandidateMainLayout`) — осталось из appendix дизайн-ревью (⚠️); не P1/P2, вынести отдельной задачей при следующем заходе.
5. Обновить `JobCandidateEdit_Spec.md` по P3-13 (дизайн-ревью §2.4): 27px достигнуто фиксом, активная вкладка `$v-selection-color` (#4d7ab2 в halo), sidebar 312/296/284, поля 15px, вкладки 14px.

---

## 6. Ограничения (что НЕ трогать)

- Java-контроллер, entity, views, data containers, options, JPQL, actions, invoke — не менялись и менять нельзя.
- ID и типы инъецируемых компонентов (`tabSheetSocialNetworks`, `blockCandidateCheckBox`, скрытые `skillBox`/`lastProjects`/`dictionatysTavlesHBox`) сохранены.
- Эталон IteractionListEdit и экран open-position-preview — вне зоны правок.
