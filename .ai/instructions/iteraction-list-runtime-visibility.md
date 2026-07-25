# Проверка runtime-видимости селекторов IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-runtime-visibility`  
BASE: `master`  
MODE: проверка без изменения кода

Hermes должен начать с проверки точного HEAD, указанного в PR, `base=master`, HEAD PR и отсутствия конфликтов. Любое несовпадение — `HEAD_MISMATCH`, проверку остановить.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
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

- compile — PASS;
- `IteractionListEditAccordionLayoutTest` — `5/5 PASS`;
- `IteractionListAccordionNavigationTest` — `5/5 PASS`;
- `IteractionListMostPopularInteractionTest` — `5/5 PASS`;
- `LeftSidebarAvatarComponentTest` — `2/2 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- SCSS всех семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Local deploy и обязательный browser smoke

Развернуть точный HEAD локально. Проверить HTTP `http://localhost:8080/hrm/` = `200`, затем войти в приложение и открыть создание и редактирование `IteractionListEdit`.

Проверить:

1. Поле «Кандидат» видно полностью, не имеет нулевой ширины и находится в одной строке с полем «Вакансия».
2. В `candidateField` вводятся минимум три символа, появляется suggestion; lookup и open работают.
3. В правой рабочей части виден отдельный селектор пяти блоков по модели `SettingsWindow`.
4. Селектор остаётся видимым при вертикальной прокрутке аккордеонов.
5. Каждый пункт раскрывает только свой блок, active-state и focus синхронизированы.
6. При выборе «Частые взаимодействия» видны ровно пять равных кнопок.
7. При наличии истории captions соответствуют top-5 текущего пользователя за скользящий год; клик устанавливает точный `Iteraction`.
8. При недостатке данных свободные позиции видимы как disabled «Нет данных».
9. Кандидат, вакансия, тип, dynamic fields, rating, recruiter, communication, comment, subscription, save/cancel работают без регрессии.
10. Выполнить visual smoke в `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`.
11. Горизонтальная прокрутка, перекрытия и схлопывание picker-полей отсутствуют.
12. Tomcat logs: новых XML loader, binding, `NullPointerException`, `ClassCastException`, unfetched/detached и critical errors — NONE.
13. P1 = 0; P2 = 0.

Hermes не меняет Java, XML, SCSS, tests и docs; не делает commit, push, rebase, merge и не изменяет production. Отчёт сохранить в `.ai/reports/` с формулировкой `проверен HEAD: <SHA>`.
