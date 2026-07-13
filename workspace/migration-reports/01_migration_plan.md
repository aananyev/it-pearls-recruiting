# 01_migration_plan.md — План миграции CUBA 7.3 → Jmix 2.8 LTS → Jmix 3.0

> Дата: 2026-07-09
> Проект: HuntTech Recruiting (HRM-система)
> Исходный: CUBA 7.3-SNAPSHOT (Java 11, PostgreSQL 11)
> Целевой: Jmix 2.8.2 LTS → Jmix 3.0.0

---

## 1. Общая стратегия

### Принципы
1. Исходный CUBA-проект — **read-only**. Никаких изменений.
2. Исходная БД — **read-only**. Никаких прямых модификаций.
3. Вся миграция — в новый Jmix-проект и новую Jmix-БД.
4. Миграция данных — только через staging-копию.
5. Каждый шаг воспроизводим и имеет откат.
6. После каждого крупного шага — git commit, сборка, отчёт.

### Файловая структура

```
workspace/
├── source-projects/hunttech_recruiting/       ← read-only (symlink / копия)
├── target-projects/hunttech_recruiting-jmix/  ← новый Jmix-проект (git)
├── migration-reports/                         ← отчёты
│   ├── 00_initial_audit.md                    ← готов
│   ├── 01_migration_plan.md                   ← текущий файл
│   ├── 02_version_decision.md                 ← готов
│   ├── 03_code_migration_report.md
│   ├── 04_db_migration_plan.md
│   ├── 05_table_mapping.md
│   ├── 06_cutover_plan.md
│   ├── 07_validation_report.md
│   ├── 08_known_issues.md
│   └── 09_next_steps.md
├── db-migration/                              ← SQL-скрипты
│   ├── 00_create_target_tablespace.sql
│   ├── 01_create_target_database.sql
│   ├── 02_apply_jmix_schema.sh
│   ├── 03_precheck_source_db.sql
│   ├── 04_migrate_reference_data.sql
│   ├── 05_migrate_business_data.sql
│   ├── 06_migrate_security_data.sql
│   ├── 07_validate_counts.sql
│   ├── 08_validate_integrity.sql
│   ├── 09_cutover_readiness_check.sql
│   ├── rollback_notes.md
│   ├── table_mapping.md
│   ├── data_migration_plan.md
│   └── cutover_plan.md
└── scripts/                                  ← shell-скрипты
    ├── setup-local-dev.sh
    ├── build-and-run.sh
    ├── run-migration.sh
    └── cutover-prod.sh
```

---

## 2. Поэтапный план

### Фаза 0: Подготовка окружения

| # | Действие | Ожидаемый результат |
|---|---|---|
| 0.1 | Подготовить Java 17 (Corretto 17+) | `java -version` → 17+ |
| 0.2 | Установить PostgreSQL 15/16 (или использовать PG11) | `psql --version` |
| 0.3 | Установить Jmix Studio / IntelliJ IDEA с Jmix plugin | — |
| 0.4 | Установить SDKMAN и Gradle 8.x | `gradle --version` → 8.x |
| 0.5 | Создать каталоги: target-projects, db-migration, scripts | — |
| 0.6 | Сделать symlink/copy source-проекта в source-projects/ | — |
| 0.7 | Backup локальной БД hunttech | `pg_dump` → `.sql` |
| 0.8 | Backup удалённой БД itpearls (через SSH) | `pg_dump` → `.sql` |
| 0.9 | Создать staging-БД для источника | `hunttech_staging` |

### Фаза 1: Создание Jmix-проекта

| # | Действие | Ожидаемый результат |
|---|---|---|
| 1.1 | Создать Jmix-проект через Jmix Studio или CLI | Проект компилируется |
| 1.2 | Base package: `com.company.hunttech` | Соответствует CUBA |
| 1.3 | Module: application (все-в-одном) или multi-module | Рекомендуется multi-module |
| 1.4 | DB: PostgreSQL, Liquibase changelog | Генерация схемы |
| 1.5 | Git init + baseline commit | Чистый git history |

**Важно:** Если CLI/Jmix Studio недоступен, создать через Spring Initializr + Jmix BOM вручную.

### Фаза 2: Перенос сущностей (Wave 1)

| # | Действие | Результат |
|---|---|---|
| 2.1 | Создать entities аналоги CUBA-сущностям | 58 entity-классов |
| 2.2 | Конвертировать `javax.persistence` → `jakarta.persistence` | — |
| 2.3 | Конвертировать `com.haulmont.cuba.core.entity.*` → `io.jmix.core.entity.*` | — |
| 2.4 | @NamePattern → @InstanceName + @DependsOnProperties | — |
| 2.5 | CUBA Views → Jmix Fetch Plans | 4 XML-файла |
| 2.6 | Enums: перенести как Jmix enums | 12 enum-классов |
| 2.7 | ExtUser → Jmix User + кастомная сущность | Сложная миграция |
| 2.8 | SomeFiles hierarchy → Jmix FileReference | — |
| 2.9 | **Сборка** | `./gradlew build` |
| 2.10 | **Git commit** | `feat: entities migration wave 1` |

