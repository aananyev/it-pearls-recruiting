# SkillAnalysisService (`hunttech_SkillAnalysisService`)

> Анализ текста резюме кандидата или описания вакансии: нейросеть (AI Control Plane, capability TEXT_GENERATION) извлекает названия навыков, сервис сопоставляет их со справочником `hunttech_SkillTree` и возвращает коллекцию сущностей навыков, упомянутых в тексте. Навыки, найденные нейросетью, но отсутствующие в справочнике, пишутся в лог (WARN) — администратор анализирует их и добавляет в skilltree.

**Связанные документы:** [AI_Function_Management_Architecture](../architecture/HRM_HuntTech_AI_Function_Management_Architecture.md) · [AI_INTEGRATION](../integrations/ai/AI_INTEGRATION.md) · [HrmAiService](HrmAiService.md) · [ProjectAiService](ProjectAiService.md) · [ProjectLogoImageProcessingService](ProjectLogoImageProcessingService.md)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Рекрутёры работают с большим числом резюме кандидатов и описаний вакансий. Ключевой шаг подбора — сопоставление навыков: какие навыки заявлены в резюме, какие требования предъявляет вакансия, насколько они пересекаются. Ручное выделение навыков из текста трудоёмко и субъективно, а справочник навыков (`HUNTTECH_SKILL_TREE`) ведётся централизованно.

**SkillAnalysisService** автоматизирует этот шаг: принимает произвольный текст (резюме или описание вакансии) и возвращает коллекцию сущностей `SkillTree` — навыков из справочника, которые упоминаются в тексте. Выделение названий навыков выполняет нейросеть через единый AI Control Plane (`AiExecutionService.executeText`, стабильный function code `SKILLS_EXTRACT`); сервис лишь сопоставляет ответ со справочником и возвращает сущности — поэтому результат всегда привязан к справочнику, а не к «вольному» тексту модели.

Сервис поддерживает четыре уровня анализа: все навыки, основные/обязательные, второстепенные/желательные и третьестепенные. Для описания вакансии это позволяет отделить обязательные требования («необходимо») от желательных («желательно»); для резюме — ключевые навыки от дополнительных.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

| Точка вызова | Роль |
|--------------|------|
| Будущие экраны JobCandidateEdit (парсинг резюме) | Анализ текста резюме → навыки кандидата |
| Будущие экраны вакансий (OpenPositionEdit и др.) | Анализ описания вакансии → требуемые навыки кандидата |
| `web-spring.xml` | Регистрирует интерфейс в `WebRemoteProxyBeanCreator` — web-контекст получает CUBA service proxy `hunttech_SkillAnalysisService` |

Сервис входит в AI Control Plane: AI-этап маршрутизируется через `AiExecutionService.executeText` (стабильный function code `SKILLS_EXTRACT`, capability `TEXT_GENERATION`, политики `USER_OVERRIDE_ALLOWED`/`FALLBACK_TO_ADMIN`, корпоративные credentials из `AdminAiConfiguration`). Промпт и модель администратор меняет в «Управление AI → Функции AI» без выпуска кода.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Анализ текста** → сервис валидирует текст (не пуст, ≤ 120 000 символов) → вызывает `AiExecutionService.executeText("SKILLS_EXTRACT", {sourceText, skillLevel})`.
- **Нейросеть** возвращает JSON-массив названий навыков (например, `["Java", "SQL", "Scrum"]`); разбор ответа устойчив к markdown-ограждениям и построчному/запятому формату.
- **Сопоставление со справочником** (`SkillNameMatcher`): точное совпадение по нормализованной форме, иначе — токенное совпадение (название нейросети может дать несколько навыков справочника, например `Java Spring` → `Java` + `Spring`); дубликаты исключаются.
- **Неизвестные навыки** (нет в справочнике) → запись в лог **WARN** с перечнем названий — администратор анализирует и добавляет их в `HUNTTECH_SKILL_TREE`.
- **AI недоступен** (функция не активна, нет credentials, ошибка провайдера) → лог `warn` + классический fallback: `SkillNameMatcher.matchText` ищет навыки справочника прямо в тексте (токенное совпадение). Анализ никогда не прерывается недоступностью ИИ.
- **Пустой или неразборчивый ответ AI** → пустой список навыков.

---

## 1. Архитектура и размещение

