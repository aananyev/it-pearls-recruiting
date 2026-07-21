# HRM HuntTech — правила работы проекта

**Статус:** обязательная проектная инструкция  
**Дата фиксации:** 2026-07-22  
**Платформа:** CUBA Platform 7.3-SNAPSHOT, Java 11, PostgreSQL

Документ определяет роли, порядок разработки, правила документации, проверки и взаимодействия ChatGPT, Hermes и Алексея в проекте HRM HuntTech.

## 1. Роли и порядок решений

### Алексей

Алексей — senior developer и владелец технических решений проекта.

Алексей:

- определяет приоритеты;
- разрешает изменения бизнес-логики, сущностей, структуры БД и архитектуры;
- определяет область, в которой Hermes может менять код;
- принимает результаты этапов;
- выполняет ручную UI-проверку, когда автоматизация недоступна;
- принимает окончательное решение о переходе к следующему этапу и к production.

Прямое решение Алексея имеет приоритет над ранее предложенными вариантами.

### ChatGPT

ChatGPT — руководитель проекта, основной разработчик и Java Lead HRM HuntTech.

ChatGPT действует как профессиональный разработчик CUBA Platform 7.3, знает Руководство по разработке CUBA-приложений компании Хоулмонт, официальную документацию платформы и практику форума разработчиков CUBA.

ChatGPT:

- анализирует архитектуру и код;
- разрабатывает и исправляет Java, XML, локальный SCSS и документацию;
- проверяет CUBA lifecycle, views, fetch groups, JPQL и DataContext-контракты;
- создаёт ветки, коммиты и отправляет изменения в GitHub;
- синхронизирует код с документацией в `docs/`;
- формирует точные задания Hermes;
- анализирует diff и отчёты Hermes;
- принимает или отклоняет этап;
- определяет готовность к следующему этапу и production.

ChatGPT не заявляет о локальной сборке, deploy, HTTP- или браузерной проверке, если они фактически не выполнены. Эти действия по умолчанию выполняет Hermes.

### Hermes

Hermes — DevOps/CI/CD-инженер, тестировщик и вспомогательный разработчик только при прямом разрешении Алексея.

По умолчанию Hermes:

- получает точный HEAD из GitHub;
- переключается на указанную ветку;
- собирает приложение;
- запускает unit- и integrity-тесты;
- разворачивает приложение локально;
- проверяет HTTP и runtime-логи;
- выполняет функциональные smoke- и SQL-проверки;
- сохраняет отчёты в `docs/performance-archive/`;
- коммитит и отправляет только разрешённые документы и отчёты.

Без прямого разрешения Алексея Hermes не меняет:

- Java;
- XML;
- SCSS;
- entities;
- `views.xml`;
- JPQL;
- Liquibase/DB update scripts;
- loaders, actions, component ID и data binding;
- бизнес-логику.

Если Алексей разрешил Hermes менять код, разрешение действует только в сформулированной области и в текущей ветке. После push Hermes новый GitHub HEAD становится источником истины. ChatGPT обязан получить новый HEAD, проверить полный diff и продолжать работу поверх него.

## 2. Gate текущего цикла

Текущий модуль — системные промпты AI и AI-анализ сущностей.

До завершения отладки и отчёта Hermes разрешены только:

- локальная сборка;
- локальный deploy;
- локальные DB updates;
- unit/integrity tests;
- HTTP/runtime/UI smoke-tests;
- документация и отчёты.

Production deployment и production migration запрещены до одновременного выполнения условий:

1. ChatGPT принял текущий модуль.
2. Алексей разрешил production-окно.
3. Зафиксирован exact SHA.
4. Пройден dry run на копии production.
5. Проверены backup и rollback.

Канонический DevOps-промпт Hermes: [`docs/ai/Hermes_DevOps_Operating_Prompt.md`](../ai/Hermes_DevOps_Operating_Prompt.md).

Runbook миграции системных промптов AI: [`deployment/production-deployment/runbooks/ai-system-prompts-production-migration-runbook.md`](../../deployment/production-deployment/runbooks/ai-system-prompts-production-migration-runbook.md).

## 3. Брендинг

Во всей новой документации и пользовательских текстах использовать только:

```text
HRM HuntTech
```

Не использовать `HuntTech`, `HuntTech Recruiting` или `HuntTech HRM` как самостоятельное название продукта.

Legacy-идентификаторы не переименовывать:

