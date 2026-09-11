# Задача: Отображение названия взаимодействия на верхних быстрых кнопках в IteractionListEdit

## Дата: 2026-09-10
## Ветка: agent/antigravity-dev
## Роли: Аналитик, Java Backend-разработчик, Автоматизированный тестировщик

### Описание задачи
В форме `IteractionListEdit` на верхних зеленых кнопках должна быть надпись, в которой отображается часто используемое взаимодействие. Точно такое же, какое появляется при наведении курсора на эту кнопку (всплывающий tooltip / description): именно этот текст (`interaction.getIterationName()`) должен быть на самой кнопке.

### Реализация
1. В `IteractionListEdit.java`:
   - В методе `createPopularInteractionButton(Iteraction interaction)`:
     - Подпись кнопки `popularButton.setCaption(interaction.getIterationName())` теперь в точности совпадает со всплывающей подсказкой `popularButton.setDescription(interaction.getIterationName())`.
     - Убран неиспользуемый параметр `int index` и обновлён Javadoc.
2. В `IteractionListMostPopularInteractionTest.java`:
   - Актуализированы проверки подписи быстрой кнопки: проверяется совпадение `popularButton.setCaption(interaction.getIterationName())` и `popularButton.setDescription(interaction.getIterationName())`.
3. Тесты и ревью:
   - `:app-core:test --tests com.company.hunttech.core.IteractionListMostPopularInteractionTest` успешно пройден (BUILD SUCCESSFUL).
   - `ocr review --audience agent`: 0 замечаний (0 finding(s)).

### Статус
Выполнено. Готово к коммиту и пушу.
