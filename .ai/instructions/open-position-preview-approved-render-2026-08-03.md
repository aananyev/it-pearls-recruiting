# Hermes verification — утверждённый рендер OpenPositionEditPreview

Проект: HRM HuntTech
Ветка: `agent/open-position-preview-render`
Base: `master`
Статус PR: `WAITING_FOR_HERMES`

Проверять только точный HEAD, указанный в PR. Перед сборкой подтвердить совпадение
HEAD ветки, HEAD PR и проверяемого SHA, `base=master`, а также отсутствие конфликтов.
При несовпадении остановить проверку со статусом `HEAD_MISMATCH`.

## Область проверки

- `OpenPositionEditPreview` реализует утверждённую двухпанельную компоновку в стиле
  `JobCandidateEdit` и общего контракта Edit-экранов.
- Sidebar имеет ширину 264px; основной визуальный образ 88px.
- HBox-поля и footer выровнены через фактический Vaadin `v-expand`; inline-смещения slot сброшены локально, правые controls не обрезаются и не выходят за workspace.
- Блок «Проект, Компания, Тип должности» проверяется отдельными локальными ролями: должность и удалённая работа находятся в первой строке, комментарий — во второй, проект и компания имеют одинаковую полную ширину без отрицательных slot-смещений.
- Вкладка «Трудовой договор» использует локальный table variant 5: theme-aware header, стабильные строки, editor cells и компактную buttonsPanel; вкладка «Описание вакансии» использует RichTextArea variant 5 с полноразмерными toolbar/content-областями.
- Первая секция формирует пары `ID + Вакансия` / `Грейд + Генерировать`; GroupBox captions на остальных вкладках полностью видны ниже tabs.
- Отдельные пункт навигации и tab оплаты не отображаются.
- Все прежние платёжные поля находятся в `laborAgreementTab` после таблицы договоров:
  «Оплата компании», «Оплата ресерчерам», «Оплата рекрутерам».
- Component ID, bindings, actions, invoke, loaders, JPQL и legacy-бизнес-логика сохранены.
- Локальные SCSS идентичны во всех семи темах CUBA Platform.

Legacy `OpenPositionEdit`, серверы, services, entities, Liquibase, БД и production
не изменялись.

## Команды

```bash
git diff --check
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.OpenPositionEditPreviewLayoutTest' \
  --tests 'com.company.hunttech.core.OpenPositionEditPreviewSharedStyleContractTest' \
  --tests 'com.company.hunttech.core.OpenPositionEditPreviewSidebarUsabilityContractTest' \
  --no-daemon --stacktrace
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' \
  --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

## Visual smoke

Локально под пользователем `alan`:

1. открыть `http://localhost:8080/hrm/` и подтвердить HTTP 200;
2. открыть `OpenPositionEditPreview` для существующей вакансии и новой карточки;
3. проверить sidebar 264px на всех desktop viewport, отсутствие
   пересечения logo/title, внутренний vertical scroll и полную видимость подписей навигации;
4. проверить отсутствие отдельной вкладки оплаты для одиночной и командной вакансии;
5. открыть «Трудовые договоры» и проверить параметры оформления, таблицу договоров,
   три платёжные accordion-секции и все прежние поля;
6. проверить одинаковую ширину и высоту controls, таблиц и RichTextArea, отсутствие
   наложений и горизонтального выхода на 1920×1080, 1600×900, 1366×768 и 1280×800;
7. проверить, что GroupBox captions полностью видны и не заходят под полосу tabs;
8. проверить footer: «Отмена» и «Сохранить и закрыть» находятся внизу справа;
9. на вкладке «Проект» проверить строки «Должность / Удалённая работа / Комментарий», затем одинаковую ширину полей «Проект» и «Компания» и отсутствие горизонтального выхода;
10. повторить smoke в семи темах и проверить отсутствие новых critical Tomcat errors.

Ожидаемый результат: `STATUS: READY_TO_MERGE`. Merge не выполнять, production не трогать.
