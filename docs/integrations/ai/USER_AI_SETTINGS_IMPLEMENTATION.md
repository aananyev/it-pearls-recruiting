# Персональные AI-настройки пользователя — документация изменений

Документ описывает изменения, внесённые в окно пользовательских настроек HuntTech HRM для управления персональными подключениями к API нейросетей.

## 1. Назначение изменения

Пользователь должен иметь возможность самостоятельно настроить одно или несколько подключений к AI-провайдерам. Настройки не должны пересекаться с настройками других пользователей. Для выбранного подключения пользователь должен иметь возможность выполнить реальное тестирование API.

## 2. Изменённые компоненты

| Слой | Файл | Изменение |
|------|------|-----------|
| Global service contract | `modules/global/src/com/company/hunttech/service/HrmAiService.java` | Добавлен метод `testConnection(UserAiConfiguration configuration)` |
| Core service | `modules/core/src/com/company/hunttech/service/HrmAiServiceBean.java` | Реализована проверка выбранной AI-конфигурации через реальный вызов провайдера |
| Core providers | `modules/core/src/com/company/hunttech/core/ai/*Provider.java` | Реализованы адаптеры десяти российских, американских и китайских AI-сервисов |
| Core HTTP base | `modules/core/src/com/company/hunttech/core/ai/AbstractOpenAiCompatibleProvider.java` | Общая обработка OpenAI-совместимых запросов, таймаутов, ошибок и ответов |
| Core test | `modules/core/test/com/company/hunttech/core/ai/AIProviderCatalogTest.java` | Проверяет десять уникальных кодов и наличие моделей по умолчанию без внешних API-вызовов |
| Web controller | `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java` | Добавлена таблица AI-конфигураций, CRUD-действия и кнопка тестирования |
| Web editor | `modules/web/src/com/company/hunttech/web/screens/useraiconfiguration/UserAiConfigurationEdit.java` | Добавлены десять провайдеров и автоподстановка модели для каждого из них |
| Web descriptor | `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml` | Добавлен datasource `userAiConfigsDs` и вкладка `AI` с таблицей |
| Localization | `messages.properties`, `messages_ru.properties` | Добавлены подписи вкладки, кнопки тестирования и сообщений результата |

## 3. Модель данных

Используется существующая сущность `UserAiConfiguration`.

| Поле | Роль в новой вкладке |
|------|----------------------|
| `user` | Владелец персональной настройки |
| `providerCode` | Код AI-провайдера |
| `apiKey` | Секретный API-ключ |
| `defaultModelName` | Модель по умолчанию для данного подключения |
| `isActive` | Признак активной конфигурации |

Новая миграция БД не потребовалась: таблица `HUNTTECH_USER_AI_CONFIGURATION` уже существовала.

## 4. Изоляция настроек пользователей

В `ext-settings-window.xml` добавлен datasource:

```xml
<collectionDatasource id="userAiConfigsDs"
                      class="com.company.hunttech.entity.UserAiConfiguration"
                      view="userAiConfiguration-view">
    <query>
        <![CDATA[select e from hunttech_UserAiConfiguration e where e.user = :ds$extUserDs]]>
    </query>
</collectionDatasource>
```

`ExtSettingsWindow` загружает `extUserDs` из текущей сессии:

```java
currentUser = (ExtUser) userSessionSource.getUserSession().getUser();
loadExtUser();
```

После этого datasource таблицы AI ограничен текущим пользователем. Пользователь не получает список конфигураций других пользователей и не может создать запись на другого владельца через личные настройки.

## 5. UI-поведение вкладки AI

Вкладка `AI` содержит:

| Элемент | Поведение |
|---------|-----------|
| Таблица `aiConfigsTable` | Показывает `providerCode`, `defaultModelName`, `isActive` |
| `Создать` | Открывает `UserAiConfigurationEdit` для новой записи |
| `Редактировать` | Доступна только при выбранной строке |
| `Удалить` | Доступна только при выбранной строке |
| `Тестирование подключения` | Доступна только при выбранной строке |

Доступность кнопок управляется методом `refreshAiActionState()`:

```java
boolean selected = aiConfigsTable != null && aiConfigsTable.getSingleSelected() != null;
aiConfigsEditBtn.setEnabled(selected);
aiConfigsRemoveBtn.setEnabled(selected);
aiConfigsTestBtn.setEnabled(selected);
```

## 6. Создание и редактирование конфигурации

Окно личных настроек переиспользует существующую модаль `UserAiConfigurationEdit`.

При создании записи контроллер задаёт владельца:

```java
entity.setUser(currentUser);
entity.setIsActive(true);
```

После закрытия модального окна таблица обновляется через `refreshAiConfigs()`.

## 7. Тестирование подключения

Кнопка `Тестирование подключения` вызывает:

```java
hrmAiService.testConnection(selected);
```

Web-слой не делает HTTP-запросы напрямую. Он только передаёт выбранную персональную конфигурацию в middleware-сервис. Это сохраняет текущую архитектуру AI-подсистемы: UI управляет данными и уведомлениями, а core-слой отвечает за провайдеров и реальные API-вызовы.

## 8. Алгоритм `HrmAiServiceBean.testConnection`

Метод выполняет следующие проверки:

1. Конфигурация выбрана.
2. Указан `providerCode`.
3. Указан `apiKey`.
4. Провайдер найден в `AIProviderRegistry`.
5. Выполнен короткий реальный запрос к API.
6. Ответ не пустой.

