# ProjectLogoImageProcessingService (`hunttech_ProjectLogoImageProcessingService`)

> Серверная обработка логотипа проекта, загружаемого пользователем в форме ProjectEdit: локальный rembg-этап (бесплатная нейросеть u2net на сервере приложения) и AI-удаление фона (capability IMAGE_GENERATION) с детерминированным классическим fallback, ресайз, вписывание в круг.

**Связанные документы:** [AI_INTEGRATION](../integrations/ai/AI_INTEGRATION.md) · [Project Edit Spec](../screens/project/hunttech_Project.edit_Spec.md) · [ImageProcessingService](file-storage/ImageProcessingService.md) (фото профиля)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Рекрутёры прикрепляют к проекту логотип — изображение компании/продукта в произвольном формате (JPEG, PNG, GIF, BMP, WebP). Логотип отображается в круглом аватаре `ovaFallbackImage` в списках и карточках. Без нормализации файл может быть тяжёлым, а прямоугольное изображение с белым фоном выглядит чужеродно в круглом аватаре (белые углы, обрезка контента по краям круга). **ProjectLogoImageProcessingService** приводит любой загруженный логотип к единому виду: PNG с прозрачным фоном, максимум 300×300, содержимое вписано в круг. С 13.08.2026 фон удаляет нейросеть (AI-функция `PROJECT_LOGO_IMAGE_GENERATE`); с 14.08.2026 первым шагом AI-конвейера стал локальный rembg (бесплатная нейросеть u2net, развёрнутая на сервере приложения — данные не покидают сервер и не требуют API-ключей), а классический конвейер остаётся автоматическим fallback — загрузка никогда не прерывается недоступностью ИИ.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

| Точка вызова | Роль |
|--------------|------|
| `ProjectEdit` (вкладка «Основное», sidebar) | Пользователь загружает логотип через кастомный upload-компонент |
| `WebProjectLogoFileUploadField` | Web-компонент (зарегистрирован в `cuba-ui-component.xml` как `upload`); перехватывает `saveFile()` в режиме IMMEDIATE и вызывает сервис |
| `web-spring.xml` | Регистрирует интерфейс в `WebRemoteProxyBeanCreator` — web-контекст получает CUBA service proxy `hunttech_ProjectLogoImageProcessingService` |

Сервис входит в AI Control Plane: AI-этап маршрутизируется через `AiExecutionService.executeImage` (стабильный function code `PROJECT_LOGO_IMAGE_GENERATE`, capability `IMAGE_GENERATION`, политики `USER_OVERRIDE_ALLOWED`/`FALLBACK_TO_ADMIN`, корпоративные credentials из `AdminAiConfiguration`). Промпт и модель администратор меняет в «Управление AI → Функции AI» без выпуска кода.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Загрузка логотипа** → `WebProjectLogoFileUploadField.saveFile` → `beanLocator.get(ProjectLogoImageProcessingService.NAME)` (proxy) → `process(data, fileName)`.
- **rembg-этап** (если `hunttech.projectLogo.rembg.enabled=true`): первый шаг AI-конвейера — POST `{url}/api/remove` (multipart, поле `file`) на локальный сервер приложения; u2net возвращает PNG с прозрачным фоном без внешних API и ключей. Недоступен (сервис лежит, таймаут, HTTP-ошибка) → платный AI-этап.
- **AI-этап** (если `hunttech.projectLogo.ai.enabled=true`): функция `PROJECT_LOGO_IMAGE_GENERATE` получает изображение и возвращает PNG с прозрачным фоном (OpenAI `images/edits`, модель `gpt-image-2`).
- **AI недоступен** (функция не активна, нет credentials, таймаут/ошибка провайдера) → лог `warn` + классический конвейер: удаление белого фона по порогу 235 (`removeAllWhite=true` — включая замкнутые полости внутри букв) и серого фона (насыщенность ≤ 30, яркость ≥ 40 — фон-градиенты типа логотипа SSP), плавный край белого фона (EDGE_SOFTNESS 24), серый фон — полностью прозрачный.
- **Детерминированный финал** (всегда): ARGB → ресайз до 300px → обрезка по содержимому → квадратный канвас со стороной = диагонали/0.95 → PNG.
- **Не-растровый файл** или пустые данные → исходные байты, `processed=false`.
- **Ошибка обработки** → компонент логирует `warn` и сохраняет исходный файл — загрузка не прерывается.

---

