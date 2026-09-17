# UI-документация IteractionList — HRM HuntTech

## Назначение и бизнес-смысл (What & Why)

Раздел объединяет living-документацию экранов работы со взаимодействиями кандидата и вакансии. Основной Edit-экран сохраняет бизнес-факт контакта, его тип, результат, рекрутёра и комментарий.

## UI Context & Navigation

- основной editor: `hunttech_IteractionList.edit`;
- sidebar: visual identity кандидата/проекта, индекс разделов, номер/дата взаимодействия, status/priority и контекст вакансии;
- workspace: toolbar, пять быстрых взаимодействий, четыре независимых бизнес-раздела и фиксированный footer;
- controller: `IteractionListEdit`;
- active descriptor: `iteraction-list-edit.xml`;
- archived descriptor: `iteraction-list-edit-old.xml`.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть документацию business/UI | требуется понять весь экран | использовать каноническую спецификацию и актуальный redesign contract |
| проверить XML-компоновку | изменяется визуальная структура | использовать документ полного redesign и XML layout history |
| проверить правую рабочую область | меняется геометрия workspace или строк полей | использовать актуальный redesign contract; workspace-документ 2026-09-17 считать историческим контрактом единой карточки |
| проверить inline XML-документацию | добавляется или изменяется элемент descriptor | использовать контракт смысловых комментариев |
| проверить Box ID | создаётся или изменяется layout-контейнер | использовать контракт смысловых ID и профильный тест |
| проверить entity и side effects | изменяется бизнес-логика | использовать entity/spec и не ограничиваться UI-документом |

## Документы

| Документ | Назначение |
|---|---|
| [IteractionListEdit_Spec.md](../IteractionListEdit_Spec.md) | Каноническая спецификация бизнес-логики и UI-контрактов editor |
| [IteractionListEdit_Redesign_2026-09-17.md](../IteractionListEdit_Redesign_2026-09-17.md) | Актуальный контракт полностью перепроектированной формы: standard sidebar + четыре business sections + quick-actions + dynamic/calendar bindings |
| [IteractionListEdit_WorkspaceLayout_2026-09-17.md](../IteractionListEdit_WorkspaceLayout_2026-09-17.md) | Исторический контракт предыдущей единой карточки; не определяет активную структуру после полного redesign |
| [IteractionListEdit_XmlLayout_2026-07-27.md](../IteractionListEdit_XmlLayout_2026-07-27.md) | Исторический XML-контракт плоской компоновки |
| [IteractionListEdit_VisualAlignment_2026-07-28.md](../IteractionListEdit_VisualAlignment_2026-07-28.md) | Базовые visual contracts: изображения, picker, AUTO и sidebar cards |
| [IteractionListEdit_XmlSemanticComments_2026-07-28.md](../IteractionListEdit_XmlSemanticComments_2026-07-28.md) | Обязательные смысловые комментарии перед XML-элементами и автоматическая проверка покрытия |
| [IteractionListEdit_BoxIdContract_2026-07-28.md](../IteractionListEdit_BoxIdContract_2026-07-28.md) | Уникальные смысловые ID `vbox`, `hbox`, `scrollBox` и `buttonsPanel` |
| [itpearls_IteractionList.edit_Spec.md](../itpearls_IteractionList.edit_Spec.md) | Legacy screen-spec и cross-reference |
| [itpearls_IteractionList.browse_Spec.md](../itpearls_IteractionList.browse_Spec.md) | Browse взаимодействий |
| [itpearls_IteractionListBrowse_Spec.md](../itpearls_IteractionListBrowse_Spec.md) | Фрагмент взаимодействий в карточке кандидата |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-17 | `IteractionListEdit` перепроектирован с нуля: старый descriptor архивирован как `old`, активный screen использует стандарт HuntTech `sidebar + workspace`, четыре независимых business sections, сохранённые quick-actions, dynamic fields и calendar binding. |
| 2026-09-17 | Добавлен предыдущий контракт компоновки правой рабочей области `IteractionListEdit`: верхнее выравнивание workspace, устойчивый двухколоночный CSS Grid flow и вертикальный ритм `12px`; после полного redesign документ сохранён как исторический. |
| 2026-07-28 | Добавлен контракт уникальных смысловых ID всех Box-компонентов `IteractionListEdit`. |
| 2026-07-28 | Добавлен контракт смысловых комментариев перед каждым XML-элементом `IteractionListEdit` и ссылка на профильный тест. |
| 2026-07-28 | Создан профильный индекс IteractionList; добавлена ссылка на точное визуальное выравнивание XML. |