| Элемент | Путь |
|---------|------|
| Интерфейс Service API | `modules/global/src/com/company/hunttech/service/SkillAnalysisService.java` |
| Реализация middleware | `modules/core/src/com/company/hunttech/service/SkillAnalysisServiceBean.java` |
| Словарный матчинг (чистая функция) | `modules/core/src/com/company/hunttech/service/SkillNameMatcher.java` |
| AI-функция | `AiFunctionConfiguration` code `SKILLS_EXTRACT`, capability `TEXT_GENERATION` |
| Seed-миграция функции | `modules/core/db/update/postgres/26/260815-1-addSkillAnalysisAiFunction.sql` + `modules/core/db/changelog/260815-1-addSkillAnalysisAiFunction.xml` |
| Реестр web proxy | `modules/web/src/com/company/hunttech/web-spring.xml` |
| CUBA service name | `hunttech_SkillAnalysisService` |

Зависимости реализации: `AiExecutionService` (AI-этап), CUBA `DataManager` (справочник skilltree), Jackson `ObjectMapper` (разбор JSON-ответа), Apache Commons не используется.

### 1.1. Граница web/core

Аналогично `ProjectAiService`/`HrmAiService`: core-реализация живёт в middleware webapp, web-экраны получают её только через CUBA service proxy, зарегистрированный в `WebRemoteProxyBeanCreator` (`web-spring.xml`). Class-based lookup запрещён.

## 2. AI-функция `SKILLS_EXTRACT`

| Параметр | Значение |
|----------|----------|
| code | `SKILLS_EXTRACT` |
| name | «Извлечение навыков из текста» |
| capability | `TEXT_GENERATION` |
| system_prompt | Русский: извлечение навыков из резюме/вакансии по уровню анализа, названия из текста, запрет выдумывания, строго JSON-массив |
| prompt_template | `Уровень анализа: ${skillLevel}\n\nТекст для анализа:\n${sourceText}` |
| temperature | 0.3 (детерминированное извлечение) |
| max_tokens | 500 (до ~100 навыков) |
| execution_policy | `USER_OVERRIDE_ALLOWED` |
| fallback_policy | `FALLBACK_TO_ADMIN` |
| seed | INSERT-only, идемпотентный (`ON CONFLICT (CODE) DO NOTHING`); существующая админская настройка не перезаписывается |

Значение `${skillLevel}`: `ALL` | `MAIN` | `SECONDARY` | `TERTIARY` — задаёт сервис из констант интерфейса (`LEVEL_*`).

## 3. API сервиса

```java
String NAME = "hunttech_SkillAnalysisService";
String FUNCTION_SKILLS_EXTRACT = "SKILLS_EXTRACT";
String PARAM_SOURCE_TEXT = "sourceText";
String PARAM_SKILL_LEVEL = "skillLevel";
String LEVEL_ALL = "ALL";          // все навыки
String LEVEL_MAIN = "MAIN";        // основные/обязательные
String LEVEL_SECONDARY = "SECONDARY"; // второстепенные/желательные
String LEVEL_TERTIARY = "TERTIARY";   // третьестепенные

List<SkillTree> analyzeAll(String sourceText);       // 1. все найденные навыки
List<SkillTree> analyzeMain(String sourceText);      // 2. основные/обязательные
List<SkillTree> analyzeSecondary(String sourceText); // 3. второстепенные/желательные
List<SkillTree> analyzeTertiary(String sourceText);  // 4. третьестепенные (если есть)
```

Возврат — коллекция сущностей `SkillTree` (view `skillTree-parser-view`), без дубликатов, в порядке обнаружения. Исключение — `DevelopmentException`: пустой текст («Текст для анализа навыков пуст.»), текст длиннее 120 000 символов.

## 4. Алгоритм

### 4.1. Справочник

Запрос: `select e from hunttech_SkillTree e where (e.notParsing is null or e.notParsing = false)` — навыки с флагом «Не парсить» (`notParsing = true`) исключаются из анализа. Мягко удалённые записи DataManager исключает автоматически.

### 4.2. Токенное сопоставление (SkillNameMatcher)

