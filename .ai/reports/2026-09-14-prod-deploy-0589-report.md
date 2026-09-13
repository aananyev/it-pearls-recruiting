# Отчёт о безопасном деплое релиза 0.589 на Production (`hr.hunttech.ru`)

> **Дата деплоя:** 14 сентября 2026 г., 00:30 MSK  
> **Целевой сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия релиза:** `0.589` (ветка `agent/antigravity-dev`, коммит `1306e245`)  
> **Статус:** ✅ **УСПЕШНО РАЗВЕРНУТО И ВЕРИФИЦИРОВАНО (HTTP 200 OK)**  
> **Каталог полного бэкапа:** `/tmp/cuba_deploy_backup_20260913_231712` (PostgreSQL dump `hunttech.dump` 865 МБ + предыдущие боевые WAR)  

---

## 1. Состав выкаченных изменений релиза 0.589

### 1.1. Исправление сохранения постоянного лимита токенов в `ExtUserEdit`
1. **Root Cause Fix**:
   - Устранено затирание введенного значения квоты старыми данными из БД при срабатывании `userDs.addItemChangeListener` во время коммита формы.
   - Введены `pendingQuotaToSave` и флаг `pendingQuotaChanged` с сохранением через `userAiQuotaService.setMonthlyQuota(...)`.
   - Логика синхронизирована в обоих контроллерах: `ExtUserEditor.java` и `ExtUserEdit.java`.

### 1.2. Разовое пополнение токенов на текущий месяц («Добавить токенов»)
1. **Интерфейс карточки пользователя**:
   - Рядом с полем `Выделено токенов` добавлена кнопка `[Добавить токенов]`.
   - Модальный `InputDialog` с валидацией строго целого числа > 0.
   - При начислении постоянный месячный лимит не изменяется, а в поле «Остаток токенов» отображается пометка о наличии активных бонусов.
2. **Специализированный Java-сервис `UserAiQuotaService`**:
   - Добавлен потокобезопасный метод `addMonthlyBonusTokens(UUID userId, int amount, String reason)` с пессимистической блокировкой `LockModeType.PESSIMISTIC_WRITE`.
   - Централизованный расчет доступных токенов: `available = allocatedTokens + extraTokens - used`.
   - Автоматическое сгорание бонусов в новом месяце без отдельных шедулеров (за счет структуры периодов `LlmChatQuotaPeriod`).
3. **Реальный LLM-пайплайн**:
   - В `LlmChatServiceBean.reserveQuota` добавлен учет `extraTokens`, что разблокирует выполнение запросов при исчерпании базового лимита.
   - Все вычисления выполнены через `long` с защитой от целочисленного переполнения.

---

## 2. Протокол безопасности и этапы деплоя

| Этап | Действие | Статус | Детализация |
|---|---|---|---|
| **1** | Локальная сборка WAR | ✅ PASS | `hrm.war` (180 МБ), `hrm-core.war` (163 МБ) собраны за 1м 2с |
| **2** | **Полный серверный бэкап** | ✅ PASS | `/tmp/cuba_deploy_backup_20260913_231712`: `hunttech.dump` (865 МБ) + `wars/` |
| **3** | Передача WAR на сервер | ✅ PASS | `rsync -avP` в `/tmp/cuba_new_deploy_0589/` (343 МБ передано) |
| **4** | **Миграция БД** | ✅ PASS | В таблицу `HUNTTECH_LLM_CHAT_QUOTA_PERIOD` добавлена колонка `EXTRA_TOKENS integer DEFAULT 0 NOT NULL` |
| **5** | Остановка и очистка кэшей | ✅ PASS | `systemctl stop tomcat9`, удаление work/temp/старых распаковок |
| **6** | Развертывание и права | ✅ PASS | Копирование WAR в `/var/lib/tomcat9/webapps/`, владелец `tomcat:tomcat`, права 644 |
| **7** | Запуск и прогрев (warmup) | ✅ PASS | `systemctl start tomcat9`, чистый старт, `Metadata initialized in 373 ms`, `AppContext started` |

---

## 3. Свидетельства верификации (Evidence)

1. **Доступность приложения по HTTP:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://hr.hunttech.ru:8080/hrm/
   # HTTP 200
   ```
2. **Доступность по прямому IP адресу и выдача JSESSIONID:**
   ```bash
   curl --noproxy '*' -I http://92.63.101.170:8080/hrm/
   # HTTP/1.1 200, Set-Cookie: JSESSIONID=8F50A4AE53DDAC1FAAAE70108998439C; Path=/hrm; HttpOnly
   ```
3. **Стили и ресурсы темы:**
   ```bash
   curl --noproxy '*' -s -o /dev/null -w "%{http_code}\n" http://hr.hunttech.ru:8080/hrm/VAADIN/themes/hunttech-modern/styles.css
   # HTTP 200
   ```
4. **Проверка структуры боевой таблицы в PostgreSQL (`hunttech`):**
   ```sql
   \d hunttech_llm_chat_quota_period
   -- extra_tokens | integer | not null | 0
   ```
5. **Логи Tomcat9 (`/opt/app_home/logs/app.log`):**
   - `RemoteProxyBeanCreator - Configuring remote proxy beans: [..., hunttech_UserAiQuotaService, ...]`
   - `Metadata initialized in 373 ms`
   - `AppContext started`
