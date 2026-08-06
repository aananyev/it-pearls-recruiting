# OpenPositionEdit — 06. CUBA-верификация корректирующего этапа: вкладка «Проект» (tabOpenPosition)

> Роль субагента: **CUBA-верификатор**. Подтверждение ОТСУТСТВИЯ функциональных изменений в корректирующем
> этапе редизайна вкладки «Проект» формы OpenPositionEdit. Код НЕ изменялся и НЕ исправлялся — только
> сравнение до/после и отчёт.
> Дата: 2026-08-05 · Ветка: `agent/open-position-edit-redesign` · HEAD: `2ff1f129ec1378c043293a8d7ba30f77316e0988` (= master).
> Метод: `git diff HEAD` + программное сравнение XML (master `git show HEAD:` vs рабочее дерево) по
> функциональным атрибутам; SCSS-анализ namespace/селекторов/md5; Java-diff посимвольно.

---

## 1. Охват и методология

| Область | Источник «до» | Источник «после» | Метод |
|---|---|---|---|
| XML-экран | `git show HEAD:…open-position-edit.xml` | рабочий файл | программный разбор: id/тип/функц. атрибуты, 215 vs 242 id, полный перебор атрибутов |
| Java-контроллер | `git show HEAD:…OpenPositionEdit.java` | рабочий файл | `git diff HEAD` (+27/−0) |
| Локальный SCSS | `01-diagnostic.md` §5.2 (md5 `f4758dc4…`, 1086 строк) | 7 рабочих копий (md5 `2451c61b…`, 1114 строк) | md5-сравнение 7 копий, глубина скобок, запрещённые селекторы |
| Shared SCSS | `01-diagnostic.md` §5.1 | рабочие файлы | `git status` (изменений нет) |
| Сущности/views/другие формы | `git show HEAD:` | рабочие файлы | `git status --short` (изменений нет) |
| Артефакты этапа | `00-arbitration-sidebar-active.md`, `02-layout-contract.md`, `03-component-map.csv`, `04-implementation-report.md` | — | сверка фактов |

**Важно:** в рабочем дереве присутствуют и незакоммиченные правки ПРЕДЫДУЩЕГО этапа редизайна (sidebar,
карточки, messages, styles.scss ×7 и т.д.). Верификация корректирующего этапа проводилась выборочно:
изменения корректирующего этапа = только E (XML: разбиение `vacancyNameHBox` + новый `gradeActionRowHBox`),
B/C/D (SCSS), A (Java, по вердикту арбитра). Все прочие диффы против master отнесены к предыдущему этапу
(зафиксированы в `01-diagnostic.md`) и функциональных атрибутов не меняют.

---

## 2. Таблица проверок

