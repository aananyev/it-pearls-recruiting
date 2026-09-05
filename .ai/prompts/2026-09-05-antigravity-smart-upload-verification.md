# ЗАДАНИЕ Antigravity: Проверка и тестирование визарда «Умная загрузка вакансии» (OpenPositionReestrBrowse)

Дата: 2026-09-05 · Постановщик: Hermes-1 (DevOps/PM) · Приоритет: высокий
Ветка: agent/antigravity-dev (перед стартом: git fetch && git rebase origin/master)

## ШАГ 0 — ОЧИСТКА КОНТЕКСТА (обязателен ПЕРЕД началом работы)
1. Убедись, что все субагент-делегирования предыдущей задачи завершены.
2. Всё незакоммиченное — закоммитить/запушить (незакоммиченное теряется).
3. НОВАЯ сессия: /new (не /compact — оставляет сжатый хвост прошлой задачи).
4. Первым делом прочитать ЭТОТ файл задания целиком; работать по нему,
   без опоры на память о прошлых задачах.
5. git fetch && git rebase origin/master.

## ЦЕЛЬ
Проверить и протестировать алгоритм и схему загрузки визарда «Умная AI-загрузка
и открытие вакансии»: от нажатия кнопки в OpenPositionReestrBrowse до создания
OpenPosition в БД и её видимости в REST-выгрузке на сайт. Выявить расхождения
с требованиями, задокументировать алгоритм, при необходимости доработать
визард (см. ТРЕБОВАНИЯ).

## ОБЪЕКТ ИССЛЕДОВАНИЯ (диагностика Hermes-1, сверь с актуальным кодом)
Цепочка:
- кнопка smartUploadBtn: modules/web/src/com/company/hunttech/web/screens/
  openposition/open-position-reestr-browse.xml:230 → OpenPositionReestrBrowse.java:925
- экран визарда: smart-open-position-upload-screen.xml (диалог 700×1100) +
  SmartOpenPositionUploadScreen.java — 3 режима входа: URL
  (runAsyncUrlAnalysis:196), файл (:274), текст (:314) → AI-парсинг в
  SmartOpenPositionParsedData → предпросмотр displayAnalysisResult:341 →
  onSaveNewPositionClick:437 → сервис
- сервис: modules/core/src/com/company/hunttech/service/
  SmartOpenPositionIngestServiceBean.java — extractTextFromFile:48,
  parseVacancyText:105, findDuplicate:457, createOpenPosition:480:
  setOpenClose(false), **setSignDraft(false) (:494)**,
  setPriority(UNDER_REVIEW=-2), findOrCreateProject (:69),
  findOrCreatePositionType, findGrade, findCity, создание SkillTree
- REST-выгрузка: modules/web/src/com/company/hunttech/rest-queries.xml,
  запрос openPositionPublic — фильтры: openClose, **signDraft**,
  internalProject=false, priority<>-2; контрактный тест
  OpenPositionRestQueriesContractTest

## ВЫЯВЛЕННОЕ РАСХОЖДЕНИЕ (проверить первым делом)
Требование пользователя: вакансия из визарда создаётся С ПРИЗНАКОМ «Черновик»
(signDraft=true) и НЕ выгружается на сайт, пока пользователь черновик не снимет.
Факт в коде: createOpenPosition ставит signDraft=FALSE, а от сайта вакансию
прячет priority=-2 («На проверку»). Это ДВА разных механизма сокрытия.
РЕШИТЬ (с согласованием пользователя через TG при неоднозначности):
- привести визард к требованию: signDraft=true при создании; priority=-2
  оставить как дополнительный статус или убрать — обосновать;
- проверить снятие черновика в OpenPositionEdit (signDraftCheckBox:369,
  подписка :4279, метка (DRAFT) :4284) → после снятия вакансия должна
  появиться в openPositionPublic (при прочих условиях).

## ТРЕБОВАНИЯ К ДОРАБОТКЕ ВИЗАРДА (если подтвердится необходимость)
1. Диалог с пользователем при нехватке данных: если для создания сущности
   (Project, Person/рекрутер, City, Position, Grade) информации нет или она
   неоднозначна — визард НЕ должен молча создавать/подставлять: показать
   вопрос пользователю (дополнить данные / создать сущность руками /
   пропустить с обоснованием). Реализация — по месту (Vaadin8-диалог или
   подтверждение на шаге предпросмотра), минимум вторжения.
2. Анти-дубли: перед созданием СОПУТСТВУЮЩИХ сущностей (Project, Person и
   др.) — обязательная проверка существующих (по названию/ключевым полям);
   findDuplicate OpenPosition тоже проверить тестом на реальном кейсе.
3. Логи [SMART_VACANCY_OPENING] сохраняй/дополняй — они источник диагностики.

## ТЕСТОВЫЙ СЦЕНАРИЙ (обязательный прогон)
1. Возьми РЕАЛЬНУЮ вакансию с need.ssp-soft.com (любую актуальную).
2. Запусти визард в UI (локальный http://localhost:8080/hrm/, вход через CDP:
   логин alan, пароль Dodo-2012; ПЕРЕД вводом ПОЛНОСТЬЮ стирать старые
   данные в полях логин-скрина).
3. Пройди полный цикл до создания OpenPosition; зафиксируй скриншотами:
   предпросмотр парсинга, шаги диалогов нехватки данных, итоговую карточку.
4. Проверь в БД/UI: signDraft, priority, openClose, project (дубль? новый?),
   связанные сущности; проверь REST: до снятия черновика GET openPositionPublic
   НЕ содержит вакансию, после снятия — содержит (curl локального REST или
   контрактный тест).
