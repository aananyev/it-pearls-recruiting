# Отчёт о безопасном деплое релиза 0.578 на Production (`hr.hunttech.ru`)

> **Дата деплоя:** 13 сентября 2026 г., 17:52 MSK  
> **Целевой сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия релиза:** `0.578` (ветка `agent/antigravity-dev`, коммит `cd8c510d`)  
> **Статус:** ✅ **УСПЕШНО РАЗВЕРНУТО И ВЕРИФИЦИРОВАНО (HTTP 200 OK)**  
> **Каталог полного бэкапа:** `/tmp/cuba_deploy_backup_20260913_164019` (PostgreSQL dump `hunttech.dump` 865 МБ + предыдущие боевые WAR)  

---

## 1. Состав выкаченных изменений релиза 0.578

### 1.1. Универсальная настройка сервисов Яндекс 360
1. **Пользовательский интерфейс**:
   - В экран персональных настроек (`ExtSettingsWindow`) добавлена вкладка **«Сервисы Яндекс 360»** с боковой навигационной кнопкой `navTabYandex`.
   - В карточку редактирования пользователя администратором (`ExtUserEdit`) добавлена вкладка `yandexTab` с кнопкой `yandexTabNav`.
   - 4 блока управления сервисами экосистемы:
     1. **«Авторизация и аккаунт»** (Email, OAuth токен, Refresh токен, кнопка «Проверить доступ»).
     2. **«Яндекс.Календарь (CalDAV)»** (URL CalDAV, часовой пояс по умолчанию `Europe/Saratov`, названия личного календаря и календаря «Hunttech у заказчика», кнопка «Найти календари»).
     3. **«Яндекс.Телемост»** (API URL, флаги автозаписи и AI-конспекта, уровень доступа, кнопка «Тестовый звонок»).
     4. **«Яндекс.Вики»** (URL базы знаний, Org ID, Collab ID, кнопка «Проверить Вики»).
   - Индикатор статуса подключения с подсветкой (`bold friendly` / `bold edit-help`).

2. **Java-сервисы и безопасность**:
   - `YandexIntegrationService`: обнаружение календарей по протоколу CalDAV (`PROPFIND`), генерация iCalendar RFC 5545 с таймзонами, рассылка приглашений через CalDAV Outbox, создание комнат видеозвонков Телемост (`POST /conferences`).
   - Безопасная поддержка нестандартных CalDAV-методов (`PROPFIND`, `REPORT`, `MKCALENDAR`) через reflection fallback для JDK `HttpURLConnection`.
   - Шифрование OAuth токенов через системный `AiSecretService`.
   - Регистрация сущности `UserYandexConfiguration` в `modules/global/src/com/company/hunttech/persistence.xml` и создание строгого контрактного автотеста `EntityPersistenceRegistrationContractTest`.

3. **Сервис AI-оркестрации (`AiYandexOrchestrationService`)**:
   - Распознавание намерений пользователя на русском языке:
     - Различение личного календаря (`PERSONAL`) при запросах *«в моем календаре...»*.
     - Корпоративного календаря заказчика (`CLIENT_INTERVIEW`, «Hunttech у заказчика») при запросах *«в календаре собеседования с заказчиком...»*.
   - Интеллектуальный поиск кандидатов в базе HRM по ФИО, расчет даты/времени, автогенерация Телемост-ссылки и сохранение встречи в истории взаимодействий `hunttech_IteractionList`.
   - Интеграция в локальный LLM-чат (`LlmChatServiceBean`) и чат агента Hermes (`HermesChatServiceBean`).

4. **Настройки Яндекс 360 пользователя `alan`**:
   - Запись конфигурации успешно перенесена в боевую таблицу `HUNTTECH_USER_YANDEX_CONFIG`:
     - Email: `alan@hunttech.ru`
     - Календарь личный: `Мои события` (`/calendars/alan%40hunttech.ru/events-32367521/`)
     - Календарь заказчика: `Hunttech у заказчика` (`/calendars/alan%40hunttech.ru/events-34179601/`)
     - Часовой пояс: `Europe/Saratov`
     - Зашифрованные OAuth и Refresh токены проверены и сохранены
     - Статус: `calendar_connected = true`

---

## 2. Протокол безопасности и этапы деплоя

| Этап | Действие | Статус | Детализация |
|---|---|---|---|
| **1** | Локальная сборка WAR | ✅ PASS | `hrm.war` (180 МБ), `hrm-core.war` (163 МБ) собраны за 1м 4с |
| **2** | **Полный серверный бэкап** | ✅ PASS | `/tmp/cuba_deploy_backup_20260913_164019`: `hunttech.dump` (865 МБ) + `wars/` (345 МБ) |
| **3** | Передача WAR на сервер | ✅ PASS | `rsync -avP` в `/tmp/cuba_new_deploy_0578/` (360 МБ передано) |
| **4** | **Миграции БД и данные пользователя `alan`** | ✅ PASS | Создана таблица `HUNTTECH_USER_YANDEX_CONFIG`, FK и индексы, зафиксированы настройки пользователя `alan` |
| **5** | Остановка и очистка кэшей | ✅ PASS | `systemctl stop tomcat9`, удаление work/temp/старых распаковок |
| **6** | Развертывание и права | ✅ PASS | Копирование WAR в `/var/lib/tomcat9/webapps/`, владелец `tomcat:tomcat`, права 644 |
| **7** | Запуск и прогрев (warmup) | ✅ PASS | `systemctl start tomcat9`, чистый старт, `RemotingServlet - Completed initialization in 410 ms` |

---

## 3. Свидетельства верификации (Evidence)

1. **Доступность приложения по HTTP:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://hr.hunttech.ru:8080/hrm/
   # HTTP 200
   ```
2. **Доступность по прямому IP адресу:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://92.63.101.170:8080/hrm/
   # HTTP 200
   ```
3. **Стили и ресурсы темы:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://hr.hunttech.ru:8080/hrm/VAADIN/themes/hunttech-modern/styles.css
   # HTTP 200
   ```
4. **Проверка настроек пользователя `alan` в боевой базе:**
   ```sql
   SELECT u.login, c.account_email, c.personal_calendar_name, c.client_calendar_name, c.default_time_zone, c.calendar_connected
   FROM hunttech_user_yandex_config c JOIN sec_user u ON u.id = c.user_id WHERE u.login = 'alan';
   -- alan | alan@hunttech.ru | Мои события | Hunttech у заказчика | Europe/Saratov | t
   ```
5. **Логи Tomcat9 (`/opt/app_home/logs/app.log`):**
   - `RemotingServlet - Completed initialization in 410 ms`
   - `AppContext started` для `hrm` и `hrm-core`
   - Ошибок инициализации метаданных нет.
