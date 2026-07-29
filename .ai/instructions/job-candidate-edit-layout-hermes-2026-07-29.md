# Hermes verification — JobCandidateEdit layout contract

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Scope

Проверить PR #105 без изменения функционального кода, документации, ветки, production,
БД production или конфигурации production.

## Repository

- Repo: `aananyev/it-pearls-recruiting`
- Branch: `agent/job-candidate-edit-layout`
- Base: `master`
- PR: `https://github.com/aananyev/it-pearls-recruiting/pull/105`
- Проверяемый HEAD: актуальный `head_sha` PR #105 после push этой инструкции.
  Точный SHA фиксируется в описании PR и должен совпадать с HEAD ветки перед началом проверки.

## Обязательные проверки

1. Подтвердить, что ветка существует и HEAD ветки/PR совпадает с проверяемым SHA.
2. Подтвердить, что PR открыт из `agent/job-candidate-edit-layout` напрямую в `master`.
3. Проверить отсутствие конфликтов с `master`.
4. Выполнить:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
git diff --check
xmllint --noout modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.JobCandidateEditLayoutContractTest' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

5. Выполнить локальный deploy, проверить HTTP 200 для `http://localhost:8080/hrm/`.
6. Открыть локальный стенд под пользователем `alan`.
7. Проверить визуально `JobCandidateEdit`:
   - sidebar-навигация расположена сразу под фото, ФИО и должностью;
   - карточки рейтинга/контактов идут ниже navigation;
   - строка вкладок не выходит за границы стандартного dialog `1200x750`;
   - нижняя панель действий занимает ширину workspace и кнопки выровнены справа;
   - вкладки «Основное», «Контакты», «Позиции и вакансии», «Взаимодействия»,
     «Резюме и файлы», «Комментарии», «История» не имеют наложений и неконтролируемой
     горизонтальной прокрутки формы;
   - таблицы допускают локальную прокрутку внутри табличной области, если колонок больше
     доступной ширины;
   - стили и шрифты согласованы в активной теме `hover`.
8. Проверить Tomcat logs на ошибки, связанные с `JobCandidateEdit`, SCSS, widgetset и загрузкой экрана.

## Ожидаемый успешный отчёт

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/job-candidate-edit-layout
PR: #105
Base: master
проверен HEAD: <SHA>
HEAD match: PASS
conflicts: NONE
checks: PASS
SCSS: PASS
clean assemble: PASS
local deploy: PASS
HTTP /hrm/: 200
visual smoke alan: PASS
Tomcat errors: NONE
docs/history synchronized: PASS
P1: 0
P2: 0
merge: not performed
production: not changed
```

Если проверка падает, вернуть `STATUS: FAILED_VERIFICATION`, указать failed step, root cause,
выполненные/невыполненные проверки и подтвердить, что код, commit, merge и production не менялись.