5. Повтори прогон с заведомо неполным текстом вакансии (без города/зарплаты) —
   проверь диалог с пользователем (требование 1).
6. Повторный запуск той же вакансии — findDuplicate должен вернуть существующую,
   дубль не создаётся.

## ИСТОЧНИКИ АЛГОРИТМА (изучить ДО тестов)
- Навык hrm-operator: ~/.hermes/profiles/hrm-operator/skills/
  hunttech-vacancy-opening/SKILL.md — правила заполнения OpenPosition из
  вакансии заказчика (SSP), именование Project «ДКС "Заказчик. Проект
  Аккаунт-менеджера" /Штат Hunttech ТК/ГПХ или ИП. Актирование N месяца/»,
  priority=2 NORMAL по умолчанию, поля-булевы только need_letter/need_exercise.
- Бот HunttechOpenCloseBot: /Users/alekseyananyev/StudioProjects/hunttech-recruiting-bot —
  services/hrm.py (перечисление приоритетов -1 Draft..4 Critical, поля
  open_close/sign_draft в payload), vacancy.py, handlers/ — СРАВНИТЬ его
  схему создания/открытия вакансий с визардом: что взять в визард
  (например, явный Draft=-1 или sign_draft=true), что нет. Бот НЕ изменять,
  только читать (чужой проект — не коммитить в него).

## ПОРЯДОК РАБОТЫ (утверждённая схема субагентов)
1. Субагент-аналитик (ПЕРЕД работой): по итогам чтения кода — черновик
   алгоритма в docs/ (см. раздел ДОКУМЕНТАЦИЯ).
2. Реализация доработки (если подтверждена) — отдельными коммитами (рус.),
   push origin HEAD:agent/antigravity-dev после каждого шага.
3. Тесты: bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test
   --tests com.company.hunttech.core.OpenPositionRestQueriesContractTest
   --no-daemon + ScreenViewIntegrityTest + новые unit/контрактные тесты на
   createOpenPosition (signDraft=true, анти-дубли, диалог нехватки данных —
   что тестируемо без UI). Тесты на изменение поведения визарда ОБЯЗАТЕЛЬНЫ.
4. UI-прогон сценария (start-app.sh --branch "$PWD"; после — НЕМЕДЛЕННО
   вернуть master start-app.sh без флагов). Скриншоты до/после.
5. Субагент-QA (ПЕРЕД PR): построчный diff-ревью границ ниже, тесты зелёные,
   PASS в .ai/reports/qa-smart-upload-YYYY-MM-DD.md. Без PASS PR не создавать.
6. ocr review --audience agent (скилл open-code-review) — результат в PR.
7. PR base=master, метка WAITING_FOR_HERMES.
8. TG-отчёт: hermes send -t telegram:272980897 с секцией '## Субагенты'
   (аналитик/QA — что сделали) и выводом по расхождению черновика.

## ДОКУМЕНТАЦИЯ (обязательно, субагент-аналитик)
По правилам living-documentation:
- docs/architecture/SmartOpenPositionUpload_Spec.md — полный алгоритм:
  цепочка экран→сервис→БД→REST, схема черновика/приоритетов, анти-дубли,
  диалоги нехватки данных, Business & Context Intro;
- обновить docs/ui/ по OpenPositionReestrBrowse если менялся UI визарда;
- раздел «Как корректировать» — точки расширения (новые источники вакансий,
  новые обязательные поля).
Документация — часть PR, не постфактум.

## ЖЁСТКИЕ ГРАНИЦЫ
1. НЕ трогать: open-position-edit.xml и job-candidate-edit.xml (у Hermes-2
   активные задачи по таблицам), защищённые формы CompanyEdit/ExtSettingsWindow/
   ExtUserEdit и их SCSS, ReestrBrowse_Design_Contract.md (ветка hermes2).
2. hunttech-recruiting-bot — только чтение (чужой репозиторий).
3. Миграции на общую БД, деплой master, прод hr.hunttech.ru — НЕ трогать
   (миграции — заявка Hermes-1).
4. Бизнес-логика вне визарда (IteractionList, резолвинг вакансий бота,
   OPEN_CLOSE-процессы) — не затрагивать; изменения локализуются в
   SmartOpenPositionUploadScreen/SmartOpenPositionIngestServiceBean/
   rest-queries.xml (если нужен фильтр) + тесты + docs.
5. Data View Integrity: все новые геттеры Java — задекларировать во view
   контейнеров/XML (.cursor/rules/data-view-integrity.mdc).

## ЭСКАЛАЦИЯ (спросить, а не решать молча)
- Если требование «черновик» конфликтует с логикой priority=-2 («На проверку»)
  так, что оба механизма несовместимы — зафиксировать варианты и спросить
  пользователя через TG.
- Если need.ssp-soft.com недоступен/без вакансий — взять текст вакансии из
  навыков hrm-operator (SSP-кейсы) и доложить о замене источника.

## ПРИЁМКА (проверяет Hermes-1)
- тестовый прогон задокументирован скриншотами (6 шагов сценария);
- signDraft-семантика визарда соответствует требованию (или обоснованный
  откат с решением пользователя);
- REST-проверка: черновик не виден в openPositionPublic, после снятия виден;
- анти-дубли: повторный прогон не создал дублей (SQL-подтверждение);
- диалог нехватки данных работает на неполном тексте;
- новые тесты зелёные + контрактные REST-тесты зелёные;
- docs/architecture/SmartOpenPositionUpload_Spec.md в PR;
- QA PASS + OCR PASS; TG-отчёт отправлен.
