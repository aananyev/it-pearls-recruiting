# Экран «Журнал вызовов AI» (AiCallLogBrowse)

## Назначение

Экран `AiCallLogBrowse` (`hunttech_AiCallLog.browse`, `ai-call-log-browse.xml`) предназначен для оперативного мониторинга и аудита всех вызовов к искусственному интеллекту в системе.

## Размещение в меню
- Раздел: «Управление AI» (`aiAdministration`)
- Пункт: «Журнал вызовов AI» (`menu_config.hunttech_AiCallLog.browse`)
- Иконка: `HISTORY`

---

## Структура интерфейса

Экран построен с использованием вертикального сплит-контейнера (`split`):
1. **Верхняя часть (65% высоты)**:
   - Фильтр CUBA (`filter`) по всем свойствам сущности.
   - Панель кнопок (`buttonsPanel`): Обновить (`REFRESH`), Удалить (`REMOVE`).
   - Таблица `aiCallLogsTable` (`groupTable`) с колонками:
     * **Время**: Дата/время вызова (`callTime`).
     * **Пользователь**: ФИО или логин с генерацией (`userDisplay`).
     * **Источник вызова**: Экран или сервис-источник (`callerSource`).
     * **Функция**: Наименование AI-функции (`functionName`).
     * **Провайдер**: Код провайдера (`providerCode`).
     * **Модель**: Имя модели (`modelName`).
     * **Токены**: Объем токенов в формате `Всего (Вход / Выход)` (`tokensDisplay`).
     * **Стоимость**: Расчетная стоимость зеленым цветом с валютой (`costDisplay`).
     * **Длительность**: Время выполнения запроса в секундах (`durationDisplay`).
     * **Статус**: Цветной бейдж `OK` (зеленый) или `ERROR` (красный) (`statusDisplay`).
2. **Нижняя часть (35% высоты)**:
   - Вкладки `tabSheet` с детальной информацией по выбранной строке:
     * Вкладка **«Промпт»** (`promptTab`): полный исходный текст запроса.
     * Вкладка **«Ответ AI»** (`responseTab`): полученный от модели ответ.
     * Вкладка **«Ошибка»** (`errorTab`): текст технической ошибки и стек исключения при сбое.

---

## Связанные классы и файлы
- XML дескриптор: `modules/web/src/com/company/hunttech/web/screens/aicalllog/ai-call-log-browse.xml`
- Контроллер: `modules/web/src/com/company/hunttech/web/screens/aicalllog/AiCallLogBrowse.java`
- Сообщения локализации: `messages.properties` / `messages_ru.properties`
