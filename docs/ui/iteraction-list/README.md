# UI-документация IteractionList — HRM HuntTech

## Назначение и бизнес-смысл (What & Why)

Раздел объединяет living-документацию экранов работы со взаимодействиями кандидата и вакансии. Основной Edit-экран сохраняет бизнес-факт контакта, его тип, результат, рекрутёра и комментарий.

## UI Context & Navigation

- основной editor: `hunttech_IteractionList.edit`;
- sidebar: кандидат, вакансия, status/priority, label-navigation, service/vacancy context;
- workspace: быстрые взаимодействия, четыре блока ввода, footer;
- controller: `IteractionListEdit`;
- descriptor: `iteraction-list-edit.xml`.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть документацию business/UI | требуется понять весь экран | использовать каноническую спецификацию |
| проверить XML-компоновку | изменяется визуальная структура | использовать документ визуального выравнивания |
| проверить inline XML-документацию | добавляется или изменяется элемент descriptor | использовать контракт смысловых комментариев |
| проверить Box ID | создаётся или изменяется layout-контейнер | использовать контракт смысловых ID и профильный тест |
| проверить entity и side effects | изменяется бизнес-логика | использовать entity/spec и не ограничиваться UI-документом |

## Документы

| Документ | Назначение |
|---|---|
| [IteractionListEdit_Spec.md](../IteractionListEdit_Spec.md) | Каноническая спецификация бизнес-логики и UI-контрактов editor |
| [IteractionListEdit_XmlLayout_2026-07-27.md](../IteractionListEdit_XmlLayout_2026-07-27.md) | Плоская XML-компоновка четырёх постоянных блоков |
| [IteractionListEdit_VisualAlignment_2026-07-28.md](../IteractionListEdit_VisualAlignment_2026-07-28.md) | Девять точных visual contracts: изображения, navigation, picker, AUTO и sidebar cards |
| [IteractionListEdit_XmlSemanticComments_2026-07-28.md](../IteractionListEdit_XmlSemanticComments_2026-07-28.md) | Обязательные смысловые комментарии перед каждым XML-элементом и автоматическая проверка покрытия |
| [IteractionListEdit_BoxIdContract_2026-07-28.md](../IteractionListEdit_BoxIdContract_2026-07-28.md) | Уникальные смысловые ID всех `vbox`, `hbox`, `scrollBox` и `buttonsPanel` |
| [itpearls_IteractionList.edit_Spec.md](../itpearls_IteractionList.edit_Spec.md) | Legacy screen-spec и cross-reference |
| [itpearls_IteractionList.browse_Spec.md](../itpearls_IteractionList.browse_Spec.md) | Browse взаимодействий |
| [itpearls_IteractionListBrowse_Spec.md](../itpearls_IteractionListBrowse_Spec.md) | Фрагмент взаимодействий в карточке кандидата |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Добавлен контракт уникальных смысловых ID всех Box-компонентов `IteractionListEdit`. |
| 2026-07-28 | Добавлен контракт смысловых комментариев перед каждым XML-элементом `IteractionListEdit` и ссылка на профильный тест. |
| 2026-07-28 | Создан профильный индекс IteractionList; добавлена ссылка на точное визуальное выравнивание XML. |
