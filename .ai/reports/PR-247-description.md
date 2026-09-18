## Baseline и верификация

- **BASE_SHA**: `ab62c20629f092634b3494775932df102737c815`
- **VERIFIED_HEAD**: `cf879b964bca546548372729f482c79a713929c4`
- **Ветка**: `agent/company-edit-fix` (создана от свежего `origin/master`)
- **Статус верификации**: `VERIFIED`

## Baseline-источники
1. **XML**: `eb795a67` (убрана лишняя карточка `companyMainContactsCard`, контактные поля телефона, email и сайта интегрированы в `companyMainCard`; каноническая разметка адресных полей и описания).
2. **Структурные исправления вкладок и SCSS**: `8f425f0f`
   - XML: `companyEditorWorkspace` (`expand="companyEditorContentScrollBox"`), общий `companyEditorContentScrollBox` вокруг `mainTab`, снят `expand` с 4 вкладок (`tabConpanyDetails`, `companyRequisitesTab`, `companyDescriptionTab`, `tabCompanyDepartament`), вкладочные scrollBox переведены на stylename `company-tab-scroll`, `editActions` (`height="AUTO"`).
   - SCSS: правило `.company-tab-scroll`, замена `overflow: hidden` на `overflow-x: hidden` у `tabsheetpanel`.
3. **Ограничения**:
   - `8478ecd6` и PR #245 не использовались как эталон.
   - Полный revert смешанных PR не применялся.

## Разрешённые изменения (`git diff BASE_SHA...VERIFIED_HEAD`)

Все заявленные файлы реально входят в `git diff BASE_SHA...VERIFIED_HEAD`:

1. `modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml` — канонический XML из `eb795a67` со структурными исправлениями вкладок из `8f425f0f` (плюс i18n ключи `msg://msgPhone`, `msg://msgEmail`, `msg://msgWebsite`).
2. `modules/web/themes/halo/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f`.
3. `modules/web/themes/havana/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f` (MD5 идентичен).
4. `modules/web/themes/helium/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f` (MD5 идентичен).
5. `modules/web/themes/hover/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f` (MD5 идентичен).
6. `modules/web/themes/hunttech-modern-dark/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f` (MD5 идентичен).
7. `modules/web/themes/hunttech-modern-light/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f` (MD5 идентичен).
8. `modules/web/themes/hunttech-modern/com.company.hunttech/company-editor.scss` — эталон SCSS из `8f425f0f` (MD5 идентичен).
9. `modules/core/test/com/company/hunttech/core/CompanyEditLayoutContractTest.java` — контрактный тест актуализирован под канонические селекторы workspace/toolbar и инварианты формы.
10. `build.gradle` — version bump 0.488 -> 0.489 (pre-commit hook).

## MD5-контроль SCSS 7 тем
- `5e536f3dccfb0bcb2083e4a83978af05` — halo
- `5e536f3dccfb0bcb2083e4a83978af05` — havana
- `5e536f3dccfb0bcb2083e4a83978af05` — helium
- `5e536f3dccfb0bcb2083e4a83978af05` — hover
- `5e536f3dccfb0bcb2083e4a83978af05` — hunttech-modern-dark
- `5e536f3dccfb0bcb2083e4a83978af05` — hunttech-modern-light
- `5e536f3dccfb0bcb2083e4a83978af05` — hunttech-modern

## Результаты контрактных тестов
- Команда: `bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*CompanyEdit*ContractTest" --rerun-tasks`
- Результат: `BUILD SUCCESSFUL in 58s` (10/10 задач выполнены, 0 ошибок)
  - `CompanyEditTabLayoutContractTest`: 3/3 тестов зелёные (tabsDoNotCombineExpandWithHeight100, tabScrollBoxesDoNotReuseWorkspaceClasses, tabSheetPanelsKeepVerticalOverflow)
  - `CompanyEditLayoutContractTest`: 8/8 тестов зелёные (usesSharedSidebarAndWorkspaceOrder, usesContractCardAndToolbarClasses, everyInputFieldUsesEditFormControl, visualContractFollowsIteractionListEditReference, uploadButtonsFollowCanonicalDarkSidebarStyle, dataBindingsViewsLoadersAndActionsPreserved, workspaceFieldRowsRemainResponsiveWithoutChangingSidebar, everyThemeAppliesCompanyLocalScss)

## Code Review (Alibaba OCR CLI)
- Команда: `ocr review --audience agent`
- Замечания учтены: хардкод русских подписей заменен на ключи локализации `msg://msgPhone`, `msg://msgEmail`, `msg://msgWebsite`.
