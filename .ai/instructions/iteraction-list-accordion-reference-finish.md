# Проверка финальной компоновки IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-accordion-reference-finish`  
BASE: `master`  
MODE: проверка точного HEAD PR без изменения функционального кода, документации и production.

Точный HEAD SHA указан в PR. До начала Hermes обязан подтвердить branch HEAD, PR HEAD, `base=master` и conflicts=NONE. Несовпадение — `HEAD_MISMATCH`, проверку остановить.

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

Ожидается: профильные тесты PASS; `ScreenViewIntegrityTest 8/8`; Data View Integrity PASS; SCSS семи тем PASS; `BUILD SUCCESSFUL`.

## Local deploy и smoke

1. Развернуть точный HEAD локально, перезапустить Tomcat.
2. Проверить `http://localhost:8080/hrm/` — HTTP 200.
3. Открыть новый и существующий `IteractionListEdit` во всех семи темах.
4. Подтвердить порядок sidebar: изображения → номер/дата → индекс → карточка вакансии.
5. Подтвердить, что `projectLogoImage` овальный, использует OvaFallbackImage и fallback без логотипа.
6. Подтвердить, что над аккордеонами расположен блок ровно той же ширины с пятью одинаковыми зелёными кнопками с полукруглыми краями; каждая занимает 20%.
7. Подтвердить видимый caption «Кандидат и вакансия».
8. Нажать «Кандидат и вакансия» в левой панели: раскрывается `participantsAccordion`, остальные секции сворачиваются, фокус переходит в `candidateField`.
9. Проверить остальные четыре пункта навигации, active-state и штатные заголовки GroupBox.
10. Проверить candidate/vacancy picker, тип и действие, результат, комментарий, быстрые кнопки, подписку, save/cancel.
11. Проверить отсутствие horizontal scroll, перекрытий, XML loader/binding errors и critical Tomcat errors; P1=0, P2=0.

Отчёт сохранить в `.ai/reports/` и включить формулировку `проверен HEAD: <полный SHA>`. Hermes не меняет код/docs, не делает commit, push, rebase, merge и не изменяет production.
