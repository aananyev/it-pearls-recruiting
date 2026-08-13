# Hermes — проверка тематических иллюстраций AI Edit-форм

PROJECT: HRM HuntTech

## Контекст

Проверить только presentation-изменение sidebar пяти AI Edit-форм: общий `icons/hunttech-logo.png` заменён на отдельные тематические HuntTech-иллюстрации. Исходные PNG имеют размер 200×200; размер `OvaFallbackImage` в формах должен остаться 176×176, как в принятой полировке по эталону JobCandidateEdit.

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/ai-edit-form-illustrations`
- PR: взять из актуального PR этой ветки
- Base: `master`
- Verified HEAD: взять из актуального тела PR перед началом проверки
- Режим: только проверка, без изменения функционального кода и `docs/`, без commit/push/rebase/merge, без production.

Перед выполнением команд Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки равен `Verified HEAD` из PR;
3. PR открыт из `agent/ai-edit-form-illustrations` прямо в `master`;
4. HEAD PR равен проверяемому SHA;
5. конфликтов с `master` нет.

Любое несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Scope проверки

Пять Edit-форм:

- `AiFunctionConfigurationEdit` → `icons/ai/ai-function-configuration.png`;
- `AdminAiConfigurationEdit` → `icons/ai/admin-ai-configuration.png`;
- `UserAiConfigurationEdit` → `icons/ai/user-ai-configuration.png`;
- `UserAiFunctionOverrideEdit` → `icons/ai/user-ai-function-override.png`;
- `VacancyPromptTemplateEdit` → `icons/ai/vacancy-prompt-template.png`.

Для каждого изображения проверить:

- source asset ровно 200×200 px;
- файл существует во всех семи темах: `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-dark`, `hunttech-modern-light`;
- `fallbackThemePath` конкретной формы указывает на её собственный asset;
- `width`, `height`, `ovalWidth`, `ovalHeight` в `OvaFallbackImage` остались 176px;
- изображения не растянуты и не обрезаны при `scaleMode="SCALE_DOWN"`;
- визуально различимы и соответствуют смыслу формы;
- палитра: чёрный / серый / белый / красный, с явной отсылкой к геометрическому HuntTech-символу.

Не должно быть изменений entity, service, loader, JPQL, DataContext, action, property binding, component ID или business logic.

## Команды

```bash
git fetch origin
git checkout agent/ai-edit-form-illustrations
git rev-parse HEAD
git diff --check origin/master...HEAD
git diff --name-only origin/master...HEAD

./gradlew :app-core:test \
  --tests '*AiEditFormIllustrationContractTest*' \
  --tests '*AiControlPlaneScreenContractTest*' \
  --tests '*VacancyPromptTemplateEditContractTest*' \
  --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидание для `ScreenViewIntegrityTest`: `8/8 PASS`.

## Local deploy и smoke

После успешной сборки:

1. выполнить штатный local deploy HRM HuntTech;
2. проверить `http://localhost:8080/hrm/` → HTTP 200;
3. проверить Tomcat logs: критические ошибки отсутствуют;
4. последовательно открыть все пять AI Edit-форм;
5. проверить отображение своей тематической иллюстрации в sidebar для каждой формы;
6. отдельно проверить активную light и dark HuntTech-тему, отсутствие missing theme resource/404;
7. убедиться, что сохранение/отмена формы работают как до визуальной правки и данные не изменяются из-за изображения.

## Отчёт

При успехе:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/ai-edit-form-illustrations
PR: <number>
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
AiEditFormIllustrationContractTest: PASS
AiControlPlaneScreenContractTest: PASS
VacancyPromptTemplateEditContractTest: PASS
ScreenViewIntegrityTest: 8/8 PASS
Data View Integrity: PASS / unchanged
SCSS: PASS
clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Tomcat errors: NONE
AI Edit illustration smoke: PASS
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
проверен HEAD: <full SHA>
```

При ошибке: `STATUS: FAILED_VERIFICATION`, указать `FAILED STEP`, `ROOT CAUSE`, необходимый log/stack trace, выполненные/неисполненные проверки и рекомендацию. Функциональный код не менять, commit/push/merge не выполнять, production не трогать.
