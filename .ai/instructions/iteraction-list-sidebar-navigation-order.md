# Проверка порядка элементов sidebar IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-sidebar-navigation-order`  
BASE: `master`  
MODE: проверка точного HEAD PR без изменения функционального кода, документации и production.

Точный полный HEAD SHA указан в PR. Перед проверкой Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `HEAD SHA для проверки` из PR;
3. PR открыт из этой ветки напрямую в `master`;
4. HEAD PR совпадает с проверяемым SHA;
5. `base=master`;
6. conflicts = NONE.

Несовпадение — `HEAD_MISMATCH`, проверку остановить. Отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Изменение

- из sidebar удалены две подписи `msg://msgHeaderIteraction` и подпись `msg://msgIteractionList`, которые отображались как «Заголовок / Список взаимодействий / Заголовок»;
- `iteractionListNavigation` расположен непосредственно после блока изображений `iteraction-list-identity-images`;
- карточка номера/даты и карточка вакансии располагаются ниже навигации;
- Java-контроллер, entity, views, loaders, JPQL, bindings, actions, `invoke`, validation и lifecycle не изменены.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
          --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- профильные тесты — PASS;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- SCSS семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Local deploy и smoke

1. Развернуть точный HEAD локально и перезапустить Tomcat.
2. Проверить `http://localhost:8080/hrm/` — HTTP `200`.
3. Открыть создание и редактирование `IteractionListEdit`.
4. Подтвердить, что в sidebar отсутствуют строки «Заголовок», «Список взаимодействий», «Заголовок».
5. Подтвердить, что индекс пяти блоков начинается сразу под изображениями кандидата и проекта.
6. Подтвердить порядок: изображения → индекс → номер/дата → карточка вакансии.
7. Проверить все пять пунктов индекса, active-state, раскрытие секции и перевод фокуса.
8. Проверить candidate/vacancy picker, тип и действие, результат, комментарий, пять быстрых кнопок, подписку, save и cancel.
9. Повторить visual smoke во всех семи темах.
10. Проверить отсутствие horizontal scroll, перекрытий, XML loader/binding errors, critical Tomcat errors; P1=0, P2=0.

Hermes не меняет Java, XML, SCSS, tests или docs; не делает commit, push, rebase или merge и не изменяет production. Отчёт сохранить в `.ai/reports/`.
