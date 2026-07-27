# Проверка общего контракта стилей Edit-экранов

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/edit-screen-shared-style-contract`  
BASE: `master`  
MODE: проверка документации без изменения кода

## Правило точного HEAD

Точный полный SHA указывается в описании PR в поле `Verified HEAD to check`.

До проверки Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с SHA из PR;
3. PR открыт из `agent/edit-screen-shared-style-contract` напрямую в `master`;
4. HEAD PR совпадает с тем же SHA;
5. conflicts = `NONE`;
6. рабочее дерево чистое.

Несовпадение означает `HEAD_MISMATCH`; проверку остановить. Итоговый отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Приоритет контракта

При проверке не изменять документ под фактические частичные стили, добавленные Hermes. Прямое указание Алексея и `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` имеют приоритет. Несоответствия текущей реализации фиксируются как будущая миграция, а не как основание ослабить контракт.

## Разрешённый diff

Только:

- `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`;
- `docs/architecture/README.md`;
- `.ai/instructions/edit-screen-shared-style-contract.md`.

Java, XML экранов, SCSS, entity, views, JPQL, loaders, actions, tests, БД и Liquibase не изменяются.

## Обязательная проверка документации

Подтвердить, что контракт содержит:

1. Business & Context Intro;
2. нормативную связь с `HRM_HuntTech_UI_UX_Design_Concept.md`;
3. правило приоритета прямых указаний Алексея и настоящего контракта;
4. единственное имя контейнера label-навигации — `label-navigation`;
5. точный набор:
   - `label-navigation`;
   - `label-nav-title`;
   - `label-nav-item`;
   - `label-nav-item-active`;
6. правило одновременного использования `label-nav-item` и `label-nav-item-active`;
7. разделение локального component ID и общего `stylename`;
8. общий набор `edit-sidebar*`;
9. общий набор `edit-workspace*`, `edit-toolbar*`, `edit-card*`, `edit-accordion-section`, `edit-footer-actions`;
10. XML-пример на основе `ExtSettingsWindow`;
11. таблицу целевого соответствия текущих stylename `ExtSettingsWindow` общим stylename;
12. запрет копирования SCSS `SettingsWindow` по экранам;
13. требование одного shared SCSS partial/mixin для семи тем на будущем этапе;
14. запрет неограниченных глобальных Vaadin-селекторов;
15. правила поэкранной миграции без массовой замены;
16. критерии visual smoke и сохранения CUBA-контрактов;
17. историю изменений с новой строкой первой.

## Статические проверки

```bash
git diff --check origin/master...HEAD

git diff --name-only origin/master...HEAD
```

Ожидается ровно три разрешённых файла.

Дополнительно:

```bash
grep -n "label-navigation" docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md
grep -n "label-nav-title" docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md
grep -n "label-nav-item-active" docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md
grep -n "ExtSettingsWindow" docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md
grep -n "приоритет" docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md
```

Markdown-заголовки, таблицы, XML/SCSS code blocks и ссылка из `docs/architecture/README.md` не повреждены.

## Сборка и runtime

```bash
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается `BUILD SUCCESSFUL`.

После clean local deploy:

```text
http://localhost:8080/hrm/ = HTTP 200
```

Поскольку Java, XML и SCSS не менялись:

- `ScreenViewIntegrityTest`: `N/A`;
- Data View Integrity: `N/A`;
- `buildScssThemes`: `N/A`;
- browser visual smoke новых стилей: `N/A`, стили ещё не реализуются.

Проверить Tomcat logs: critical errors = `NONE`; P1=0; P2=0.

## Запреты

Hermes не изменяет код или документацию, не создаёт commit, не выполняет push, rebase, merge, разрешение конфликтов и production-действия.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/edit-screen-shared-style-contract
PR: <номер>
Base: master
Verified HEAD: <полный SHA>
HEAD match: PASS
Conflicts: NONE
Allowed diff: PASS, 3 files
Documentation contract: PASS
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
