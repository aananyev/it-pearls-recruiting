# Hermes — проверка OpenPositionEditPreview

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repo: `aananyev/it-pearls-recruiting`
- Branch: `agent/open-position-edit-preview`
- Base: `master`
- PR: `#110`
- Verified HEAD: указан отдельным комментарием в PR после создания последнего коммита
- Режим: проверка без изменения функционального кода и документации

Перед запуском Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `Verified HEAD` из PR;
3. PR открыт из `agent/open-position-edit-preview` прямо в `master`;
4. HEAD PR совпадает с `Verified HEAD`;
5. конфликтов с `master` нет.

Любое несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Разрешённый diff

Ожидаются только:

- новый `OpenPositionEditPreview.java`;
- новый `open-position-edit-preview.xml`;
- новые `OpenPositionEditPreviewLayoutTest.java` и `OpenPositionEditPreviewRouteGuardTest.java`;
- `docs/ui/OpenPositionEditPreview_Spec.md`;
- индекс `docs/ui/README.md`;
- диагностические task-файлы Hermes;
- эта инструкция.

Legacy `OpenPositionEdit.java`, `open-position-edit.xml`, browse-экраны, `web-menu.xml`, entities, services, `views.xml`, JPQL в существующих файлах, Liquibase и theme/SCSS не должны изменяться.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests '*OpenPositionEditPreviewLayoutTest*' \
          --tests '*OpenPositionEditPreviewRouteGuardTest*' \
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

- `OpenPositionEditPreviewLayoutTest` — 8/8 PASS;
- `OpenPositionEditPreviewRouteGuardTest` — PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- SCSS — PASS по всем темам;
- `BUILD SUCCESSFUL`.

## Local deploy

Развернуть точный `Verified HEAD` локально. Production не изменять.

Проверить:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200.

## Доступ к preview

Экран не включён в меню и browse. Открывать под администратором либо разработчиком с разрешением на экран:

```text
hunttech_OpenPosition.editPreview
```

Маршрут:

```text
http://localhost:8080/hrm/#main/open-position-edit-preview?id=<encoded-open-position-id>
```

Для получения корректно закодированного UUID допустимо использовать штатный CUBA `RouteGenerator.getEditorRoute(entity, OpenPositionEditPreview.class)` в отладочной консоли/временном runtime-сценарии без коммита изменений.

## Обязательная регрессия runtime-ошибки

Использовать вакансию, на которой Hermes воспроизвёл ошибку:

```text
UUID: fffa4236-4d35-8b34-3bf3-baa240aa3722
Route id: 7zz913ck9nhct3qwxtm90amds2
URL: http://localhost:8080/hrm/#main/open-position-edit-preview?id=7zz913ck9nhct3qwxtm90amds2
```

Подтвердить:

1. preview открывается без EclipseLink `ValidationException`;
2. `positionType` и его описания отображаются;
3. в Tomcat logs отсутствуют `instantiatingValueholderWithNullSession` и обращения к lazy `positionType` с null Session;
4. legacy `open-position-edit?id=...` продолжает работать без изменения поведения.

## Smoke-сценарии

Использовать существующую заполненную вакансию с проектом, компанией, владельцем, зарплатой и связанными данными.

1. Открыть preview по прямому route.
2. Убедиться, что legacy `hunttech_OpenPosition.edit` по-прежнему открывается из browse без изменения вида и поведения.
3. Проверить sidebar: логотип, название, ключевые параметры, владелец, подписка.
4. Последовательно открыть все доступные вкладки через горизонтальный TabSheet.
5. Повторить переходы через label-навигацию; active-state должен совпадать с выбранной вкладкой.
6. Для одиночной вакансии пункт и вкладка «Оплаты» скрыты.
7. Для карточки команды пункт и вкладка «Оплаты» доступны.
8. Раскрыть и свернуть каждый accordion. Значения полей не должны сбрасываться.
9. Проверить таблицы договоров, файлов, навыков и новостей, включая штатные create/edit/remove actions.
10. Проверить RichTextArea описания, тестового задания, памятки и шаблона письма.
11. Проверить BPM fragment согласования и комментарии.
12. Выполнить сохранение существующей вакансии без изменения значений.
13. Изменить одно безопасное тестовое значение, сохранить, повторно открыть preview и подтвердить сохранение; затем вернуть значение.
14. Открыть preview повторно и проверить отсутствие повторных/дублирующих строк коллекций.
15. Открыть preview и сохранить, не заходя в тяжёлые вкладки.
16. Нажать «Отмена» после изменения поля и подтвердить отсутствие сохранения.

## Runtime-контроль

В Tomcat logs не допускаются новые:

- `Cannot get unfetched attribute`;
- `instantiatingValueholderWithNullSession`;
- detached entity errors;
- `IllegalStateException`;
- `NullPointerException` в preview-сценариях;
- Vaadin RPC / `ClassCastException`;
- ошибки загрузки XML;
- ошибки DI компонентов;
- ошибки DataContext merge/commit;
- P1/P2 дефекты.

## Отчёт

Создать `.ai/reports/2026-08-01-open-position-edit-preview-verification.md`.

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/open-position-edit-preview
PR: #110
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
Compile: PASS
Preview layout test: 8/8 PASS
Preview route guard test: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
URL positionType regression: PASS
Smoke: PASS
Legacy screen unchanged: PASS
Tomcat errors: NONE
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

Ошибка → `STATUS: FAILED_VERIFICATION` с FAILED STEP, ROOT CAUSE, релевантным stack trace и перечнем невыполненных проверок. Код, commit, merge и production Hermes не меняет.
