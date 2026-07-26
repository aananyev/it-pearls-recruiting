# Hermes — проверка единого OvaFallbackImage в CandidateCVEdit

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES
REPOSITORY: aananyev/it-pearls-recruiting
BRANCH: agent/candidate-cv-ova-fallback
BASE: master
MODE: проверка без изменения функционального кода и `docs/`

## Проверка источника истины

1. Получить ветку из GitHub и выполнить checkout `agent/candidate-cv-ova-fallback`.
2. Получить ожидаемый полный HEAD SHA из описания draft PR.
3. Подтвердить:
   - ветка существует;
   - локальный `git rev-parse HEAD` совпадает с ожидаемым SHA;
   - PR открыт из `agent/candidate-cv-ova-fallback` напрямую в `master`;
   - HEAD PR совпадает с проверяемым SHA;
   - конфликтов с `master` нет.
4. При несовпадении остановить проверку со статусом `HEAD_MISMATCH`.

## Разрешённая область

Только проверка изменений:

- `CandidateCVEdit.candidatePic` объявлен и описан как единый `OvaFallbackImage`;
- `candidateFaceDefaultImage` отсутствует;
- ручное переключение visibility фотографии отсутствует;
- `CandidateCVEditVisualContractTest` фиксирует новый контракт;
- `docs/ui/CandidateCVEdit_Spec.md` синхронизирован.

Hermes не изменяет Java, XML, SCSS, тесты или документацию, не создаёт commit, не выполняет push, rebase, merge и не трогает production.

## Команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.CandidateCVEditVisualContractTest' \
          --tests 'com.company.hunttech.core.CandidateCVEditPhotoViewContractTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

## Data View Integrity

Подтвердить, что `CandidateCV.fileImageFace` остаётся прямым property runtime-view `candidateCVDc`, а `candidatePic` и `fileImageFaceUpload` используют `candidateCVDc / fileImageFace`. Getter незагруженного атрибута detached-сущности не вызывается.

## Local deploy и HTTP

Развернуть точный проверяемый HEAD локально. Проверить:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200.

## Functional smoke CandidateCVEdit

Проверить:

1. открытие существующего `CandidateCV` с фотографией;
2. открытие существующего `CandidateCV` без фотографии;
3. отображение `icons/no-programmer.jpeg` через тот же `candidatePic`;
4. загрузку новой фотографии через `fileImageFaceUpload`;
5. очистку фотографии и возврат fallback без перезагрузки экрана;
6. выбор изображения, извлечённого из PDF;
7. повторное открытие сохранённого `CandidateCV`;
8. сохранение и закрытие формы;
9. отсутствие второго DOM-компонента `candidateFaceDefaultImage`;
10. отсутствие визуального скачка layout между фотографией и fallback.

## Логи и критерий результата

Проверить Tomcat logs. Не допускаются новые:

- `ClassCastException`;
- `instantiatingValueholderWithNullSession`;
- `Cannot get unfetched attribute`;
- detached errors;
- `IllegalStateException`;
- `NullPointerException` в сценариях фотографии;
- Vaadin RPC errors.

PASS требует:

- `CandidateCVEditVisualContractTest`: 6/6 PASS;
- `CandidateCVEditPhotoViewContractTest`: 2/2 PASS;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- Data View Integrity: PASS;
- SCSS: PASS;
- `BUILD SUCCESSFUL`;
- local deploy: PASS;
- HTTP 200;
- smoke: PASS;
- Tomcat errors: NONE;
- P1 = 0, P2 = 0;
- документация и история синхронизированы.

Отчёт сохранить в:

```text
.ai/reports/2026-07-26-candidate-cv-ova-fallback-image.md
```

Успешный итог:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/candidate-cv-ova-fallback
PR: <номер PR>
Base: master
Verified HEAD: <полный SHA>
HEAD match: PASS
Conflicts: NONE
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```