| # | Атрибут / область | Вердикт | Доказательство |
|---|---|---|---|
| 1 | **Component IDs** (вкладка «Проект») | **UNCHANGED** | master: 60 id в tabOpenPosition, work: 62. Все 60 общих id присутствуют. Единственное добавление корректирующего этапа — `gradeActionRowHBox` (новый визуальный контейнер, заявлен контрактом E и `03-component-map.csv`). `citiesLabel`/`hboxProject1` (только в master) и `openPositionEditorCardsRow1/2`, `openPositionEditorIdentifiersCard` (только в work) — правки предыдущего этапа. `gradeActionRowHBox` встречается в XML ровно 1 раз (уникален). |
| 2 | **XML component types** | **UNCHANGED** | Все типы совпадают: `vacansyIDTextField`/`vacansyNameField` = textField, `gradeLookupPickerField` = lookupPickerField, `generateVacancyNameFieldButton` = button, `commandOrPosition` = radioButtonGroup, `parentOpenPositionField` = lookupPickerField, `numberPositionField` = textField, `openPositionFieldSalary*` = textField. Единственное изменение типа по всему файлу — `citiesLabelHBox` hbox→vbox (предыдущий этап, контейнер без Java-привязок). |
| 3 | **property / dataContainer / optionsContainer** | **UNCHANGED** | Полный перебор по всему файлу: 0 диффов по `property`/`dataContainer`/`optionsContainer` для всех общих id. В строках E: `vacansyIDTextField` (dataContainer=openPositionDc, property=vacansyID), `vacansyNameField` (openPositionDc, vacansyName, required), `gradeLookupPickerField` (openPositionDc, grade, **optionsContainer=gradeDc**), `generateVacancyNameFieldButton` (без биндингов, как в master). box.expandRatio сохранены (ID=1, Вакансия=8, Грейд=1). |
| 4 | **actions / invoke / handlers** | **UNCHANGED** | Множества `invoke` идентичны (7 пар в master и work): `addCity→addListCity`, `addOpenPositionNewsButton→addOpenPositionNewsButton`, **`generateVacancyNameFieldButton→generateNameFieldButton` (сохранён!)**, `rescanSkills→rescanJobDescription`, `scanJDButton→addShortDescription`, `setSalaryFieldButton→setSalaryFieldButtonInvoke`, `subscribePositionButton→subscribePosition`. **Nav-кнопки `openPositionEditorNavIdentifiers/Settings/Team/Project/Personnel/Salary` — БЕЗ invoke (6/6 проверено в XML), как требует вердикт арбитра.** Внутренний action `picker_lookup` в `gradeLookupPickerField` сохранён; секция `<actions>` файла идентична. |
| 5 | **visible / enabled / readonly / required** | **UNCHANGED** | Полный перебор: 0 диффов по `visible`/`enable`/`editable`/`readonly`/`required`/`caption`. Точечно: `ownerTextField` — `enable="false"` + `editable="false"` идентичны master; `tabPayments` — `visible="false"` сохранена (16 вкладок, id-наборы и visible/caption/icon равны; в master у tabPayments опечатка `margin="tue,…"` — в work исправлено на `true,…`, косметика предыдущего этапа); `required="true"` на `vacansyNameField`, `commandOrPosition`, `numberPositionField`, `positionTypeField`, `remoteWorkField`, `projectNameField`, `companyNameField`, `companyDepartamentField`, `cityOpenPositionField` — все сохранены. |
| 6 | **loaders / JPQL / views / DataContext** | **UNCHANGED** | `<data>`-секция master и work идентичны после нормализации (12 491 байт обе; `normalize()` с учётом комментариев — True). Все 14 загрузчиков (`openPositionDc`, `laborAgreementDc`, `commentsOpenPositionDc`, `someFilesesDc`, `openPositionSkillsListsDc`, `procAttachmentsDc`, `openPositionParentDc`, `positionTypesDc`, `openPositionNewsDc`, `projectNamesDc`, `companyNamesDc`, `companyDepartamentsDc`, `citiesDc`, `gradeDc`) идентичны: entity class, view, JPQL-запросы, cacheable, условия `<c:jpql>`. views.xml не изменялся (отсутствует в `git status`). |
| 7 | **Сущности, справочники, другие формы** | **UNCHANGED** | `git status --short`: нет изменений entity-классов, справочников, `open-position-edit-preview.xml`, `OpenPositionEditPreview.java`, browse-экранов, views. Новые untracked-файлы — только тесты предыдущего этапа (`OpenPositionEditLayoutContractTest.java`, `OpenPositionEditDetachedObjectTest.java`) и артефакты (`.team/`, `docs/`, `.ai/`). |
| 8 | **Java (OpenPositionEdit.java)** | **UNCHANGED** (функционально) | `git diff HEAD` = **+27 / −0**. Содержимое: (а) 6 `@Named`-инъекций `Button openPositionEditorNav{Identifiers,Settings,Team,Project,Personnel,Salary}` — id точно соответствуют кнопкам XML (6/6 FOUND); (б) блок визуальной синхронизации в СУЩЕСТВУЮЩЕМ `onTabSheetOpenPositionSelectedTabChange` (removeStyleName ×6, addStyleName ×1), размещён ДО раннего return, с null-защитой `event.getSelectedTab()`. Lazy-загрузка не тронута: `PersistenceHelper.isNew(getEditedEntity())` ранний return сохранён, if-блоки `loadExerciseLob`/`loadMemoForInterviewLob` и др. на месте, других изменений нет (нет правок сохранения, закрытия, расчётов, бизнес-логики). Запрещённые контрактным тестом строки в Java: `open-position-editor`=0, `edit-footer-actions`=0, `edit-accordion-section`=0. |
| 9 | **Локальный SCSS (изоляция)** | **PASS** | 7 копий `open-position-editor.scss` **md5-идентичны** (`2451c61b5823728601f9c826f4455998`, все 7). Структура: `@mixin open-position-editor-theme { .open-position-editor { … } }` — все правила (в т.ч. новые `row-grade`, `cards-row` column, удалённые мёртвые nth-child(5)/(7)) вложены в namespace (глубина скобок: mixin=1, namespace=2, правила=3; cards-row на стр. 503–522 на глубине 3). Глобальных `.v-*` селекторов — 0 (полный скан), вложенных `@media` — 0, баланс скобок 126/126→0. Новые/изменённые правила корректирующего этапа: `row-title` сокращён до nth-child(1) (ID, `flex: 0 1 130px`) + `.v-expand` (Вакансия); добавлен `row-grade` (Грейд `flex: 3 1 420px`, кнопка `flex: 0 0 auto`, `width: auto !important` через `> .v-slot:last-child > *`); `cards-row` → `flex-direction: column !important` + `flex-wrap: nowrap` + слоты `width: 100% !important`. |
| 10 | **Shared SCSS (edit-screen-shared-styles.scss)** | **UNCHANGED** | 7 копий `edit-screen-shared-styles.scss` отсутствуют в `git status` — не изменялись. `styles.scss` ×7 содержат только `@import`/`@include open-position-editor` (+2 строки, правка предыдущего этапа). |
| 11 | **Прочее** | — | `git diff --check` — чистый (exit 0). `messages.properties`/`messages_ru.properties` — только presentation-ключи предыдущего этапа (`openPositionEditor*` подписи, комментарий «только presentation»), корректирующий этап их не трогал. Оба XML (master и work) проходят `xml.dom.minidom` parse. |