Тестовый запрос:

```java
provider.generateText(
        "Ответь одним словом: ok",
        "Тестирование подключения к API искусственного интеллекта.",
        configuration.getApiKey(),
        configuration.getDefaultModelName(),
        Map.of("temperature", 0.0));
```

Такой запрос проверяет сразу несколько вещей:

- валидность API-ключа;
- доступность endpoint провайдера;
- корректность имени модели;
- работоспособность парсинга ответа в реализации `AIProvider`;
- наличие Java-компонента провайдера в приложении.

## 9. Обработка результата

При успехе пользователь видит уведомление:

```text
Подключение к API нейросети успешно проверено
```

При ошибке пользователь видит уведомление:

```text
Ошибка подключения к API нейросети
```

Подробная причина передаётся в описание уведомления из исключения.

Если провайдер выбран в UI, но не реализован в Java-коде приложения, сервис возвращает понятное сообщение:

```text
Провайдер AI «<код>» не подключён в приложении.
```

## 10. Поддерживаемые провайдеры

На момент изменения UI и core-слой поддерживают одинаковый набор:

| Регион | Java-компонент | Код | Протокол |
|--------|----------------|-----|----------|
| Россия | `YandexGptProvider` | `yandex` | Yandex Completion API, `Api-Key` |
| Россия | `GigaChatProvider` | `gigachat` | OAuth 2 + GigaChat Chat Completions |
| США | `OpenAiProvider` | `openai` | OpenAI Chat Completions |
| США | `AnthropicProvider` | `anthropic` | Anthropic Messages API |
| США | `GeminiProvider` | `gemini` | Gemini GenerateContent API |
| США | `GrokProvider` | `grok` | OpenAI-совместимый xAI API |
| Китай | `DeepSeekProvider` | `deepseek` | OpenAI-совместимый API |
| Китай | `QwenProvider` | `qwen` | Alibaba Model Studio compatibility API |
| Китай | `KimiProvider` | `kimi` | Moonshot compatibility API |
| Китай | `GlmProvider` | `glm` | Z.AI compatibility API |

### 10.1 Провайдеры в SettingWindow

Все провайдеры подключаются через единый пользовательский сценарий:

1. Пользователь открывает личные настройки.
2. Переходит на вкладку `AI`.
3. Нажимает `Создать`.
4. Выбирает одного из десяти провайдеров.
5. Поле `Модель по умолчанию` автоматически получает рекомендуемое значение, если оно было пустым.
6. Пользователь вводит личный API-ключ в поле `API-ключ`.
7. Сохраняет запись и запускает `Тестирование подключения`.

API-ключ не добавляется в исходный код и update-скрипты БД. Это осознанное ограничение: ключ является секретом конкретного пользователя и должен попадать в БД только через защищённый пользовательский ввод в SettingWindow.

### 10.2 Особенности авторизации

- YandexGPT разбирает значение `folderId|apiKey`, строит `modelUri` и отправляет секрет в заголовке `Authorization: Api-Key`.
- GigaChat принимает ключ авторизации, получает краткоживущий OAuth-токен и только затем вызывает генерацию. При отсутствии scope используется `GIGACHAT_API_PERS`.
- Claude использует заголовки `x-api-key` и `anthropic-version` и разбирает формат Anthropic Messages.
- Gemini передаёт ключ в `x-goog-api-key` и разбирает формат GenerateContent.
- Остальные шесть сервисов используют общий OpenAI-совместимый транспорт.

### 10.3 Совместимость старых записей MiMo

Старый код `mimo` удалён из выбора, потому что прежняя реализация с названием Xiaomi MiMo фактически обращалась к Moonshot AI. Для такой персональной записи нужно открыть настройку, выбрать `Moonshot Kimi`, проверить модель `kimi-k2.5` и сохранить. Новый стабильный код — `kimi`.

## 11. Ограничения и важные замечания

- API-ключ хранится в существующей колонке `API_KEY`; отдельное шифрование в рамках этого изменения не добавлялось.
- Browse-экран мониторинга не должен отображать API-ключи.
- Тест подключения выполняет реальный внешний запрос и может расходовать минимальные лимиты провайдера.
- Если пользователь изменил запись, тестировать нужно после сохранения модального окна редактирования.
- Для рабочих AI-функций `HrmAiServiceBean` по-прежнему выбирает активную конфигурацию текущего пользователя.

## 12. Проверка после изменения

Выполнена компиляция:

```bash
./gradlew app-global:compileJava app-core:compileJava app-web:compileJava
```

Результат: `BUILD SUCCESSFUL`.

Выполнен изолированный тест каталога, не расходующий лимиты внешних AI API:

```bash
./gradlew app-core:test --tests com.company.hunttech.core.ai.AIProviderCatalogTest
```

Результат: `BUILD SUCCESSFUL`, каталог содержит ровно десять уникальных провайдеров.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-21 | Добавлена вкладка AI в личные настройки, таблица персональных подключений и тест подключения |
| 2026-07-21 | Добавлена поддержка DeepSeek в SettingWindow и core-провайдер `DeepSeekProvider` |
| 2026-07-21 | Реализованы 10 AI-провайдеров, единый транспорт совместимых API и специальные адаптеры Claude, Gemini, YandexGPT и GigaChat |