## 1. Архитектура и размещение

| Элемент | Путь |
|---------|------|
| Интерфейс Service API | `modules/global/src/com/company/hunttech/app/ProjectLogoImageProcessingService.java` |
| Реализация middleware | `modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java` |
| DTO результата | `modules/global/src/com/company/hunttech/app/ProcessedImage.java` (общий с `ImageProcessingService`) |
| Конфигурация | `modules/global/src/com/company/hunttech/config/HunttechProjectLogoConfig.java` |
| AI-функция | `AiFunctionConfiguration` code `PROJECT_LOGO_IMAGE_GENERATE`, capability `IMAGE_GENERATION` |
| Web-компонент | `modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java` |
| Реестр web proxy | `modules/web/src/com/company/hunttech/web-spring.xml` |
| CUBA service name | `hunttech_ProjectLogoImageProcessingService` |

Зависимости реализации: `AiExecutionService` (платный AI-этап), локальный HTTP-вызов rembg (`HttpURLConnection`, multipart form-data), CUBA `Configuration`, `ImageIO`/Java2D (классический конвейер), Apache Commons Lang.

### 1.1. Граница web/core

Аналогично `ImageProcessingService` (см. `docs/services/file-storage/ImageProcessingService.md` §1.1): core-реализация живёт в отдельном middleware webapp. Web-компонент получает её **только** через CUBA service proxy, зарегистрированный в `WebRemoteProxyBeanCreator` (`web-spring.xml`). Class-based lookup запрещён; отсутствие записи воспроизводит `NoSuchBeanDefinitionException` (баг был выявлен и закрыт 13.08.2026).

## 2. Конфигурация (`HunttechProjectLogoConfig`)

Источник: `@Source(type = SourceType.DATABASE)` — ключи в `SYS_CONFIG`:

| Свойство | Ключ | Тип | По умолчанию | Смысл |
|----------|------|-----|--------------|-------|
| `maxSize` | `hunttech.projectLogo.maxSize` | int | **300** | Максимальная сторона логотипа, px |
| `format` | `hunttech.projectLogo.format` | String | **png** | Выходной формат (PNG — прозрачность) |
| `whiteThreshold` | `hunttech.projectLogo.whiteThreshold` | int | **235** | Порог «белизны» классического flood-fill (0–255) |
| `graySaturationThreshold` | `hunttech.projectLogo.graySaturationThreshold` | int | **30** | Макс. насыщенность (max−min каналов) пикселя «серого фона» (градиенты, логотип SSP) |
| `grayMinChannel` | `hunttech.projectLogo.grayMinChannel` | int | **40** | Мин. яркость (minChannel) пикселя «серого фона»; темнее — не удаляется (тёмно-серый текст) |
| `circleInscribeRatioPercent` | `hunttech.projectLogo.circleInscribeRatio` | int | **71** | Резерв на будущее; реализация использует `CANVAS_MARGIN=0.95` от диагонали |
| `removeAllWhite` | `hunttech.projectLogo.removeAllWhite` | boolean | **true** | Удалять ВСЕ белые пиксели по порогу, включая замкнутые полости внутри букв (просвет «А»); `false` — flood-fill только от краёв (белые элементы дизайна сохраняются) |
| `enabled` | `hunttech.projectLogo.enabled` | boolean | **true** | Общий выключатель обработки |
| `aiProcessingEnabled` | `hunttech.projectLogo.ai.enabled` | boolean | **true** | Платный AI-этап; `false` — сразу классический конвейер (после rembg) |
| `rembgEnabled` | `hunttech.projectLogo.rembg.enabled` | boolean | **true** | Локальный rembg-этап (первый шаг AI-конвейера); `false` — сразу платный AI |
| `rembgUrl` | `hunttech.projectLogo.rembg.url` | String | **http://127.0.0.1:7000** | Базовый URL rembg-сервера; эндпоинт `{url}/api/remove` (multipart `file`) |
| `rembgTimeoutMs` | `hunttech.projectLogo.rembg.timeoutMs` | int | **15000** | Таймаут HTTP-запроса к rembg, мс (обработка 0.7–2.5 с + холодный старт модели) |

## 3. API сервиса

```java
String NAME = "hunttech_ProjectLogoImageProcessingService";
ProcessedImage process(byte[] data, String fileName);
```

### `ProcessedImage`