### Фаза 3: Перенос бизнес-логики (Wave 2)

| # | Действие | Результат |
|---|---|---|
| 3.1 | Перенести Service интерфейсы → Jmix Services | 18+ интерфейсов |
| 3.2 | Перенести Service реализации → Spring @Service | — |
| 3.3 | Заменить `AppBeans.get()` на DI (`@Autowired`, `@Inject`) | — |
| 3.4 | Перенести Entity Listeners | 2 слушателя |
| 3.5 | Перенести JMX Beans (OpenPositionApproval) | 1 бин |
| 3.6 | Перенести Events (BeanNotificationEvent, UiNotificationEvent) | 2 события |
| 3.7 | Перенести AI-интеграцию (AIProvider, MiMo, OpenAI) | 4 класса |
| 3.8 | Перенести Telegram Bot | 15+ файлов |
| 3.9 | Перенести Email-сервисы | — |
| 3.10 | Перенести интеграции (HeadHunter, PDFBox, Thumbnailator) | — |
| 3.11 | **Сборка** | `./gradlew build` |
| 3.12 | **Git commit** | `feat: business logic wave 2` |

### Фаза 4: Перенос UI (Wave 3) — САМАЯ ТРУДОЁМКАЯ

| # | Действие | Результат |
|---|---|---|
| 4.1 | Настроить Jmix Flow UI модуль | — |
| 4.2 | Перенести LoginScreen | 1 экран |
| 4.3 | Перенести MainScreen (ExtMainScreen) | 1 экран |
| 4.4 | Перенести Browse-экраны (→ StandardListView) | ~60 экранов |
| 4.5 | Перенести Edit-экраны (→ StandardDetailView) | ~60 экранов |
| 4.6 | Перенести Lookup-экраны | ~5 экранов |
| 4.7 | Перенести Fragments | ~10 фрагментов |
| 4.8 | Перенести Кастомные компоненты (5 шт.) | Портирование на Vaadin 24 |
| 4.9 | Перенести Dashboard виджеты | ~16 виджетов |
| 4.10 | Адаптировать темы (hover → Jmix) | SCSS миграция |
| 4.11 | **Сборка** | `./gradlew build` |
| 4.12 | **Git commit** | `feat: UI migration wave 3` |

### Фаза 5: Перенос безопасности (Wave 4)

| # | Действие | Результат |
|---|---|---|
| 5.1 | Создать Resource Roles в Jmix | Аналог CUBA roles |
| 5.2 | Создать Row-Level Roles | Аналог CUBA constraints |
| 5.3 | Создать кастомные экраны для ExtUser редактирования | — |
| 5.4 | Настроить anonymous session | — |
| 5.5 | **Сборка** | `./gradlew build` |
| 5.6 | **Git commit** | `feat: security migration wave 4` |

### Фаза 6: Add-ons и интеграции (Wave 5)

| # | Действие | Результат |
|---|---|---|
| 6.1 | Настроить Jmix Reports (аналог CUBA Reports) | — |
| 6.2 | Настроить Jmix Charts (аналог CUBA Charts) | — |
| 6.3 | Настроить Jmix BPM (аналог CUBA BPM) | — |
| 6.4 | Настроить Jmix Dashboards (с портированием виджетов) | — |
| 6.5 | Настроить Jmix Email (аналог CUBA Email) | — |
| 6.6 | Настроить Jmix File Storage | — |
| 6.7 | Настроить Jmix FTS (Full-Text Search) | — |
| 6.8 | **Сборка** | `./gradlew build` |
| 6.9 | **Git commit** | `feat: addons migration wave 5` |

### Фаза 7: Миграция БД

| # | Действие | Результат |
|---|---|---|
| 7.1 | Backup исходной БД → staging | — |
| 7.2 | Создать tablespace + новую Jmix-БД | — |
| 7.3 | Применить Liquibase changelog (схема Jmix) | Таблицы созданы |
| 7.4 | Проверить pre-checks (03_precheck_source_db.sql) | OK |
| 7.5 | Перенести справочники (04_migrate_reference_data.sql) | — |
| 7.6 | Перенести бизнес-данные (05_migrate_business_data.sql) | — |
| 7.7 | Перенести security (06_migrate_security_data.sql) | — |
| 7.8 | Проверить counts (07_validate_counts.sql) | OK |
| 7.9 | Проверить integrity (08_validate_integrity.sql) | OK |
| 7.10 | **Git commit** | `feat: database migration wave 7` |

### Фаза 8: Тестирование и валидация

| # | Действие | Результат |
|---|---|---|
| 8.1 | Сборка + запуск приложения на новой БД | 200 OK |
| 8.2 | Проверка логина/аутентификации | — |
| 8.3 | Проверка открытия key screens | — |
| 8.4 | Проверка CRUD операций | — |
| 8.5 | Проверка отчётов | — |
| 8.6 | Проверка Telegram Bot | — |
| 8.7 | Проверка email отправки | — |
| 8.8 | Проверка FTS поиска | — |
| 8.9 | Проверка BPM процессов | — |
| 8.10 | Проверка dashboard виджетов | — |
| 8.11 | **Заполнить отчёт 07_validation_report.md** | — |

