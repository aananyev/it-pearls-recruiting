# Спецификация «класс → правило контракта → тема» для таблиц OpenPositionEdit

| Компонент (класс)           | Правило контракта (stylename)               | Тема(ы)                                                                 |
|-----------------------------|---------------------------------------------|-------------------------------------------------------------------------|
| laborAgreementDataGrid      | open-position-editor-table-variant5         | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark |
| someFilesTable              | open-position-editor-table-variant5         | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark |
| openPositionSkillsListTable | open-position-editor-table-variant5         | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark |
| openPostionNewsDataGrid     | open-position-editor-table-variant5         | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark |

**Пояснение:**  
Все четыре таблицы в форме `OpenPositionEdit` используют общий визуальный контракт `table-variant5`, реализованный через локальный stylename `open-position-editor-table-variant5`. Этот stylename определён в共享 mixin `edit-screen-shared-styles.scss` и синхронно подключён во всех семи темах проекта. Контракт задаёт геометрию, отступы, поведение при ховере/активном состоянии и отсутствие горизонтального скролла (кроме специализированных случаев), согласно правилам разделов 5.5 и 5.6 общего контракта стилей Edit-экранов.