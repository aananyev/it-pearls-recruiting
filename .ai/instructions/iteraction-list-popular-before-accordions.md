# Hermes — проверка блока быстрых взаимодействий IteractionListEdit

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

Проверить точный HEAD ветки `agent/iteraction-list-popular-before-accordions` и PR напрямую в `master` без изменения кода и документации.

## Проверка SHA и PR

1. Ветка существует.
2. Branch HEAD = переданному SHA.
3. PR открыт из `agent/iteraction-list-popular-before-accordions` в `master`.
4. PR HEAD = переданному SHA.
5. Base = `master`.
6. Conflicts = `NONE`.

Несовпадение любого SHA: `HEAD_MISMATCH`, проверку остановить.

## Команды

```bash
git diff --check

./gradlew :app-core:test \
  --tests "com.company.hunttech.core.IteractionListEditAccordionLayoutTest" \
  --tests "com.company.hunttech.core.IteractionListMostPopularInteractionTest" \
  --tests "com.company.hunttech.core.IteractionListAccordionNavigationTest" \
  --tests "com.company.hunttech.core.IteractionListRpcCompatibilityContractTest" \
  --no-daemon --stacktrace

./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

## Runtime smoke

1. Выполнить clean local deploy.
2. Проверить `http://localhost:8080/hrm/` — HTTP 200.
3. Открыть `IteractionListEdit`.
4. Подтвердить, что внутри вкладки непосредственно перед `participantsAccordion` видны ровно пять кнопок.
5. При наличии статистики подписи кнопок читаемы; при нехватке данных позиции «Нет данных» остаются видимыми и disabled.
6. Клик по заполненной кнопке устанавливает точный `iteractionTypeField`.
7. Блок не исчезает при раскрытии и сворачивании рабочих аккордеонов.
8. Проверить candidate/vacancy, lookup/open, фильтр подписок, type, dynamic fields, rating, recruiter, communication, comment, subscription, save/cancel.
9. Tomcat logs: новых `Cannot get unfetched attribute`, `ClassCastException`, `IllegalStateException`, P1 и P2 нет.

## Ограничения

- Код и `docs/` не менять.
- Commit, push, rebase, merge не выполнять.
- Production не изменять.

Успех: `STATUS: READY_TO_MERGE`, с формулировкой `проверен HEAD: <SHA>`.