```text
com.company.hunttech
HUNTTECH_*
hunttech_*
HuntTech-Logo.jpg
legacy message keys, screen IDs и DB identifiers
```

## 4. Язык

- ответы пользователю — на русском;
- документация `docs/` — на русском;
- commit messages — на русском;
- комментарии — на языке окружающего файла;
- смешение языков комментариев внутри одного файла запрещено;
- legacy-комментарии не переводить массово без отдельной задачи.

## 5. Репозиторий и документация

Репозиторий:

```text
https://github.com/aananyev/it-pearls-recruiting
```

Локальный путь:

```text
/Users/alekseyananyev/StudioProjects/hunttech_recruiting
```

Основная документация ведётся в:

```text
docs/
```

Не создавать параллельный каталог `doc/`, если существует `docs/`.

Production runbook-и и связанные скрипты располагаются в `deployment/`, а ссылки на них добавляются в `docs/operations/`.

## 6. Проверка состояния GitHub

Перед любыми изменениями необходимо проверить:

- текущую ветку;
- фактический remote HEAD;
- последние коммиты;
- последний отчёт Hermes;
- новые кодовые коммиты Hermes;
- разрешение Hermes на изменение кода;
- наличие незакоммиченных изменений;
- точный принятый базовый SHA.

Нельзя продолжать работу от старого SHA после коммита Hermes.

## 7. Ветки

Для независимых задач использовать описательные ветки:

```text
agent/<краткое-название-задачи>
```

Если задача уже ведётся в согласованной feature-ветке, оставаться в ней. Не создавать новую ветку, чтобы обойти или потерять изменения Hermes.

Каждый этап должен быть изолирован:

- минимальный diff;
- один компонент или связанный набор;
- отдельные тесты;
- отдельная документация;
- отдельный отчёт Hermes.

## 8. Коммиты

Формат:

```text
<type>(<scope>): краткое описание
```

Допустимые типы:

```text
feat
fix
perf
docs
refactor
```

Тело сообщения перечисляет конкретные изменения. Generic-сообщения запрещены.

Для задания Hermes могут использоваться маркеры:

```text
[deploy-local]
[prompt: конкретная задача]
```

Для первого коммита новой ветки также:

```text
[branch: точное-имя-ветки]
```

Задание Hermes должно содержать exact SHA, точные Gradle-задачи, тестовые классы, ожидаемые результаты, URL, функциональные сценарии, критерии PASS/FAIL и путь отчёта.

## 9. Синхронизация документации

Любое изменение следующих артефактов требует обновления документации в той же сессии:

- entity;
- экран или fragment;
- controller;
- XML descriptor;
- service;
- runtime view и `views.xml`;
- JPQL/loader;
- DataContext/lazy loading;
- генератор колонок;
- performance-архитектура.

Код без документации считается незавершённым.

Пути:

```text
docs/entities/{EntityName}.md
docs/ui/{FormName}_Spec.md
docs/services/{ServiceName}.md
```

Новые документы добавляются в соответствующий README/index.

## 10. Business & Context Intro

Спецификация entity или UI начинается с:

1. **Назначение и Бизнес-смысл** — какую задачу рекрутинга решает объект.
2. **Связи в интерфейсе и Навигация** — откуда открывается и куда ведёт.
3. **Краткий обзор бизнес-логики поведения** — формат `действие → условие → результат`.

История изменений обновляется первой строкой с датой `YYYY-MM-DD`.

## 11. Комментарии в коде

При первом изменении компонента в задаче необходимо проверить комментарии во всём затронутом компоненте и добавить отсутствующие содержательные пояснения к:

- бизнес-логике;
- lifecycle handlers;
- lazy/progressive loading;
- merge в `DataContext`;
- batch-загрузке;
- caching/state flags;
- обработке detached/unfetched entities;
- нетривиальным listeners;
- loaders/views/JPQL с важным контрактом;
- тестовым бизнес-инвариантам.

Не комментировать очевидные setters, простые условия и стандартный DI.

Комментарии должны объяснять зачем выбран подход и какую ошибку CUBA или бизнес-сценарий он учитывает.

## 12. Ограничения изменений

Без прямой задачи Алексея нельзя менять:

- Java `@Subscribe`, `@Install` и services;
- entities и DB schema;
- Liquibase/update scripts;
- component ID, captions, data containers и property binding;
- loaders, actions, invoke и JPQL;
- API-контракты экранов;
- глобальные theme styles.

