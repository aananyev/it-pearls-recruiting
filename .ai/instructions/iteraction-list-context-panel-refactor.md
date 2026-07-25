# Проверка контекстной панели IteractionListEdit

PROJECT: HRM HuntTech

Репозиторий: `aananyev/it-pearls-recruiting`  
Ветка: `agent/iteraction-list-context-panel-refactor`  
Base: `master`  
Статус: `WAITING_FOR_HERMES`  
Режим: проверка точного HEAD PR без изменения функционального кода, документации и production.

Перед началом Hermes обязан подтвердить:

- ветка существует;
- PR открыт из указанной ветки напрямую в `master`;
- HEAD ветки и HEAD PR совпадают с SHA из PR;
- conflicts = NONE;
- формулировка отчёта содержит `проверен HEAD: <SHA>`.

## Команды

```bash
git diff --check
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
  --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- `IteractionListSidebarContextPanelTest` — 3/3 PASS;
- `LeftSidebarAvatarComponentTest` — 2/2 PASS;
- `IteractionListAccordionNavigationTest` — 4/4 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS;
- SCSS всех семи тем — PASS;
- `BUILD SUCCESSFUL`;
- local deploy — PASS;
- `http://localhost:8080/hrm/` — HTTP 200;
- Tomcat critical errors — NONE;
- P1 = 0, P2 = 0.

## Visual smoke

Проверить Halo как эталон и остальные шесть тем:

1. sidebar имеет ширину 272 px, при узком viewport — 252 px;
2. captions «Номер» и «Дата» компактны и не выглядят как заголовок карточки;
3. подразделение, проект, статус и приоритет переносятся по словам и не накладываются друг на друга;
4. HTML в значении подразделения остаётся светлым и читаемым;
5. warning вакансии и индикатор приоритета не вытесняют текст;
6. предел найма отображается отдельным caption, сумма и единица — строкой ниже;
7. изображения кандидата и проекта сохраняют размеры 104 × 104 и 76 × 76;
8. навигация под карточкой доступна через вертикальную прокрутку sidebar;
9. горизонтальная прокрутка sidebar и основной формы отсутствует;
10. bindings, actions, loaders, JPQL, save/cancel и lifecycle работают без изменений.

Hermes не меняет Java, XML, SCSS, tests или docs, не делает commit, push, rebase или merge и не трогает production. Отчёт сохранить в `.ai/reports/`.