---

## 3. Детальная сверка строк E (единственное XML-изменение корректирующего этапа)

**Строка 1 — `vacancyNameHBox`** (work): `vacansyIDTextField` + `vacansyNameField`. Все функциональные
атрибуты идентичны master (dataContainer/property/caption/box.expandRatio/width/required). Изменён только
`stylename` (`large` → `edit-form-control` — предыдущий этап) и добавлен stylename контейнера
(`open-position-editor-field-row open-position-editor-row-title` — предыдущий этап).

**Строка 2 — `gradeActionRowHBox`** (новый контейнер, work): `gradeLookupPickerField` +
`generateVacancyNameFieldButton`. Атрибуты полей ПОЛНОСТЬЮ идентичны master (перенесены без изменений):
- `gradeLookupPickerField`: dataContainer=openPositionDc, property=grade, box.expandRatio=1,
  **optionsContainer=gradeDc**, width=100%, caption=msgGrade, внутренний `<action id="lookup" type="picker_lookup"/>`;
- `generateVacancyNameFieldButton`: caption=msgGenerateName, description=msgGenerateNameDesc, width=AUTO,
  align=BOTTOM_RIGHT, **invoke="generateNameFieldButton"**.

Единственное изменение — родительский контейнер (reparent) и удаление `stylename="large"` (визуальное,
предыдущий этап). Reparent допустим: `03-component-map.csv` фиксирует «reparent» в allowed_visual_change,
Java-привязки идут по id (`@Inject`/`@Subscribe`), контейнеры `vacancyNameHBox`/`gradeActionRowHBox` в Java
не инжектятся (подтверждено `01-diagnostic.md` §8 и отсутствием ссылок в diff).

---

## 4. Итоговые строки контракта

```
FUNCTIONAL_CONTRACT: UNCHANGED
ENTITY_CONTRACT: UNCHANGED
REFERENCE_DATA_CONTRACT: UNCHANGED
OTHER_SCREENS: UNCHANGED
LOCAL_SCSS_ISOLATION: PASS
```

---

## 5. Отклонения

Отклонений, затрагивающих функциональность, НЕ выявлено. Единственные отличия от master в пределах
корректирующего этапа — строго визуальные и задокументированные:

1. XML: новый контейнер `gradeActionRowHBox` + перенос двух компонентов из `vacancyNameHBox`
   (все функциональные атрибуты сохранены; `invoke="generateNameFieldButton"` сохранён).
2. Java: +27 строк — 6 `@Named`-инъекций и визуальная синхронизация `label-nav-item-active`
   (по вердикту арбитра `00-arbitration-sidebar-active.md`, CONDITIONALLY_ALLOWED).
3. SCSS ×7: `cards-row` → column, `row-title` очищен от мёртвых селекторов, добавлен `row-grade`
   (все изменения внутри namespace `.open-position-editor`).

---

STATUS: VERIFIED
