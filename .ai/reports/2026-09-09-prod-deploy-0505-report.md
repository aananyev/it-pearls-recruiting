# Отчёт о деплое релиза 0.505 на Production (`hr.hunttech.ru`)

> **Дата деплоя:** 2026-09-09 01:25 MSK  
> **Сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия:** `0.505` (ветка `agent/antigravity-dev`)  
> **Статус:** ✅ **УСПЕШНО ЗАДЕПЛОЕНО И ВЕРИФИЦИРОВАНО**  
> **Каталог серверного бэкапа:** `/tmp/cuba_deploy_backup_20260909_012117` (WAR + `hunttech.dump`)  

---

## 1. Состав выкаченных изменений

1. **Экран редактирования кандидата (`JobCandidateEdit`)**:
   - Восстановлена видимость и корректная высота DataGrid во вкладках:
     - «Список резюме» (`jobCandidateCandidateCvTable`, класс `.job-candidate-resume-grid`)
     - «Список взаимодействий» (`jobCandidateIteractionListTable`, класс `.job-candidate-iteraction-grid`)
     - Таблица соцсетей (`socialNetworkTable`, класс `.job-candidate-social-grid`)
   - Устранено глобальное правило `height: auto !important` на `.c-data-grid-composition`.
   - Внедрена flex-композиция `.c-data-grid-composition > .v-grid` во всех 7 темах оформления.
2. **Экран настроек (`ExtSettingsWindow`)**:
   - Ликвидировано наложение карточек во вкладке «Интерфейс».
   - Замена жесткого `<grid>` на гибкую адаптивную верстку `vbox`/`hbox` со стилями `.interface-settings-rows` и `.interface-field-row`.
   - Сохранен плейсхолдер `<grid id="grid" visible="false"/>` для обратной совместимости.
   - Центрирование профиля и кнопок в сайдбаре 312px под аватар 120px.
   - Адаптивные метрики `.interface-field-label` (`flex: 0 1 220px; overflow-wrap: anywhere`).
3. **База данных**:
   - Выполнен `updateDb` через локальный туннель CUBA Platform. Структура базы синхронизирована с кодом.

---

## 2. Ход выполнения деплоя

| Этап | Действие | Результат |
|---|---|---|
| 1 | Сборка WAR-архивов (`buildWar`) | `hrm.war` (182M), `hrm-core.war` (163M) собраны успешно |
| 2 | Создание бэкапа на проде | `/tmp/cuba_deploy_backup_20260909_012117` (`hunttech.dump` + копия действующих `.war`) |
| 3 | Остановка Tomcat (`tomcat9`) | Завершена корректно |
| 4 | Очистка кэшей CUBA | Каталоги `hrm`, `hrm-core` и `work/Catalina/localhost/*` очищены |
| 5 | Загрузка новых WAR на сервер | 361 МБ передано по SSH (pv) |
| 6 | Обновление структуры БД | `assembleDbScripts` + `updateDb` через SSH-туннель выполнены успешно |
| 7 | Запуск Tomcat | Служба `tomcat9` поднята, PID 348552 |
| 8 | Инициализация контекстов | `hrm` (15 184 ms), `hrm-core` (AppContext started, бот создан) |

---

## 3. Результаты верификации (Verification Evidence)

1. **HTTP Status**:
   - `GET http://92.63.101.170:8080/hrm/` -> **`HTTP 200 OK`** (JSESSIONID cookie выдана).
2. **Стили и темы**:
   - `GET http://92.63.101.170:8080/hrm/VAADIN/themes/hunttech-modern/styles.css` -> **`HTTP 200 OK`**.
   - Проверено наличие селекторов `.job-candidate-iteraction-grid`, `.job-candidate-resume-grid`, `.interface-settings-rows` в боевом CSS.
3. **Логи**:
   - `journalctl -u tomcat9`: `AppContext started`, отсутствие ошибок уровня `SEVERE` / `ExceptionInInitializerError`.

---

## 4. Резервная копия для экстренного отката (Rollback Reference)

При необходимости немедленного отката:
```bash
ssh root@hr.hunttech.ru
BACKUP_DIR="/tmp/cuba_deploy_backup_20260909_012117"

systemctl stop tomcat9
rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
cp -a "$BACKUP_DIR/wars/"*.war /var/lib/tomcat9/webapps/
su - postgres -c "pg_restore -p 5432 -U postgres -d hunttech --clean --if-exists '$BACKUP_DIR/hunttech.dump'"
systemctl start tomcat9
```
