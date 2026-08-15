# Задача: привести реализацию форм кандидатов в точное соответствие с эскизами

Дата: 2026-08-15 · Автор задания: Hermes-1 (по решению пользователя) · Статус: Open
Исполнитель: Antigravity (worktree `../hrm-antigravity`)

## Контекст

Ты разработал эскизы форм кандидатов (тестовые экраны), затем по ним сделал
реализацию (боевые формы), которая **сильно отличается от эскизов**.
Пользователь требует: реализация должна точно соответствовать эскизам.

- Эскизы (эталон дизайна): `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-test*-browse.xml`
  и контроллеры `JobCandidateTest*Browse.java` — «Тест 1: Split-View (Master-Detail)»,
  «Тест 2: High-Density DataGrid», «Тест 3: Kanban Pipeline & Метрики»,
  «Тест 4: Executive Card-Grid», «Тест 5: Row Expansion (Details)».
- Реализация (привести к эталону): боевые формы кандидатов — `JobCandidateBrowse`
  (XML + Java), `JobCandidateEdit` (XML + Java) и связанные фрагменты/колонки,
  которые ты делал по этим эскизам.

## Что сделать

1. **Навести порядок в своём worktree** (сейчас он грязный):
   - `git fetch origin` — обновиться от свежего `origin/master`;
   - разрешить незарезолвленный конфликт `build.gradle` (UU) — взять обе стороны;
     версию вручную НЕ бампать (это делает pre-commit hook);
   - закоммитить текущие правки `JobCandidateTest3/4/5Browse.java`;
   - создать рабочую ветку от актуального master: `git switch -c agent/forms-match-eskizy`
     (или обновить `agent/antigravity-dev` — как удобнее, префикс `agent/`);
   - `git push origin HEAD:agent/<ветка>`.
2. **Сверить реализацию с эскизами** по каждому эскизу (1–5) и выписать
   расхождения в `docs/`-отчёт `.ai/reports/2026-08-15-forms-match-audit.md`:
   - компоновка (layout, секции, порядок, отступы, карточки/таблицы);
   - стили (stylename, SCSS, цвета/размеры/скругления — по концепции
     `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md` и эталону
     `IteractionListEdit`/эскизов);
   - поведение (кнопки, фильтры, раскрытие строк, kanban-стадии, детали);
   - данные (колонки/поля, которые показываются).
3. **Привести реализацию в соответствие** — правки в боевых формах по списку
   расхождений. НЕ ломать: бизнес-логику, CUBA-контракты (data containers,
   loaders, actions, валидаторы), права доступа. Структуру БД/entity/миграции
   НЕ менять (без согласования — запрещено).
4. **Проверить визуально** (разрешено поднимать СВОЮ ветку):
   - сборка: `bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-web:compileJava`
     (все gradle-прогоны — только через эту обёртку, mutex);
   - запуск своей ветки на общем Tomcat:
     `bash ../hunttech_recruiting/scripts/start-app.sh --branch "$PWD"`
     (guard'ы: без merge-конфликтов, без новых миграций относительно
     origin/master, mutex; если откажет — разберись, не обходи через --force);
   - вход в приложение: `http://localhost:8080/hrm/`
     **логин: `admin` / пароль: `Dodo-2012`** (локальная dev-копия);
   - меню «Рекрутинг → Кандидаты» и «Тест 1–5»: сверь боевую форму с каждым
     эскизом, сделай скриншоты (до/после правок) в `/tmp/hrm-shots-forms/`;
   - ПОСЛЕ проверки верни master на общий Tomcat:
     `bash ../hunttech_recruiting/scripts/start-app.sh` (без флагов).
5. **Тесты и docs** (обязательно, правила проекта):
   - контрактные тесты форм + `ScreenViewIntegrityTest` через agent-gradle.sh;
   - при правке SCSS — `:app-web:buildScssThemes`;
   - синхронизация `docs/ui/{FormName}_Spec.md` и `docs/entities/JobCandidate.md`
     (Business & Context Intro, история изменений YYYY-MM-DD);
   - inline-комментарии в изменённых XML (правило xml-screen-documentation).
6. **PR** (base=master): описание «что изменено, как сверено с эскизами
   (ссылки на скриншоты), что ждёшь от Hermes-1», метка WAITING_FOR_HERMES,
   отчёт `.ai/reports/2026-08-15-forms-match-audit.md` в PR-описании.

## Definition of Done

- [ ] worktree чистый, ветка `agent/*` от свежего origin/master, запушена
- [ ] расхождения «реализация vs эскизы» задокументированы (аудит-отчёт)
- [ ] реализация приведена в соответствие: компоновка, стили, поведение, данные
- [ ] скриншоты до/после в `/tmp/hrm-shots-forms/` + в PR
- [ ] боевые формы открываются без ошибок (в т.ч. без UNFETCHED ATTRIBUTE
      ACCESS — view контейнеров покрывают все getter'ы контроллеров)
- [ ] тесты зелёные, docs синхронизированы, Tomcat возвращён на master
- [ ] PR с меткой WAITING_FOR_HERMES, merge ждёт Hermes-1

Если по ходу выяснится, что какие-то эскизные решения конфликтуют с
бизнес-сценарием/CUBA-контрактом — зафиксируй в отчёте и в PR-комментарии,
не «молча отходи» от эскиза.