### Фаза 9: Upgrade Jmix 2.8 → Jmix 3.0

| # | Действие | Результат |
|---|---|---|
| 9.1 | Прочитать Jmix upgrade guide | — |
| 9.2 | Обновить Jmix BOM 2.8 → 3.0 | — |
| 9.3 | `./gradlew build` и исправить ошибки | — |
| 9.4 | Проверить deprecated API | — |
| 9.5 | Полное регрессионное тестирование | — |
| 9.6 | **Git commit** | `chore: upgrade jmix 2.8 → 3.0` |

### Фаза 10: Cutover на production

| # | Действие | Ожидаемый результат |
|---|---|---|
| 10.1 | Развернуть Jmix рядом со старым CUBA | — |
| 10.2 | Сделать финальный backup prod БД | — |
| 10.3 | Восстановить в staging | — |
| 10.4 | Запустить миграцию данных | — |
| 10.5 | Запустить валидацию | — |
| 10.6 | Остановить старый CUBA | — |
| 10.7 | Переключить reverse proxy/DNS на Jmix | — |
| 10.8 | Smoke-тесты | — |
| 10.9 | **Финальный отчёт** | — |

---

## 3. Что переносится автоматически

- **Entity-классы** — Jmix Studio / IDE миграция CUBA→Jmix (частично)
- **Views.xml → fetch-plans.xml** — инструмент миграции Jmix
- **Базовые security roles** — автоматически при миграции схемы
- **Liquibase changelog** — генерация из entities
- **menu.xml → Jmix menu** — Jmix Studio / CLI

## 4. Что требует ручной проверки

- **ExtUser extends User** — сложная миграция, т.к. модель User в Jmix иная
- **Кастомные Vaadin-компоненты** — портирование на Vaadin 24
- **Dashboard виджеты** — переработка под Jmix Dashboard
- **Telegram Bot интеграция** — проверка совместимости с Spring Boot 3
- **AI-интеграция** — проверка совместимости
- **BPM процессы** — полная переработка
- **FTS настройки** — Elasticsearch конфигурация
- **CUBA-specific persistence** — `CustomDbTypeConverter`, `CustomDbmsFeatures`, `CustomSequenceSupport`

## 5. Что временно отключается

- **BPM модуль** — если Jmix BPM несовместим, временное отключение
- **FTS** — если Elasticsearch не развёрнут локально
- **Dashboard** — пока не портированы виджеты

## 6. Риски

| # | Риск | Вероятность | Влияние | Митигация |
|---|---|---|---|---|
| 1 | CUBA 7.3-SNAPSHOT несовместим | Средняя | Высокое | Тест на отдельном проекте |
| 2 | Объём UI → время миграции | Высокая | Среднее | Приоритезация экранов |
| 3 | Telegram Bot конфликт slf4j | Средняя | Среднее | Resolution strategy |
| 4 | BPM миграция | Средняя | Высокое | Возможен skip BPM |
| 5 | Data loss при миграции БД | Низкая | Критическое | Staging + validation |
| 6 | Отсутствие Jmix Studio | Низкая | Среднее | Ручная миграция |

## 7. Контрольные точки

| Точка | Критерий | Проход |
|---|---|---|
| **CP1** | Аудит завершён, план утверждён | ☐ |
| **CP2** | Jmix-проект создан, компилируется | ☐ |
| **CP3** | Все сущности + views перенесены | ☐ |
| **CP4** | Бизнес-логика скомпилирована | ☐ |
| **CP5** | UI работает (ключевые экраны) | ☐ |
| **CP6** | Безопасность настроена | ☐ |
| **CP7** | Add-ons работают | ☐ |
| **CP8** | Миграция БД успешна (counts + integrity) | ☐ |
| **CP9** | Приложение запущено на Jmix 2.8 | ☐ |
| **CP10** | Upgrade до Jmix 3.0 завершён | ☐ |
| **CP11** | Production cutover выполнен | ☐ |

## 8. Команды сборки и запуска

```bash
# Сборка Jmix-проекта
cd workspace/target-projects/hunttech_recruiting-jmix
./gradlew build

# Запуск (через Spring Boot)
./gradlew bootRun

# Запуск (через Jmix)
./gradlew :application:bootRun

# Применение Liquibase
./gradlew updateDatabase
```

## 9. План отката

На каждом этапе:
1. `git checkout` предыдущего коммита Jmix-проекта
2. При миграции БД: восстановить staging из backup
3. При cutover: вернуть reverse proxy на старый CUBA

**Никогда не удалять старый CUBA-проект и старую БД** — это основной rollback-механизм.

## 10. План боевого cutover

См. `workspace/db-migration/cutover_plan.md` (будет создан на фазе подготовки БД).