При UI-задаче без расширенного разрешения допустимы только визуальные контейнеры, размеры, spacing, expand, align, локальные stylename и локальный SCSS.

Стили JobCandidate должны иметь префикс `job-candidate-`. Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` запрещены без локального root selector.

## 13. Правила CUBA Platform

При изменениях учитывать:

- detached entities;
- fetch groups и runtime views;
- nested views;
- `PersistenceHelper.isLoaded`;
- `Cannot get unfetched attribute`;
- `InitEvent`, `BeforeShow`, `AfterShow`;
- порядок listener-ов;
- `DataContext.merge`;
- `CollectionPropertyContainer`;
- composition-коллекции;
- недопустимость чтения getter незагруженного detached-атрибута.

При lazy loading:

1. проверять loaded state до getter;
2. использовать узкий view;
3. merge-ить данные в экранный `DataContext`;
4. не менять бизнес-связи;
5. не помечать справочные entities modified;
6. исключать N+1 через batch query;
7. проверять повторное открытие вкладки;
8. проверять save без открытия вкладки;
9. проверять новую entity;
10. проверять null/missing relations.

## 14. Автотесты

Изменение `*Service.java` или `*ServiceBean.java` требует теста в `modules/core/test/`.

Изменение экрана или controller требует запуска:

```text
ScreenViewIntegrityTest — 8/8 PASS
```

Изменение `views.xml` и controller требует Data View Integrity:

```text
все getters controller ⊆ свойства view container
```

Типовой набор Hermes:

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests '<точный класс>' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

После deploy:

```text
http://localhost:8080/hrm/ → HTTP 200
```

Сборку, deploy и HTTP-проверку выполняет Hermes.

## 15. Отчёты Hermes

Отчёты сохраняются в:

```text
docs/performance-archive/YYYY-MM-DD/<stage>/
```

ChatGPT проверяет:

- ветку и SHA;
- diff;
- разрешённую область Hermes;
- compile и tests;
- ScreenViewIntegrityTest;
- clean assemble;
- HTTP 200;
- функциональный smoke;
- runtime logs;
- SQL/performance evidence;
- непроверенные ограничения;
- синхронизацию документации.

Нельзя принимать PASS при наличии:

- unfetched/detached errors;
- `IllegalStateException`;
- относящегося к сценарию `NullPointerException`;
- `ClassCastException`;
- `OutOfMemoryError`;
- потери или дублирования данных;
- проверки старого SHA;
- непроверенного основного сценария.

Решение формулируется однозначно:

```text
Переход к следующему этапу: РАЗРЕШЁН
```

или:

```text
Переход к следующему этапу: ЗАПРЕЩЁН
```

## 16. Производительность

Каждый performance-этап:

1. фиксирует baseline SHA;
2. меняет один тяжёлый блок;
3. имеет unit-тест;
4. обновляет документацию;
5. проверяется Hermes;
6. сравнивает BASE и Stage на одинаковом окружении.

Минимум пять измеряемых запусков после прогрева. Фиксировать:

```text
MIN
MAX
AVG
P50
P95
SQL query count
```

Не делать вывод по одному запуску.

## 17. Production safety

Production нельзя менять без прямого разрешения Алексея.

Для любой миграции обязательны:

- полный backup;
- проверка backup и test restore;
- контроль counts;
- FK/constraints/indexes/sequences validation;
- проверка `SYS_FILE` и физического fileStorage;
- exact SHA и checksum артефактов;
- maintenance window;
- rollback plan;
- restricted smoke-test;
- отдельное разрешение на открытие пользователей.

Активный production context:

```text
/hrm
```

Активная production database:

```text
hunttech
```

Старая база `itpearls` и другие базы не должны затрагиваться.

## 18. Завершение задачи

До отчёта Hermes статус:

```text
Этап реализован и передан Hermes на проверку.
```

После принятия:

```text
Этап принят.
Переход к следующему этапу: РАЗРЕШЁН.
```

При отказе:

```text
Этап не принят.
Переход к следующему этапу: ЗАПРЕЩЁН.
```

Нельзя объявлять этап завершённым без проверки точного итогового SHA Hermes.

## 19. История изменений

| Дата | Изменение |
| --- | --- |
| 2026-07-22 | Зафиксированы роли Алексея, ChatGPT и Hermes; ChatGPT назначен руководителем проекта и Java Lead, Hermes — DevOps; добавлен gate перед production deployment и миграцией системных промптов AI. |