- **Нормализация**: нижний регистр, обрезка, схлопывание пробелов.
- **Токенизация** по пробелам с удалением обрамляющей пунктуации; внутренние спецсимволы сохраняются: `C++` → `c++`, `C#` → `c#`, `ASP.NET` → `asp.net`, `1С` → `1с`.
- **Совпадение** — точная последовательность токенов: `Java EE` ≡ `java ee`, но `Java` ≠ `JavaScript` (разные токены).
- **Приоритет** точного совпадения всего названия: `Java Core` в справочнике → возвращается только `Java Core`, без подтягивания `Java`.
- **Составное название** без точного совпадения может дать несколько навыков: `Java Spring` → `Java` + `Spring`.
- **Дедупликация** по id сущности.

### 4.3. Неизвестные навыки (контракт логирования)

```text
WARN AI-анализ нашёл навыки, отсутствующие в справочнике skilltree —
     администратору добавить их в HUNTTECH_SKILL_TREE: [Kotlin, Rust]
```

Порядок действий администратора: открыть экран справочника навыков (SkillTree), добавить найденные названия, при необходимости пометить «Не парсить». После добавления навыки начнут возвращаться сервисом без изменений кода.

### 4.4. Классический fallback (AI недоступен)

При `RuntimeException` из `executeText` (функция не активна, нет credentials, ошибка провайдера) — `SkillNameMatcher.matchText` ищет навыки справочника прямо в тексте тем же токенным правилом. Уровень в fallback не различается (возвращаются все найденные навыки) — это документированная деградация, при которой анализ не прерывается.

## 5. Устойчивость разбора ответа нейросети

Принимаются: чистый JSON-массив `["Java", "SQL"]`; JSON в markdown-ограждении (` ```json … ``` `); построчный/запятый список без JSON (`Java, SQL` или `Java\nSQL`). Пустой/неразборчивый ответ → пустой список.

## 6. Тестирование

| Файл | Назначение |
|------|------------|
| `modules/core/test/com/company/hunttech/core/SkillNameMatcherTest.java` | чистые unit-тесты матчинга: регистр, составные названия, ловушка JavaScript/Java, `C++`/`C#`, русские навыки, неизвестные, дедупликация, приоритет точного совпадения, поиск по тексту |
| `modules/core/test/com/company/hunttech/core/SkillAnalysisServiceBeanTest.java` | контейнерный тест со стабом `AiExecutionService`: JSON/plain/markdown-ответы, уровни, fallback при отказе AI, исключение `notParsing`, пустой текст, пустой ответ |
| `modules/core/test/com/company/hunttech/core/SkillAnalysisAiFunctionSeedContractTest.java` | seed-контракт: INSERT-only/идемпотентность, TEXT_GENERATION, русские промпты с `${sourceText}`/`${skillLevel}` и требованием JSON-массива, include в master, 4 метода интерфейса, proxy в web-spring.xml |

Запуск:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-core:test \
  --tests '*SkillNameMatcherTest*' \
  --tests '*SkillAnalysisServiceBeanTest*' \
  --tests '*SkillAnalysisAiFunctionSeedContractTest*' \
  --no-daemon
```

## 7. Инструкция по развертыванию

- Код входит в артефакты `app-global`, `app-core`, web-клиент; миграция БД — seed AI-функции `SKILLS_EXTRACT` (применяется штатным `updateDb`/Liquibase; production-скрипт `260815-1-addSkillAnalysisAiFunction.sql` — INSERT-only, идемпотентный).
- Web-артефакт содержит запись `hunttech_SkillAnalysisService` в `WebRemoteProxyBeanCreator`.
- Для AI-этапа администратор настраивает в «Управление AI»: активную корпоративную конфигурацию (любой из 10 провайдеров; для РФ — DeepSeek/GigaChat/Yandex) и привязывает её к функции `SKILLS_EXTRACT`. Без настройки — автоматический классический словарный поиск.
- Проверка после деплоя: вызов `analyzeAll` на тексте с заведомо известными навыками справочника → возвращаются соответствующие сущности `SkillTree`; навыки вне справочника появляются в логе (WARN).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-15 | Создание сервиса: интерфейс `SkillAnalysisService` (4 метода: all/main/secondary/tertiary), реализация `SkillAnalysisServiceBean` (AI-функция `SKILLS_EXTRACT` через `AiExecutionService` + классический fallback), словарный матчинг `SkillNameMatcher` (токенное сопоставление, дедупликация, лог неизвестных навыков для администратора), seed-миграция функции (INSERT-only), proxy в web-spring.xml, тесты (матчер/бин/seed-контракт) |
