# Арбитражное решение: пункт 5.1 — синхронизация активного пункта sidebar

## Контекст

- Вкладка «Проект» (tabOpenPosition) формы OpenPositionEdit: левая label-навигация в sidebar.
- Nav-кнопки объявлены в XML статически: `openPositionEditorNavIdentifiers` имеет `stylename="borderless label-nav-item label-nav-item-active"`, остальные 5 — только `label-nav-item`.
- В Java (OpenPositionEdit.java) кнопки НЕ инъецированы; обработчик `onTabSheetOpenPositionSelectedTabChange` (строка 426, `@Subscribe("tabSheetOpenPosition")`) управляет только lazy-загрузкой LOB/коллекций и НЕ трогает стили навигации.
- Требование 5.1 задания: активное выделение должно совпадать с реально открытой вкладкой, переключаться при смене вкладки, не допускать двойного выделения, не быть статическим stylename.

## Решение арбитра (одобрено пользователем, 2026-08-05)

**Разрешена минимальная Java-правка** контроллера `OpenPositionEdit.java`:

1. Добавить 6 `@Named`-инъекций кнопок навигации:
   - `openPositionEditorNavIdentifiers`, `openPositionEditorNavSettings`, `openPositionEditorNavTeam`, `openPositionEditorNavProject`, `openPositionEditorNavPersonnel`, `openPositionEditorNavSalary`.
2. В существующем обработчике `onTabSheetOpenPositionSelectedTabChange` (НЕ создавая новый) добавить визуальную синхронизацию: по `event.getSelectedTab().getName()` снять `label-nav-item-active` со всех 6 кнопок и установить его на кнопку, соответствующую активной вкладке/секции.
3. Логика lazy-загрузки (существующие if-блоки loadExerciseLob/loadMemoForInterviewLob/...) — НЕ трогать.
4. Это чисто визуальное поведение: бизнес-логика, сущности, справочники, loaders, JPQL, views не затрагиваются.

## Соответствие вкладок → пунктов навигации

| Вкладка (tab id) | Активный пункт навигации |
|---|---|
| tabOpenPosition (Проект) | openPositionEditorNavIdentifiers (Идентификаторы) |
| laborAgreementTab (Трудовой договор) | openPositionEditorNavSettings (Настройки вакансии) — либо свой пункт; окончательно по диагностике |
| Остальные вкладки | без активного пункта (навигация описывает только секции вкладки «Проект») — по решению субагента 1/контракта |

## Ограничения

- Не добавлять invoke на nav-кнопки (Java-методы для скролла секций не создаются — это выходило бы за визуальный скоуп).
- Кнопки остаются визуальными указателями; клик по ним НЕ переключает вкладку (требование не требует этого — требуется только синхронизация состояния).
- Правка входит в scope корректирующего этапа как CONDITIONALLY_ALLOWED (Java-файл OpenPositionEdit.java — только добавление визуальной синхронизации, без изменения бизнес-логики).

## Дата

2026-08-05
