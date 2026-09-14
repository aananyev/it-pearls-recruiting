# Отчет об успешном деплое релиза 0.591 на боевой сервер (Production)

**Дата:** 14 сентября 2026 г.  
**Сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
**Ветка:** `agent/antigravity-dev`  
**Коммит сборки:** `c2861e13`  
**Версия приложения:** `0.591`  
**Ответственный агент:** Руководитель проектов (Project Manager)  

---

## 1. Состав релиза 0.591

1. **Исправление парсинга мульти-слотов Яндекс-Календаря**:
   - Поддержка перечислений дней недели («пн, вт, ср», «понедельник, среда») и форматов дат («14, 15, 16 сентября»).
   - Поддержка диапазонов времени («с 10:00 до 12:00», «с 10 до 12») с автоматической генерацией дискретных слотов фиксированной длительности (`durationMinutes`, по умолчанию 30 мин).
   - Корректная обработка дат с точками («15.09.2026», «15.09»), предотвращающая разрыв строки регулярными выражениями.
   - Дедупликация слотов по ключу `YYYY-MM-DD HH:mm`.

2. **Оркестрация Яндекс-Календаря в чате**:
   - Автоматическая передача флага `attachTelemost=true` при бронировании интервью через оркестратор `AiYandexOrchestrationServiceBean`.
   - Возврат подтверждения со ссылкой на видеоконференцию Яндекс.Телемост.

3. **Верификация и качество**:
   - Контрактный автотест `YandexIntegrationContractTest` успешно пройден.
   - Кодовая база проверена через Alibaba OCR CLI (`ocr review --audience agent`).
   - Успешный локальный прогрев и проверка UI (`http://localhost:8080/hrm/` -> `HTTP 200`).

---

## 2. Процедура деплоя на Production

### Шаг 1. Резервное копирование (Backup)
- Создан полный дамп рабочей базы данных PostgreSQL:
  - Каталог: `/tmp/cuba_deploy_backup_20260914_071105/`
  - Файл дампа: `hunttech.dump` (**865 МБ**)
  - Предыдущие боевые WAR: `hrm.war` (181 МБ), `hrm-core.war` (163 МБ)

### Шаг 2. Загрузка новых пакетов
- WAR-файлы релиза 0.591 собраны локально (`agent-gradle.sh buildWar`) и переданы по rsync:
  - `/var/lib/tomcat9/webapps/hrm.war` (181 МБ)
  - `/var/lib/tomcat9/webapps/hrm-core.war` (163 МБ)

### Шаг 3. Остановка, очистка и перезапуск Tomcat
- Служба `tomcat9` остановлена.
- Очищены каталоги кэша и предыдущей распаковки:
  - `/var/lib/tomcat9/webapps/hrm`
  - `/var/lib/tomcat9/webapps/hrm-core`
  - `/var/lib/tomcat9/work/Catalina/localhost/*`
  - `/var/lib/tomcat9/temp/*`
- Установлены корректные права: `chown tomcat:tomcat`, `chmod 644`.
- Служба `tomcat9` запущена.

### Шаг 4. Инициализация и верификация запуска
- Лог старта `/opt/app_home/logs/app.log`:
  ```
  2026-09-14 07:31:56.558 INFO [main] com.haulmont.cuba.core.sys.AbstractWebAppContextLoader - Initializing 'web' block, servlet context path: /hrm
  2026-09-14 07:32:04.196 INFO [main] com.haulmont.cuba.core.sys.AbstractWebAppContextLoader - AppContext started
  ```
- Сервисы `hunttech_YandexIntegrationService` и `hunttech_AiYandexOrchestrationService` успешно зарегистрированы и экспортированы.

---

## 3. Результаты проверки доступности

- Локально на боевом сервере:
  - `http://localhost:8080/hrm/` $\rightarrow$ **`HTTP 200 OK`** (JSESSIONID cookie set, text/html)
- По внешним адресам:
  - `http://92.63.101.170:8080/hrm/` $\rightarrow$ **`HTTP 200 OK`**
  - `http://hr.hunttech.ru:8080/hrm/` $\rightarrow$ **`HTTP 200 OK`**

**Итог:** Деплой релиза 0.591 на боевой сервер успешно завершен. Приложение функционирует в штатном режиме.
