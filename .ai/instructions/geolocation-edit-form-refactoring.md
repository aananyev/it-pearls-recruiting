# Hermes — проверка рефакторинга географических Edit-форм

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/geolocation-edit-form-refactoring`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка без изменения кода и документации

До запуска подтвердить branch HEAD = PR HEAD = переданный SHA, `base=master`, conflicts `NONE`. Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Область проверки

Проверить визуальный рефакторинг `CountryEdit`, `RegionEdit`, `CityEdit` по общему контракту Edit-экранов. Запрещено менять Java/XML/SCSS/docs, делать commit, push, rebase, merge и изменять production.

## Инварианты

1. Не изменены entity, views, data containers, property/options bindings, loaders, JPQL, actions и save lifecycle.
2. Все три экрана используют `edit-screen-layout`, `edit-sidebar`, `edit-workspace`, toolbar, cards и footer-actions.
3. Navigation использует только `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`.
4. Country: основные поля и composition-таблица регионов работают.
5. Region: страна выбирается прежним picker action; composition-таблица городов работает.
6. City: регион выбирается прежним picker action.
7. Navigation меняет только focus и active-state.

## Команды

```bash
git diff --check
git diff --name-only master...HEAD

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: compile PASS, `ScreenViewIntegrityTest 8/8 PASS`, SCSS PASS, `BUILD SUCCESSFUL`.

## Local deploy и smoke

После clean local deploy:

- `http://localhost:8080/hrm/` = HTTP 200;
- открыть browse и Edit для стран, регионов и городов;
- проверить новый и существующий экземпляр каждого справочника;
- проверить focus label-навигации;
- Country: add/edit/remove региона;
- Region: выбор страны и add/edit/remove города;
- City: выбор региона;
- сохранить и отменить без потери данных;
- проверить темы Halo, Hover, Havana, Helium, hunttech-modern, hunttech-modern-light, hunttech-modern-dark;
- Tomcat critical errors = NONE;
- P1=0; P2=0.

Успех оформить как `STATUS: READY_TO_MERGE` с формулировкой `проверен HEAD: <SHA>`. Ошибка — `STATUS: FAILED_VERIFICATION` с root cause и логом.
