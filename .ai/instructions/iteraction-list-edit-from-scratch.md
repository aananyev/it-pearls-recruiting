# Hermes — проверка IteractionListEdit после полного рефакторинга

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

Проверить только точный HEAD draft PR из ветки:

```text
agent/iteraction-list-edit-from-scratch
```

Base должен быть `master`. Точный полный HEAD SHA указан в описании PR. Перед запуском команд подтвердить:

```text
branch HEAD = PR HEAD = переданному SHA
base = master
conflicts = NONE
```

Несовпадение означает `HEAD_MISMATCH`; проверку остановить.

## Режим работы

Hermes выполняет проверку без изменения функционального кода и документации.

Запрещены:

- изменения Java, XML, SCSS, tests и `docs/`;
- commit, push, rebase, merge;
- разрешение конфликтов;
- production deploy, restart, миграции и изменения БД.

## Неизменяемые бизнес-контракты

- `InteractionService.getMostPolularIteraction(currentUser, 5)`;
- текущий пользователь из `UserSession`;
- период последнего календарного месяца;
- `group by iteractionType`;
- `order by count DESC`;
- точный объект `Iteraction` внутри click listener;
- штатный `iteractionTypeField` value-change handler;
- entity, БД, Liquibase, loaders, JPQL и views;
- component ID, bindings, required, validators, actions и `invoke`;
- subscriptions, notifications, dynamic fields и save lifecycle.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*IteractionList*Test' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

```text
IteractionList tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
buildScssThemes: PASS для семи тем
clean assemble: BUILD SUCCESSFUL
```

## Статический и DOM-контракт

Проверить:

1. XML порядок: sidebar → workspace → toolbar → quick actions → scroll → footer.
2. В XML отсутствует промежуточный `TabSheet`.
3. Четыре реальных аккордеона имеют `height=AUTO`.
4. Первый аккордеон открыт, остальные закрыты.
5. `label-navigation` содержит четыре рабочих пункта.
6. Active navigation сохраняет `label-nav-item` и добавляет только `label-nav-item-active`.
7. Ручное раскрытие GroupBox синхронизирует active-state.
8. Presentation-controller не содержит `DataManager`, loader calls, commit, `getEditedEntity()` или `setValue()`.
9. Пять quick-action позиций видимы постоянно и равны по ширине.
10. Placeholder `Нет данных` disabled и не имеет click listener.
11. Единственный shared source находится в `themes/common`.
12. Семь theme-local файлов являются symbolic links на canonical partial.

## Functional smoke

Проверить:

1. создание нового взаимодействия;
2. редактирование существующего;
3. копирование;
4. выбор кандидата;
5. выбор вакансии;
6. фильтр подписок;
7. 0 найденных популярных типов → пять disabled;
8. 1–4 типа → активные реальные + disabled placeholders;
9. 5+ типов → пять активных;
10. активная кнопка устанавливает точный тип;
11. disabled-кнопка ничего не меняет;
12. ручной выбор типа;
13. `buttonCallAction`;
14. `addString`, `addDate`, `addInteger`;
15. required-state;
16. рейтинг;
17. рекрутёр;
18. способ коммуникации;
19. комментарий;
20. подписка;
21. save;
22. cancel;
23. повторное открытие;
24. сохранение без раскрытия всех аккордеонов;
25. кандидат без фотографии;
26. проект без логотипа;
27. отсутствующий файл в FileStorage;
28. отсутствие `Cannot get unfetched attribute`;
29. отсутствие `ClassCastException`;
30. отсутствие горизонтальной прокрутки.

## Visual smoke

Темы:

```text
Halo
Havana
Helium
Hover
hunttech-modern
hunttech-modern-light
hunttech-modern-dark
```

Viewport:

```text
desktop
<=1366px
```

Подтвердить:

- sidebar 270/250 px;
- фотография кандидата 112 px и является главным образом;
- project logo 80 px, `object-fit/scale-down`;
- active navigation item ровно один;
- quick actions всегда видимы;
- аккордеоны и required markers не перекрываются;
- footer доступен;
- horizontal scroll отсутствует.

## Runtime

Выполнить:

- clean local deploy;
- `http://localhost:8080/hrm/` = HTTP 200;
- анализ Tomcat logs;
- P1=0;
- P2=0.

Успешный отчёт:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/iteraction-list-edit-from-scratch
PR: <номер>
Base: master
проверен HEAD: <полный SHA>
HEAD match: PASS
Conflicts: NONE
Compile: PASS
IteractionList tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS: PASS
Build: PASS
Deploy: PASS
HTTP /hrm/: 200
Functional smoke: PASS
Visual smoke: PASS
Tomcat critical errors: NONE
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

При ошибке: `STATUS: FAILED_VERIFICATION`, точный failed step, root cause, log/stack trace и список невыполненных проверок. Код не менять.