| Поле | Тип | Описание |
|------|-----|----------|
| `data` | `byte[]` | Итоговое содержимое файла |
| `name` | `String` | Имя без расширения |
| `extension` | `String` | Расширение без точки (после обработки — `png`) |
| `processed` | `boolean` | `true` — файл перекодирован; `false` — возврат оригинала |

DTO реализует `Serializable` — обязательная часть удалённого контракта web ↔ core.

## 4. Правила обработки (rembg → AI → классика)

1. `data == null` или `length == 0` → `DevelopmentException("Empty image data")`.
2. `ImageIO.read` вернул `null` → оригинал, `processed=false`.
3. `rembgEnabled=true` → `POST {rembgUrl}/api/remove` (multipart `file`, таймаут `rembgTimeoutMs`); PNG-ответ используется как источник для финала.
4. rembg недоступен (выключен, таймаут, HTTP-ошибка, пустой/не-растровый ответ) → `warn` + платный AI-этап: `aiProcessingEnabled=true` → `AiExecutionService.executeImage("PROJECT_LOGO_IMAGE_GENERATE", {sourceFileName}, data, mimeType)`.
5. AI-результат — растровый → используется как источник для финала; пустой/не-растровый/исключение → классический конвейер (лог `warn`, загрузка продолжается).
6. Финал (всегда): ARGB → ресайз ≤ `maxSize` → удаление белого/серого фона (flood-fill от краёв: белые по порогу 235; серые — насыщенность ≤ 30 при яркости ≥ 40, полностью прозрачные) → обрезка по содержимому → `fitIntoCircle` → запись в `format`.
7. Ошибка IO при обработке → `DevelopmentException`.

AI-контекст функции: `sourceFileName` (имя загруженного файла). Промпт и модель задаёт администратор; seed `260813-2-addProjectLogoAiFunction` — INSERT-only и идемпотентный.

## 5. Интеграционные точки (web)

### `WebProjectLogoFileUploadField`

Цепочка: `saveFile` → `processLogo` → `beanLocator.get(ProjectLogoImageProcessingService.NAME)` → `process(data, fileName)` → при `processed=true` — `descriptor.setExtension`, `setSize`, перезапись файла в `FileStorageService`. При ошибке — `warn` и сохранение исходного файла.

## 6. Тестирование

| Файл | Назначение |
|------|------------|
| `modules/core/test/com/company/hunttech/hunttech/core/ProjectLogoImageProcessingServiceBeanTest.java` | классический конвейер: конвертация, ресайз, прозрачность, круг, pass-through |
| `modules/core/test/com/company/hunttech/hunttech/core/ProjectLogoRembgServiceBeanTest.java` | rembg-этап: доступен → результат используется; недоступен/отключён → классический fallback (встроенный `HttpServer`-заглушка на случайном порту, конфиг через `AppContext.setProperty`) |
| `modules/core/test/com/company/hunttech/core/ProjectLogoImageProcessingServiceCoreBeanLookupTest.java` | запись proxy в `web-spring.xml`, запрет class-based lookup, AI-контракт с fallback |
| `modules/core/test/com/company/hunttech/core/ProjectLogoAiFunctionSeedContractTest.java` | seed: INSERT-only, capability IMAGE_GENERATION, русские промпты, `gpt-image-2`, include в master |

Запуск:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew :app-core:test \
  --tests '*ProjectLogoImageProcessingServiceBeanTest*' \
  --tests '*ProjectLogoRembgServiceBeanTest*' \
  --tests '*ProjectLogoImageProcessingServiceCoreBeanLookupTest*' \
  --tests '*ProjectLogoAiFunctionSeedContractTest*' \
  --no-daemon
