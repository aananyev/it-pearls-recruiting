# Hermes verification — Edit left panel polish

Проект: HRM HuntTech
Ветка: `agent/edit-left-panel-polish`
Base: `master`
Статус PR: `WAITING_FOR_HERMES`

Проверять только точный HEAD, указанный в PR. При несовпадении HEAD ветки, HEAD PR или base
остановить проверку со статусом `HEAD_MISMATCH`.

## Область проверки

- `JobCandidateEdit`: левая панель, label-навигация, tab captions, кнопки загрузки фото и footer-actions.
- `CandidateCVEdit`: левая панель 312px, label-навигация, tab captions, единая ширина input/picker/upload controls.
- `OpenPositionEditPreview`: левая панель preview-компоновки OpenPosition, tab captions и sidebar usability layer.

Legacy `OpenPositionEdit` не заменялся preview-экраном. Production, серверная логика, entities,
Liquibase, services, JPQL, loaders и permissions не менялись.

## Команды

```bash
git diff --check
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' \
  --tests 'com.company.hunttech.core.JobCandidateEditLayoutContractTest' \
  --tests 'com.company.hunttech.core.CandidateCVEditVisualContractTest' \
  --tests 'com.company.hunttech.core.OpenPositionEditPreviewSidebarUsabilityContractTest' \
  --tests 'com.company.hunttech.core.OpenPositionEditPreviewSharedStyleContractTest' \
  --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

## Visual smoke

Локально под пользователем `alan`:

1. открыть `http://localhost:8080/hrm/`;
2. проверить HTTP 200;
3. открыть `JobCandidateEdit`, `CandidateCVEdit`, `OpenPositionEditPreview`;
4. проверить, что sidebar не перекрывает workspace, пункты label-навигации выровнены,
   tab captions видны полностью, `Загрузить` и `Очистить` оформлены одинаково,
   `Сохранить и закрыть` и `Отмена` сгруппированы внизу справа;
5. проверить отсутствие новых critical Tomcat errors. Ошибки отсутствующих файлов в локальном
   fileStorage считать окружением, если они совпадают с существующими missing-file записями.

Ожидаемый результат: `READY_TO_MERGE`, merge не выполнять, production не трогать.
