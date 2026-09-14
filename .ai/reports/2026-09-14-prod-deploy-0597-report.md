# Отчет об успешном деплое релиза 0.597 на боевой сервер (Production)

**Дата:** 14 сентября 2026 г.  
**Сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
**Ветка:** `agent/antigravity-dev`  
**Коммит сборки:** `1b81d29e`  
**Версия приложения:** `0.597`  
**Ответственный агент:** Руководитель проектов (Project Manager)  

---

## 1. Состав релиза 0.597

1. **Редизайн и эргономическая переработка формы взаимодействия `IteractionListEdit` (`hunttech_IteractionList.edit`)**:
   - **Горизонтальный ряд даты и календаря (`actionDateCalendarRow`)**:
     - Поле даты `addDate` зафиксировано в компактной ширине **260px** вместо растягивания на всю ширину формы.
     - Плашка календаря `calendarBox` с чекбоксом `addToCalendarCheckBox` и списком календарей `calendarLookupField` (**320px**, стиль `iteraction-calendar-lookup`) скомпонована в единую компактную строку `calendarInlineBox` рядом с полем даты.
     - В контроллере `IteractionListEdit.java` реализовано синхронное управление видимостью `actionDateCalendarRow.setVisible(visible)` в методе `updateCalendarVisibility()`, исключающее пустоты при скрытой дате.
   - **Ограничение коротких полей ввода и кнопок**:
     - Кнопка вызова `buttonCallAction`: ширина изменена со 100% на `AUTO` (ограничена диапазоном 200–340px, класс `iteraction-call-action-btn`, выравнивание `MIDDLE_LEFT`).
     - Поле `addInteger`: ширина сокращена со 100% до **220px**.
     - Поле `addString`: максимальная ширина ограничена **640px**.
     - Поле рейтинга `ratingField`: ширина зафиксирована на **260px**.
     - Поле способа связи `communicationMethodField`: ширина зафиксирована на **340px**.
   - **Защита сайдбара и контрактов**:
     - Левый сайдбар (312px) сохранен абсолютно неизменным (строго по требованию).
     - Сохранен строгий контракт стиля `edit-form-control` (ровно 7 полей для прохождения канонических тестов).
   - **Синхронизация SCSS**:
     - Обновлен файл `iteraction-list-visual-alignment.scss` во всех 7 темах (`halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`).

2. **Персонализированный доступ к ИИ для всех активных пользователей**:
   - Настройка прямого доступа к DeepSeek (`deepseek-v4-flash`, приоритет 10).
   - Резервный OpenRouter (`nvidia/nemotron-3-ultra-550b-a55b:free`, приоритет 5).
   - Создание профилей с согласием на fallback и квотой в 110 000 токенов.

3. **Оптимизация закрытия вакансий (`OpenPositionBrowse`)**:
   - Устранение тройной перезагрузки загрузчика `openPositionsDl`.
   - Изолированный пул потоков для отправки оповещений в Telegram и email.

---

## 2. Процедура деплоя на Production

### Шаг 1. Локальная сборка WAR-пакетов
- Выполнена сборка WAR-пакетов релиза 0.597 через обёртку `agent-gradle.sh buildWar`:
  - `hrm-core.war` (163 МБ)
  - `hrm.war` (182 МБ)
- Пакеты верифицированы и скопированы в локальный каталог `../tomcat_wars/`.

### Шаг 2. Автоматическое резервное копирование на сервере (Safety Gate)
- Скрипт `deploy-prod.sh` создал полный дамп рабочей базы данных PostgreSQL и копию текущих WAR:
  - Каталог бэкапа: `/tmp/cuba_deploy_backup_20260914_125048/`
  - Файл дампа: `hunttech.dump` (**865 МБ**)
  - Предыдущие боевые WAR сохранены в `/tmp/cuba_deploy_backup_20260914_125048/wars/`

### Шаг 3. Остановка, очистка кэшей и загрузка обновлений
- Служба `tomcat9` остановлена.
- Очищены каталоги кэша и предыдущей распаковки:
  - `/var/lib/tomcat9/webapps/hrm`
  - `/var/lib/tomcat9/webapps/hrm-core`
  - `/var/lib/tomcat9/work/Catalina/localhost/*`
  - `/var/lib/tomcat9/temp/*`
- Выполнена передача обновленных WAR через SSH (`pv + rsync`):
  - `hrm-core.war` (171 202 385 байт)
  - `hrm.war` (190 885 490 байт)
- Выставлены права владельца `tomcat:tomcat` и права доступа `644`.

### Шаг 4. Обновление структуры базы данных
- Выполнена проверка скриптов миграции (`assembleDbScripts`).
- Запущен `CUBA updateDb` через защищенный SSH-туннель `localhost:15432` $\rightarrow$ `hr.hunttech.ru:5432`.
- Структура базы данных успешно синхронизирована.

### Шаг 5. Запуск Tomcat и прогрев
- Служба `tomcat9` запущена.
- Логи развертывания `/opt/app_home/logs/app.log`:
  ```text
  2026-09-14 11:59:42.771 INFO  [main] com.haulmont.cuba.core.sys.AbstractWebAppContextLoader - Initializing 'web' block, servlet context path: /hrm
  2026-09-14 12:01:30.671 INFO  [main] com.haulmont.cuba.core.sys.AbstractWebAppContextLoader - AppContext started
  2026-09-14 12:01:36.070 DEBUG [http-nio-0.0.0.0-8080-exec-2] com.vaadin.server.VaadinServlet - Accepted access to a JAR entry using a class loader: jar:file:/var/lib/tomcat9/webapps/hrm/WEB-INF/lib/app-web-toolkit-0.597-SNAPSHOT-client.jar!/...
  ```

---

## 3. Результаты проверки доступности (Smoke Testing)

1. **Локальный опрос на сервере**:
   - `http://localhost:8080/hrm/` $\rightarrow$ **`HTTP 200 OK`** (JSESSIONID cookie set, text/html)
2. **Внешний доступ**:
   - `http://hr.hunttech.ru:8080/hrm/` $\rightarrow$ **`HTTP 200 OK`**
   - Сессия создается, статические ресурсы версии `0.597` отдаются штатно.

---

**Итог:** Деплой релиза **0.597** на боевой сервер `hr.hunttech.ru` успешно завершен. Приложение работает в штатном режиме.
