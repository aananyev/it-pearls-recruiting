# Задание: SkillAnalysisService — анализ навыков резюме/вакансии (AI + справочник skilltree)

Дата: 2026-08-15
Автор: Hermes-2 (ветка agent/hermes2-dev)
Адресат: Hermes-1 (проверка PR, merge, deploy)

## Что сделано

Новый middleware-сервис `hunttech_SkillAnalysisService` — анализ текста резюме кандидата
или описания вакансии: нейросеть (через существующий AI Control Plane, без изменений
архитектуры ChatGPT) извлекает названия навыков, сервис сопоставляет их со справочником
`HUNTTECH_SKILL_TREE` и возвращает коллекцию сущностей `SkillTree`.

4 метода (уровни анализа):

1. `analyzeAll(text)` — ВСЕ навыки, упомянутые в тексте;
2. `analyzeMain(text)` — основные/обязательные (в вакансии — требования «необходимо»,
   в резюме — ключевые навыки);
3. `analyzeSecondary(text)` — второстепенные/желательные («желательно» / доп. навыки);
4. `analyzeTertiary(text)` — третьестепенные, если такие есть.

Контракт «неизвестные навыки»: найденные нейросетью навыки, которых нет в справочнике,
пишутся в лог WARN («администратору добавить их в HUNTTECH_SKILL_TREE») — возвращается
только то, что есть в справочнике. Навыки с флагом `notParsing=true` исключаются.

Файлы:

- `modules/global/src/com/company/hunttech/service/SkillAnalysisService.java` — интерфейс;
- `modules/core/src/com/company/hunttech/service/SkillAnalysisServiceBean.java` — реализация
  (AI-функция `SKILLS_EXTRACT` через `AiExecutionService.executeText`; при недоступности AI —
  бесшовный классический fallback — токенный словарный поиск по тексту, анализ не прерывается);
- `modules/core/src/com/company/hunttech/service/SkillNameMatcher.java` — чистый словарный
  матчинг (нормализация, токенизация с сохранением C++/C#/1С, точное совпадение с
  приоритетом, составные названия, дедупликация);
- `modules/core/db/update/postgres/26/260815-1-addSkillAnalysisAiFunction.sql` +
  `modules/core/db/changelog/260815-1-addSkillAnalysisAiFunction.xml` + include в
  `db.changelog-master.xml` — seed AI-функции `SKILLS_EXTRACT` (TEXT_GENERATION,
  INSERT-only, идемпотентный, `ON CONFLICT (CODE) DO NOTHING`);
- `modules/web/src/com/company/hunttech/web-spring.xml` — proxy `hunttech_SkillAnalysisService`;
- `docs/services/SkillAnalysisService.md` — полная документация (обязательный пункт задачи);
- тесты: `SkillNameMatcherTest` (12), `SkillAnalysisServiceBeanTest` (10, стаб
  AiExecutionService — реальные провайдеры не вызываются), `SkillAnalysisAiFunctionSeedContractTest` (5).

## Как проверено

- `:app-core:cleanTest` + три тестовых класса — 27/27 зелёных;
- `:app-web:compileJava` — BUILD SUCCESSFUL;
- `ScreenViewIntegrityTest` — 8/8;
- XML-валидность changelog/web-spring — OK.

Примечание: seed-контрактный тест поймал пропущенное значение `CAPABILITY` в SQL-скрипте
(миграция упала бы на проде) — исправлено, тест зелёный.

## Что ожидается от Hermes-1

- Проверить PR (база master, ветка agent/hermes2-dev, метка WAITING_FOR_HERMES);
- После merge + deploy + restart: применить миграцию (seed `SKILLS_EXTRACT`), в
  «Управление AI» привязать активную корпоративную конфигурацию к функции
  `SKILLS_EXTRACT` (без привязки сервис работает в классическом fallback-режиме);
- Smoke: вызов `analyzeAll` на тексте с известными навыками справочника → возвращаются
  соответствующие `SkillTree`; навык вне справочника → WARN в логе с названием.
