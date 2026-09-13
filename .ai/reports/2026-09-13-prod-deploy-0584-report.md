# Отчёт о безопасном деплое релиза 0.584 на Production (`hr.hunttech.ru`)

> **Дата деплоя:** 13 сентября 2026 г., 22:25 MSK  
> **Целевой сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия релиза:** `0.584` (ветка `agent/antigravity-dev`, коммит `f517ea10`)  
> **Статус:** ✅ **УСПЕШНО РАЗВЕРНУТО И ВЕРИФИЦИРОВАНО (HTTP 200 OK)**  
> **Каталог полного бэкапа:** `/tmp/cuba_deploy_backup_20260913_211523` (PostgreSQL dump `hunttech.dump` 865 МБ + предыдущие боевые WAR)  

---

## 1. Состав выкаченных изменений релиза 0.584

### 1.1. Интеграция и оркестрация Яндекс 360 в локальном LLM-чате
1. **Перехват намерений создания событий и бронирования**:
   - В `LlmChatServiceBean` и `HermesChatServiceBean` добавлена пре-проверка `isMeetingBookingIntent`: перед вызовом внешней нейросети система распознает намерение создать встречу или событие в календаре Яндекс 360.
   - Прямой вызов `AiYandexOrchestrationService.executeMeetingBooking`: событие создается через протокол CalDAV (RFC 5545) напрямую в календаре пользователя или календаре заказчика.
   - Исключены отказы нейросети вида *«Я не имею доступа к вашему календарю...»*.

2. **Интеллектуальный синтаксический анализ русского текста**:
   - Форматы времени: `12-00`, `12.00`, `12:00` с защитой от коллизий с датами (`(?<![\d.\-:])` и `(?![:.\-]?\d)`).
   - Длительность: `длительностью 1 час`, `на 30 минут` с фильтрацией времени суток и выражений вида «на 5 человек».
   - Часовые пояса: поддержка Саратова (`Europe/Saratov`, UTC+4), Самары (`Europe/Samara`, UTC+4), Москвы (`Europe/Moscow`, UTC+3) и индивидуального пояса пользователя.
   - Тема события: извлечение из кавычек `«...»`, `"... "` и ключевых слов `тема:` / `тему:` с корректным отсечением служебных предлогов.
   - Личные задачи без кандидатов: автоматическое отключение флага Телемоста.

3. **Безопасность квот и транзакций**:
   - Сообщение ассистента с результатом создания события/ошибкой CalDAV гарантированно сохраняется в БД.
   - При сбое подключения CalDAV зарезервированные токены корректно освобождаются через `releaseFailedReservation`.

### 1.2. Промпты ИИ и регламент вакансий
1. **Калиброванные системные промпты вакансий**:
   - `VACANCY_SMART_PARSE_JSON` (версия 5): поддержка 4 артефактов стандарта HuntTech (стандартизированное описание, чек-лист must-have, карта поиска, план интервью).
   - `STANDARDIZE_VACANCY` (версия 2): сокрытие ставок заказчика, структура по 14 пунктам.
   - `LLM_CHAT` (версия 6): добавлены строгие инструкции по экосистеме Яндекс 360 (Календарь, Телемост, Вики).

---

## 2. Протокол безопасности и этапы деплоя

| Этап | Действие | Статус | Детализация |
|---|---|---|---|
| **1** | Локальная сборка WAR | ✅ PASS | `hrm.war` (180 МБ), `hrm-core.war` (163 МБ) собраны за 57 секунд |
| **2** | **Полный серверный бэкап** | ✅ PASS | `/tmp/cuba_deploy_backup_20260913_211523`: `hunttech.dump` (865 МБ) + `wars/` |
| **3** | Передача WAR на сервер | ✅ PASS | `rsync -avP` в `/tmp/cuba_new_deploy_0584/` (359 МБ передано) |
| **4** | **Миграции БД и системных данных** | ✅ PASS | Применены 6 SQL-скриптов миграций структуры и промптов ИИ (версии `LLM_CHAT` v6, `VACANCY_SMART_PARSE_JSON` v5, `STANDARDIZE_VACANCY` v2) |
| **5** | Остановка и очистка кэшей | ✅ PASS | `systemctl stop tomcat9`, удаление work/temp/старых распаковок |
| **6** | Развертывание и права | ✅ PASS | Копирование WAR в `/var/lib/tomcat9/webapps/`, владелец `tomcat:tomcat`, права 644 |
| **7** | Запуск и прогрев (warmup) | ✅ PASS | `systemctl start tomcat9`, чистый старт, `Metadata initialized in 393 ms`, `AppContext started` |

---

## 3. Свидетельства верификации (Evidence)

1. **Доступность приложения по HTTP:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://hr.hunttech.ru:8080/hrm/
   # HTTP 200
   ```
2. **Доступность по прямому IP адресу:**
   ```bash
   curl --noproxy '*' -I http://92.63.101.170:8080/hrm/
   # HTTP/1.1 200, Set-Cookie: JSESSIONID=53CB51C8FC72D89D433314997718C15E; Path=/hrm; HttpOnly
   ```
3. **Стили и ресурсы темы:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://hr.hunttech.ru:8080/hrm/VAADIN/themes/hunttech-modern/styles.css
   # HTTP 200
   ```
4. **Конфигурации в боевой базе (`hunttech`):**
   ```sql
   SELECT code, configuration_version, update_ts FROM hunttech_ai_function_configuration 
   WHERE code IN ('LLM_CHAT', 'VACANCY_SMART_PARSE_JSON', 'STANDARDIZE_VACANCY') ORDER BY code;
   -- LLM_CHAT                 | 6 | 2026-09-13 21:23:32.595622
   -- STANDARDIZE_VACANCY      | 2 | ...
   -- VACANCY_SMART_PARSE_JSON | 5 | ...
   ```
5. **Логи Tomcat9 (`/opt/app_home/logs/app.log`):**
   - `RemotingProxyBeanCreator - hunttech_YandexIntegrationService, hunttech_AiYandexOrchestrationService`
   - `Metadata initialized in 393 ms`
   - `AppContext started`
