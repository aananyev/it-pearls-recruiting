# Отчёт о деплое релиза 0.555 на Production (`hr.hunttech.ru`)

> **Дата деплоя:** 2026-09-12 15:37 MSK  
> **Сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия:** `0.555` (ветка `agent/antigravity-dev`, коммит `0d853a62`)  
> **Статус:** ✅ **УСПЕШНО ЗАДЕПЛОЕНО И ВЕРИФИЦИРОВАНО**  
> **Каталог серверного бэкапа:** `/tmp/cuba_deploy_backup_20260912_153130` (WAR + `hunttech.dump` 749 МБ)  

---

## 1. Состав выкаченных изменений

1. **Отметка о дате и времени сообщений (LLM-чат)**:
   - В конце каждого сообщения пользователя и ответа нейросети выводится штамп: `<div class="llm-chat-msg-footer"><span class="llm-chat-msg-time">dd.MM.yyyy HH:mm:ss</span></div>`.
   - Штампы поддерживаются для обеих вкладок («Чат с ИИ» и «Hermes»), а также при живом потоковом выводе (streaming).
2. **Безопасное отображение форматированного текста (HTML)**:
   - Внедрен санитизированный парсер с белым списком безопасных тегов (`span`, `b`, `strong`, `i`, `table`, `code`, `p` и др.).
   - Исключено экранирование полезной разметки (например, цветные ценники вакансий из Hermes, блоки кода).
   - Потенциально опасные элементы (`script`, `iframe`, `object`, опасные inline-события JS) строго нейтрализуются.
3. **Интерактивное открытие карточки вакансии/сущности в HRM**:
   - В сообщениях Hermes ссылки на вакансии (`#main/0/hunttech_OpenPosition.edit?id=...`, `http://localhost:8080/hrm/#main/0/...`, `hrm://vacancy/...`) преобразуются в кликабельные кнопки.
   - По нажатию открывается экран редактирования сущности (`OpenPositionEdit`) без блокировки по правам на запись (пользователи без права редактирования открывают карточку в режиме чтения).
4. **Автоматический скролл до последних сообщений**:
   - При открытии чата и переключении между вкладками «Чат с ИИ» и «Hermes» скролл плавно и надежно перемещается до самых последних сообщений.
   - Реализована каскадная цепочка задержек DOM-рендеринга (`10..1500 ms`).

---

## 2. Ход выполнения деплоя

| Этап | Действие | Результат |
|---|---|---|
| 1 | Локальный коммит и push | Коммит `0d853a62` отправлен в `origin HEAD:agent/antigravity-dev` |
| 2 | Сборка WAR (`buildWar`) | `hrm.war` (182M), `hrm-core.war` (163M) успешно скомпилированы (`BUILD SUCCESSFUL`) |
| 3 | Серверный бэкап | `/tmp/cuba_deploy_backup_20260912_153130`: сохранен дамп БД (`hunttech.dump` 749M) и боевые WAR |
| 4 | Остановка Tomcat (`tomcat9`) | Завершена штатно (`systemctl stop tomcat9`) |
| 5 | Очистка кэшей CUBA | Каталоги распакованных приложений и кэш Catalina очищены |
| 6 | Загрузка новых WAR | 345 МБ передано по SSH через `pv` + `rsync` |
| 7 | Синхронизация БД | `assembleDbScripts` + `updateDb` через локальный SSH-туннель выполнены успешно |
| 8 | Запуск Tomcat | Служба `tomcat9` запущена (PID 1322411) |
| 9 | Инициализация приложений | `hrm`: `AppContext started` (16 249 ms); `hrm-core`: `AppContext started` (94 259 ms), Telegram бот инициализирован |

---

## 3. Результаты верификации (Verification Evidence)

1. **HTTP Status приложения:**
   - `GET http://hr.hunttech.ru:8080/hrm/` -> **`HTTP 200 OK`** (выдана cookie `JSESSIONID`).
   - `GET http://92.63.101.170:8080/hrm/` -> **`HTTP 200 OK`**.
2. **Стили и тема:**
   - `GET http://hr.hunttech.ru:8080/hrm/VAADIN/themes/hunttech-modern/styles.css` -> **`HTTP 200 OK`** (724 КБ).
   - В боевом CSS присутствуют классы `.llm-chat-msg-time`, `.llm-chat-launcher`, `.llm-chat-launcher-window`.
3. **Логи Tomcat:**
   - `journalctl -u tomcat9`: чистый старт, ProtocolHandler `http-nio-0.0.0.0-8080` активен, ошибок `SEVERE` нет.

---

## 4. Инструкция по экстренному откату (Rollback Reference)

При необходимости немедленного возврата к состоянию до релиза 0.555:
```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail
BACKUP_DIR="/tmp/cuba_deploy_backup_20260912_153130"

systemctl stop tomcat9
rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
rm -rf /var/lib/tomcat9/work/Catalina/localhost/* /var/lib/tomcat9/temp/*
cp -a "$BACKUP_DIR/wars/"*.war /var/lib/tomcat9/webapps/
chown -R tomcat:tomcat /var/lib/tomcat9/webapps/hrm*.war

# Восстановление базы данных:
su - postgres -c "pg_restore -p 5432 -U postgres -d hunttech --clean --if-exists '$BACKUP_DIR/hunttech.dump'"

systemctl start tomcat9
EOF
```
