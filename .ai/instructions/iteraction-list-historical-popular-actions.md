# Hermes — проверка исторического контракта быстрых взаимодействий

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

Проверить точный HEAD ветки `agent/iteraction-list-historical-popular-actions` и PR напрямую в `master`. Код и документацию не изменять, commit/push/rebase/merge и production не выполнять.

## Контракт

- `IteractionListEdit` вызывает `InteractionService.getMostPolularIteraction(currentUser, 5)`;
- сервис анализирует последний календарный месяц;
- выборка ограничена текущим рекрутёром, группируется по типу и сортируется по count DESC;
- форма показывает до пяти фактически найденных зелёных кнопок без placeholders;
- клик устанавливает точный `Iteraction` в `iteractionTypeField` и запускает штатный value-change handler;
- host расположен внутри вкладки перед `participantsAccordion`.

## Команды

```bash
git diff --check

./gradlew :app-web:compileJava :app-core:compileTestJava   --no-daemon --stacktrace

./gradlew :app-core:test   --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest'   --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: профильный тест `6/6 PASS`, `ScreenViewIntegrityTest 8/8 PASS`, SCSS PASS, `BUILD SUCCESSFUL`.

## Runtime smoke

1. Выполнить clean local deploy и открыть новый и существующий `IteractionListEdit`.
2. Проверить HTTP `http://localhost:8080/hrm/` = 200 и отсутствие critical Tomcat errors.
3. Для пользователя с историей за последний месяц сопоставить кнопки с SQL/JPQL count: порядок по убыванию, максимум пять.
4. Для пользователя без истории подтвердить отсутствие искусственных кнопок «Нет данных» и стабильность layout.
5. Нажать каждую доступную кнопку: `iteractionTypeField` получает точный тип, обновляются required/comment/addDate/addString/addInteger/buttonCallAction по штатной логике.
6. Проверить save/cancel/subscription без регрессии.

Успех: `STATUS: READY_TO_MERGE` с формулировкой `проверен HEAD: <SHA>`, P1=0, P2=0. Новый commit аннулирует отчёт.
