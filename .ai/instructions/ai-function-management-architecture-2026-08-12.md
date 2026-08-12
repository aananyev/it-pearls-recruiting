# Проверка архитектуры управления AI-функциями HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/ai-function-management-architecture`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка документации без изменения функционального кода

## Проверяемый HEAD

Точный полный `Verified HEAD to check` указывается в описании PR после публикации последнего коммита этой ветки.

До выполнения проверок Hermes обязан подтвердить:

1. ветка существует;
2. локальный HEAD ветки равен `Verified HEAD to check` из PR;
3. PR открыт из `agent/ai-function-management-architecture` напрямую в `master`;
4. HEAD PR равен проверяемому SHA;
5. conflicts = `NONE`;
6. рабочее дерево чистое.

Несовпадение → `HEAD_MISMATCH`, дальнейшую проверку остановить.

## Разрешённый diff

Ожидаются только:

- `docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md`;
- `docs/architecture/README.md`;
- `.ai/instructions/ai-function-management-architecture-2026-08-12.md`.

Java, XML, SCSS, entity, views, JPQL, loaders, actions, services, tests, Liquibase и production-конфигурация не изменяются.

## Проверка содержания документации

Подтвердить, что архитектурный документ содержит:

1. Business & Context Intro: What & Why, UI Context & Navigation, Behavior Summary;
2. ссылки на `HRM_HuntTech_UI_UX_Design_Concept.md`, `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`, `XML_Screen_Documentation_Standard.md` и существующий AI integration passport;
3. явную фиксацию, что отдельный канонический Browse-контракт отсутствует в базовом master и документ не выдает локальные правила за глобальный контракт;
4. требования к будущим AI Browse/Edit-формам;
5. сущности `AiFunctionConfiguration`, `AdminAiConfiguration`, существующую `UserAiConfiguration`, `UserAiFunctionOverride` и отдельную ответственность `UserAiProfile`;
6. политики `ADMIN_ONLY`, `USER_OVERRIDE_ALLOWED`, `USER_REQUIRED`;
7. `AiConfigurationResolver`, `AiExecutionService`, сохранение `AIProviderRegistry`;
8. защиту корпоративных credentials и отсутствие секретов в browse-view/logs;
9. оценку текущей незапушенной реализации AI в `ProjectEdit` отдельно от фактического GitHub master;
10. запрет возрастного AI-скоринга кандидатов и ограничение рекомендаций профессиональными данными;
11. границы первого этапа: другие бизнес-сущности и формы не изменяются;
12. поэтапную миграцию и тестовый контракт;
13. историю изменений с новой строкой первой;
14. ссылку на новый документ из `docs/architecture/README.md` и новую строку истории README.

## Статические проверки

```bash
git diff --check origin/master...HEAD

git diff --name-only origin/master...HEAD
```

Ожидается ровно три файла из разрешённого diff.

Проверить ссылки и ключевые разделы:

```bash
grep -n "Business & Context Intro" docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md
grep -n "AiFunctionConfiguration" docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md
grep -n "UserAiFunctionOverride" docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md
grep -n "ProjectEdit" docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md
grep -n "Browse-контракт" docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md
grep -n "HRM_HuntTech_AI_Function_Management_Architecture.md" docs/architecture/README.md
```

## Сборка и runtime

Документационный diff не меняет runtime, однако для полного проектного gate выполнить:

```bash
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается `BUILD SUCCESSFUL`.

После clean local deploy проверить:

```text
http://localhost:8080/hrm/ = HTTP 200
```

Проверить Tomcat logs: critical errors = `NONE`; P1=0; P2=0.

Для данного doc-only PR:

- `ScreenViewIntegrityTest`: `N/A`;
- Data View Integrity: `N/A`;
- `buildScssThemes`: `N/A`;
- browser visual smoke: `N/A`.

## Запреты

Hermes не изменяет Java/XML/SCSS/entity/views/JPQL/loaders/actions/services/Liquibase и документацию, не делает commit, push, rebase, merge, не разрешает конфликты и не выполняет production-действия.

## Успешный отчёт

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/ai-function-management-architecture
PR: <номер>
Base: master
Verified HEAD: <полный SHA>
HEAD match: PASS
Conflicts: NONE
Allowed diff: PASS, 3 files
Architecture documentation: PASS
Architecture README: PASS
Git diff check: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Tomcat critical errors: NONE
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
проверен HEAD: <полный SHA>
```

При ошибке: `STATUS: FAILED_VERIFICATION` с `FAILED STEP`, `ROOT CAUSE`, релевантным выводом и перечнем невыполненных проверок.
