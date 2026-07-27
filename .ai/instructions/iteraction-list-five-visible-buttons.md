# Hermes — проверка пяти видимых кнопок IteractionListEdit

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

Проверить точный HEAD ветки `agent/iteraction-list-five-visible-buttons-clean` и PR №82 напрямую в `master`. Код и документацию не изменять; commit, push, rebase, merge и production не выполнять.

## Неизменённая бизнес-логика

- `IteractionListEdit` по-прежнему вызывает `InteractionService.getMostPolularIteraction(currentUser, 5)`;
- сервис анализирует последний календарный месяц;
- фильтр текущего рекрутёра, group by type и count DESC не изменены;
- активная кнопка устанавливает точный `Iteraction` в `iteractionTypeField`;
- `InteractionServiceBean.java` и базовый `setMostPopularIteraction()` не изменены.

## Исправление presentation-слоя

- `IteractionListEditAccordionNavigation` расширяет основной screen ID `hunttech_IteractionList.edit`;
- после базового `BeforeShow` расширение считает уже созданные компоненты `mostPopularHbox`;
- недостающие позиции до пяти добавляются как disabled-кнопки «Нет данных»;
- пустая позиция не имеет click listener, не выполняет запрос и не меняет сущность;
- все пять позиций имеют равную ширину и видны перед `participantsAccordion`.

## Команды

```bash
git diff --check
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListRpcCompatibilityContractTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: профильные тесты PASS, `ScreenViewIntegrityTest 8/8 PASS`, SCSS PASS, `BUILD SUCCESSFUL`.

## Runtime smoke

1. Clean local deploy и HTTP `/hrm/` = 200.
2. Открыть `IteractionListEdit` пользователем без статистики за месяц: видны пять disabled-кнопок «Нет данных».
3. Открыть пользователем с 1–4 типами: всегда пять позиций, найденные активны, остальные disabled.
4. Открыть пользователем с 5+ типами: видны пять активных кнопок в порядке сервиса.
5. Клик по каждой активной кнопке устанавливает точный `Iteraction`; disabled-позиции ничего не меняют.
6. Проверить required/comment/addDate/addString/addInteger/buttonCallAction, save/cancel и Tomcat logs.

Успех: `STATUS: READY_TO_MERGE`, `проверен HEAD: <SHA>`, P1=0, P2=0. Новый commit аннулирует отчёт.
