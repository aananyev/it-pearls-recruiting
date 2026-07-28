# Hermes — проверка визуального выравнивания IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-xml-visual-alignment`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка без изменения кода и документации

До запуска подтвердить branch HEAD = PR HEAD = переданный SHA, `base=master`, conflicts `NONE`. Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение

Проверить девять визуальных контрактов:

1. два `OvaFallbackImage` по `96 × 96`;
2. видимый title `Разделы формы`;
3. восстановленные `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`;
4. одинаковое оформление `candidateField` и `vacancyFiels`;
5. естественная `height=AUTO` блока результата;
6. status/priority горизонтально `50/50`;
7. service-card полностью внутри sidebar;
8. vacancy-card полностью внутри sidebar;
9. vacancy-card содержит `vacancy.vacansyName`.

## Разрешённый diff

- `iteraction-list-edit.xml` — только visual layout и дополнительный read-only label binding;
- `messages.properties`, `messages_ru.properties` — caption vacancy name;
- новый локальный SCSS partial семи тем;
- `styles.scss` семи тем — import/include нового partial;
- `IteractionListVisualAlignmentTest`;
- living-документация и эта инструкция.

Запрещены изменения entity, DB/Liquibase, services, JPQL, views, loaders, business handlers, production/infra.

## Статическая проверка

- XML parse PASS;
- все существующие field IDs/actions/bindings сохранены;
- два image имеют четыре размера `96px`;
- `iteractionListNavigationTitle` присутствует;
- четыре navigation item присутствуют;
- оба primary picker имеют один stylename;
- result section/body/grid имеют `height=AUTO`;
- status/priority — HBox + два `50%`;
- service/vacancy card IDs присутствуют;
- `sidebarVacancyNameLabel` связан с `vacancy.vacansyName`;
- семь SCSS partial идентичны;
- import/include расположен после reference-finish;
- CSS scoped under `.iteraction-list-editor`.

## Команды

```bash
git diff --check
git diff --name-only master...HEAD

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается compile PASS, профильные tests PASS, `ScreenViewIntegrityTest 8/8 PASS`, SCSS семи тем PASS, `BUILD SUCCESSFUL`.

## SCSS identity

```bash
sha256sum modules/web/themes/{halo,havana,helium,hover,hunttech-modern,hunttech-modern-light,hunttech-modern-dark}/com.company.hunttech/iteraction-list-visual-alignment.scss
```

Ожидается один hash.

## Browser smoke

1. Открыть новую и существующую запись.
2. Проверить два одинаковых круглых изображения.
3. Проверить title и четыре styled navigation item.
4. Проверить active item + focus/scroll.
5. Сравнить candidate/vacancy controls, включая action buttons.
6. Проверить result card с короткими и длинными значениями.
7. Проверить status/priority в одной строке.
8. Проверить service-card без horizontal overflow.
9. Проверить vacancy-card с длинными vacancy/company/project values.
10. Проверить vacancy name внутри карточки.
11. Проверить 1700×950, 1366×768, 1100×760 и семь тем.
12. Проверить save/cancel/subscribe и отсутствие регрессии business behavior.

После clean deploy проверить `/hrm/` = HTTP 200, Tomcat critical errors NONE, P1=0, P2=0.

Hermes не меняет код/docs, не делает commit, push, rebase, merge, не разрешает конфликты и не изменяет production.
