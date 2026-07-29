# Проверка видимости названий частых взаимодействий

## Границы проверки

- проект: HRM HuntTech;
- ветка: `agent/iteraction-list-popular-captions`;
- base: `master`;
- режим: проверка без изменения кода;
- production-серверы не использовать и не изменять.

## Сценарий

1. Сверить HEAD ветки с SHA в PR и убедиться, что base — `master`.
2. Выполнить `./gradlew :app-web:buildScssThemes --no-daemon --stacktrace`.
3. Выполнить `./gradlew :app-core:test --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' --no-daemon --stacktrace`.
4. В локальном развёртывании открыть `hunttech_IteractionList.edit` под пользователем с частыми взаимодействиями.
5. Убедиться, что каждая из пяти верхних кнопок показывает полное или переносимое наименование соответствующего `Iteraction.iterationName`, а placeholder остаётся disabled.
6. Нажать одну активную кнопку и подтвердить: в поле типа взаимодействия устанавливается соответствующее значение; сохранение и остальные действия формы не выполнять.

## Инварианты

- Не изменять `IteractionListEdit.java`, XML, loaders, views, entity, service, JPQL, actions или data binding.
- Изменения ограничены локальными SCSS-стилями `iteraction-list-*`, регрессионным тестом и документацией.
