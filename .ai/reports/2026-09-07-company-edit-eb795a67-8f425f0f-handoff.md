# Отчёт передачи CompanyEdit (Hermes-1 / Antigravity)

**Дата**: 2026-09-07  
**Задача**: Восстановление экрана `CompanyEdit` в новой ветке от свежего `origin/master` по строгому baseline:
- XML: `eb795a67`
- Структурные исправления вкладок и SCSS: `8f425f0f`
- Запрет эталонов: `8478ecd6` и PR #245 не использовались
- Запрет: без полного revert смешанных PR

---

## 1. Контрольные суммы и SHA

- **BASE_SHA**: `ab62c20629f092634b3494775932df102737c815` (свежий `origin/master`)
- **VERIFIED_HEAD**: `cf879b964bca546548372729f482c79a713929c4`
- **PR**: #247 (base: `master`, head: `agent/company-edit-fix`)
- **Метка**: `WAITING_FOR_HERMES`

---

## 2. Разрешённый diff (`BASE_SHA...VERIFIED_HEAD`)

Команда проверки:
```bash
git diff --stat ab62c20629f092634b3494775932df102737c815...cf879b964bca546548372729f482c79a713929c4
```

Состав diff:
1. `modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml` — базовый XML `eb795a67` (убрана `companyMainContactsCard`, контактные поля в `companyMainCard`, канонические карточки адреса и описания) + структурные исправления `8f425f0f` (`companyEditorContentScrollBox`, снятие `expand` с 4 вкладок, класс `company-tab-scroll`, `editActions height="AUTO"`, локализованные `msg://` ключи телефонов/email/сайта).
2. `modules/web/themes/halo/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
3. `modules/web/themes/havana/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
4. `modules/web/themes/helium/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
5. `modules/web/themes/hover/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
6. `modules/web/themes/hunttech-modern-dark/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
7. `modules/web/themes/hunttech-modern-light/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
8. `modules/web/themes/hunttech-modern/com.company.hunttech/company-editor.scss` — эталон SCSS `8f425f0f`.
9. `modules/core/test/com/company/hunttech/core/CompanyEditLayoutContractTest.java` — контрактный тест актуализирован под канонические селекторы workspace/toolbar.
10. `build.gradle` — version bump 0.488 -> 0.489.

---

## 3. MD5-контроль SCSS (все 7 тем идентичны)

```
5e536f3dccfb0bcb2083e4a83978af05  halo/company-editor.scss
5e536f3dccfb0bcb2083e4a83978af05  havana/company-editor.scss
5e536f3dccfb0bcb2083e4a83978af05  helium/company-editor.scss
5e536f3dccfb0bcb2083e4a83978af05  hover/company-editor.scss
5e536f3dccfb0bcb2083e4a83978af05  hunttech-modern-dark/company-editor.scss
5e536f3dccfb0bcb2083e4a83978af05  hunttech-modern-light/company-editor.scss
5e536f3dccfb0bcb2083e4a83978af05  hunttech-modern/company-editor.scss
```

---

## 4. Результаты контрактных тестов

```bash
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*CompanyEdit*ContractTest" --rerun-tasks
```
**Результат**: `BUILD SUCCESSFUL in 58s`, 10/10 tasks executed.

- `CompanyEditTabLayoutContractTest` (3 теста) — ✅ 3/3 PASS
  - `tabsDoNotCombineExpandWithHeight100` — PASS
  - `tabScrollBoxesDoNotReuseWorkspaceClasses` — PASS
  - `tabSheetPanelsKeepVerticalOverflow` — PASS
- `CompanyEditLayoutContractTest` (8 тестов) — ✅ 8/8 PASS
  - `everyInputFieldUsesEditFormControl` — PASS
  - `visualContractFollowsIteractionListEditReference` — PASS
  - `uploadButtonsFollowCanonicalDarkSidebarStyle` — PASS
  - `usesSharedSidebarAndWorkspaceOrder` — PASS
  - `dataBindingsViewsLoadersAndActionsPreserved` — PASS
  - `workspaceFieldRowsRemainResponsiveWithoutChangingSidebar` — PASS
  - `usesContractCardAndToolbarClasses` — PASS
  - `everyThemeAppliesCompanyLocalScss` — PASS

---

## 5. Alibaba OCR Review

- Команда: `ocr review --audience agent`
- Сессия: `e8b7b6d0-bd68-4ff3-b693-12c4e4829637`
- Замечания по hardcoded Russian captions устранены (`msg://msgPhone`, `msg://msgEmail`, `msg://msgWebsite`).