```

## 7. Инструкция по развертыванию

- Код входит в артефакты `app-global`, `app-core`, web-клиент; миграция БД — seed AI-функции (применяется штатным `updateDb`/Liquibase).
- Web-артефакт обязан содержать запись `hunttech_ProjectLogoImageProcessingService` в `WebRemoteProxyBeanCreator`.
- **rembg-сервер** (развёрнут 14.08.2026 на проде `hr.hunttech.ru`): systemd `rembg.service` (юзер `rembg`, `NoNewPrivileges`), venv `/opt/rembg/venv`, модель `/opt/rembg/.u2net/u2net.onnx` (168 МБ), слушает только `127.0.0.1:7000`; эндпоинт `POST /api/remove` (в rembg 2.0.78 — именно `/api/remove`, `/health` отсутствует). Время: первый запрос ~2.2 с (загрузка модели), повторные ~0.7 с. Подробности и питфоллы установки — в скилле `hunttech-devops`.
- Для платного AI-этапа администратор настраивает в «Управление AI»: активную корпоративную конфигурацию (провайдер OpenAI, ключ), модель `gpt-image-2` (или свою) у функции `PROJECT_LOGO_IMAGE_GENERATE`. Без настройки — автоматический классический конвейер.

### 7.1 Корпоративное подключение OpenAI (как настроено 13.08.2026, локальная среда)

1. **Ключ шифрования**: `hunttech.ai.encryptionKey` (≥32 симв.) в `${app.home}/local.app.properties` (`deploy/app_home/local.app.properties`, вне Git — `deploy/*` в .gitignore). Пустое значение блокирует корпоративные секреты предсказуемой ошибкой.
2. **Шифрование API-ключа**: AES-GCM (SHA-256 от ключа шифрования), формат `v1:<iv>:<ciphertext>` — идентичен `AiSecretCipher`.
3. **Корпоративная конфигурация**: запись в `HUNTTECH_ADMIN_AI_CONFIGURATION` (провайдер `openai`, модель `gpt-4o`, `IS_ACTIVE=true`, `API_KEY_ENCRYPTED`).
4. **Привязка**: `HUNTTECH_AI_FUNCTION_CONFIGURATION.ADMIN_CONFIGURATION_ID` → `PROJECT_LOGO_IMAGE_GENERATE` (и `PROJECT_DESCRIPTION_GENERATE`).
5. **Проверка**: загрузка логотипа в ProjectEdit → лог `Логотип ... обработан`; при 4xx/5xx провайдера (например, HTTP 429 — исчерпаны кредиты OpenAI) — `warn` + классический fallback, загрузка не прерывается.

- Локальный deploy точного HEAD, перезапуск Tomcat, HTTP `/hrm/` = 200, smoke: ProjectEdit → загрузка логотипа → лог без `NoSuchBeanDefinitionException`.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-14 | Локальный rembg-этап — первый шаг AI-конвейера: бесплатная нейросеть u2net на сервере приложения (`POST {rembgUrl}/api/remove`, multipart `file`) удаляет фон до платного AI-этапа; недоступность rembg (сервис лежит, таймаут, HTTP-ошибка) → платный AI → классика; конфиг `hunttech.projectLogo.rembg.{enabled,url,timeoutMs}`; тест `ProjectLogoRembgServiceBeanTest` (встроенный `HttpServer`-заглушка, 3 сценария); сервер развёрнут на проде `hr.hunttech.ru` (systemd rembg.service, 127.0.0.1:7000) |
| 2026-08-14 | Классический конвейер удаляет серый фон-градиенты (логотип SSP): пиксели с насыщенностью ≤ 30 и яркостью ≥ 40 (`graySaturationThreshold`/`grayMinChannel`), соединённые с краем, становятся полностью прозрачными; белый фон — как раньше (порог 235, плавный край); тест `testGrayGradientBackgroundBecomesTransparent` |
| 2026-08-13 | Классический конвейер: конфиг `hunttech.projectLogo.removeAllWhite` (default true) — удаление всех белых пикселей по порогу, включая замкнутые полости внутри букв (просвет «А» Альфа-Банка); тест `testWhiteCavityInsideLetterBecomesTransparent` |
| 2026-08-13 | Настроено корпоративное подключение OpenAI: `hunttech.ai.encryptionKey` в `${app.home}/local.app.properties`, admin-конфигурация `HUNTTECH_ADMIN_AI_CONFIGURATION` (openai, gpt-4o, активна), привязка к функциям `PROJECT_LOGO_IMAGE_GENERATE` и `PROJECT_DESCRIPTION_GENERATE`; загрузка логотипа реально вызывает `images/edits` (при 4xx — классический fallback) |
| 2026-08-13 | AI-first: функция `PROJECT_LOGO_IMAGE_GENERATE` (IMAGE_GENERATION, OpenAI `images/edits`) с детерминированным классическим fallback; исправлена интеграция web↔core (запись в `WebRemoteProxyBeanCreator` устранила `NoSuchBeanDefinitionException`); конфиг `hunttech.projectLogo.ai.enabled` |
| 2026-08-12 | Создание сервиса: классический конвейер (PNG, ресайз 300, flood-fill белого фона, вписывание в круг), интеграция с `WebProjectLogoFileUploadField` |
