# BL-2026-032 — восстановление компоновки «Подобрать вакансию»

STATUS: WAITING_FOR_CI

BASE_SHA: `0c1885abe4e677a3895a3d7b833956873f1e6132`
BRANCH: `agent/bl-2026-032-candidate-vacancy-layout`

## Цель

Восстановить устойчивую компоновку CUBA-экрана `hunttech_CandidateVacancyMatch` без изменения AI-подбора и рабочего процесса рекрутера.

## Разрешённая область

- XML-компоновка `candidate-vacancy-match-screen.xml` без изменения component ID и binding;
- локальный `candidate-vacancy-match-screen.scss` во всех семи темах;
- UI-контрактные тесты;
- спецификация `CandidateVacancyMatchScreen`.

## Запрещённая область

- алгоритм AI-подбора, Java-контроллер, сущности, сервисы, loaders, actions и права;
- БД, миграции и deployment.

## Критерии приёмки

- обычный, пустой, длинный и ошибочный результаты остаются внутри экрана;
- уменьшенная ширина не вызывает наложение или обрезание рабочих областей;
- повторное открытие не меняет геометрию;
- все data bindings, actions, loaders и component ID сохранены;
- локальный root CSS-класс ограничивает все правила экрана;
- SCSS и CUBA screen integrity проходят проверку.

## Чекпоинты

- 2026-09-22: проверены `origin/master`, удалённые ветки и открытые PR; открытых PR нет; `BASE_SHA` зафиксирован.
- 2026-09-22: исходная backlog-карточка отсутствует в актуальном GitHub HEAD; требования приняты из прямого задания Алексея.
- 2026-09-22: определены экран `hunttech_CandidateVacancyMatch`, контроллер `CandidateVacancyMatchScreen.java`, XML-дескриптор и семь локальных SCSS partial.
- 2026-09-22: подтверждена merge-регрессия PR #260: стили вмешиваются во внутреннее позиционирование и размеры Vaadin slot/expand-контейнеров.
- 2026-09-22: удалены широкие переопределения Vaadin slot/expand; узкий режим ограничен непосредственными контейнерами `mainSplit`; семь theme partial синхронизированы.
- 2026-09-22: добавлен UI-контракт локального root-класса, сохранности bindings/ID и идентичности SCSS семи тем; спецификация экрана синхронизирована.
- 2026-09-22: static verification PASS — XML parse, 75 component ID и binding-контракт неизменны, семь theme partial идентичны, SCSS braces и `git diff --check` PASS.
- 2026-09-22: `origin/master` повторно проверен и совпадает с `BASE_SHA`; открытых PR нет, конфликт с актуальным master отсутствует.
- 2026-09-22: Gradle wrapper 5.6.4 отсутствует в кеше, а доступ к `services.gradle.org` закрыт средой; `ScreenViewIntegrityTest`, `CandidateVacancyMatchContractTest` и `buildScssThemes` должны быть подтверждены CI на точном SHA.
- 2026-09-22: runtime/UI smoke не выполнялся, поскольку локальный deployment прямо запрещён задачей; сценарии обычного, пустого, длинного, ошибочного результата, узкой ширины и повторного открытия переданы в PR acceptance checklist.
