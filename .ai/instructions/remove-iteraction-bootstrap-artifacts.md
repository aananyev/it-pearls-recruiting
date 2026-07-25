# Hermes — удаление временных bootstrap-файлов

PROJECT: HRM HuntTech
REPO: aananyev/it-pearls-recruiting
BRANCH: ops/remove-iteraction-bootstrap-artifacts
BASE: master
MODE: проверка без изменения кода

Проверить точный HEAD ветки и PR, base=master, conflicts=NONE и отсутствие следующих временных файлов:

- `.github/workflows/apply-iteraction-usability-pr.yml`;
- `.github/workflows/iteraction-list-usability-navigation.yml`;
- `.ai/tasks/.iteraction-list-usability-navigation-trigger`.

Выполнить `git diff --check`. Сборка, deploy, HTTP smoke и production для этого repository-cleanup не требуются: функциональный код, XML, SCSS, docs и данные приложения не изменяются.

Hermes не меняет код, не делает commit, push, rebase или merge. Отчёт должен содержать `проверен HEAD: <SHA>` и статус `READY_TO_MERGE` либо `FAILED_VERIFICATION`.
