# ProjectLogoImageProcessingService (`hunttech_ProjectLogoImageProcessingService`)

> Legacy-сервис художественной обработки логотипа: локальный rembg, AI-удаление фона и классический fallback. С 2026-09-21 общий upload sidebar/profile изображений использует отдельный нейтральный `SidebarImageNormalizationService`; этот сервис не является текущим обязательным pre-storage pipeline ProjectEdit.

**Связанные документы:** [AI_INTEGRATION](../integrations/ai/AI_INTEGRATION.md) · [Project Edit Spec](../screens/project/hunttech_Project.edit_Spec.md) · [ImageProcessingService](file-storage/ImageProcessingService.md) (фото профиля)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Исторически сервис приводил логотип к PNG с прозрачным фоном, максимум 300×300, и вписывал содержимое в круг с rembg/AI/flood-fill fallback. Этот художественный контракт меняет фон и canvas, поэтому он не подходит для общей безопасной загрузки, где обязательны сохранение пропорций/alpha, отсутствие crop/stretch и отказ вместо сохранения недекодируемого original. Текущий общий upload-контракт описан в [SidebarImageNormalizationService](SidebarImageNormalizationService.md); legacy-сервис сохраняется как отдельная специализированная возможность и для совместимости существующего API.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

| Точка вызова | Роль |
|--------------|------|
| Legacy API / специализированный вызов | Явный вызов `ProjectLogoImageProcessingService` сохраняет rembg/AI/flood-fill поведение |
| `ProjectEdit` и общий `WebProjectLogoFileUploadField` | С 2026-09-21 используют `SidebarImageNormalizationService`, а не этот сервис |
| `web-spring.xml` | Legacy proxy может сохраняться для совместимости; общий upload дополнительно регистрирует `hunttech_SidebarImageNormalizationService` |

Сервис входит в AI Control Plane: AI-этап маршрутизируется через `AiExecutionService.executeImage` (стабильный function code `PROJECT_LOGO_IMAGE_GENERATE`, capability `IMAGE_GENERATION`, политики `USER_OVERRIDE_ALLOWED`/`FALLBACK_TO_ADMIN`, корпоративные credentials из `AdminAiConfiguration`). Промпт и модель администратор меняет в «Управление AI → Функции AI» без выпуска кода.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Ниже зафиксирован legacy-контракт при **явном** вызове сервиса. Автоматический upload Project/Company/City/Region/JobCandidate/CandidateCV/Person ему больше не делегирует.

- **Явный legacy-вызов** → клиент получает proxy `ProjectLogoImageProcessingService.NAME` → `process(data, fileName)` (режим логотипа) или `process(data, fileName, true)` (исторический щадящий режим фото). Общий upload-компонент эту цепочку больше не запускает.
- **Фото кандидата (щадящий режим)**: фон удаляется ТОЛЬКО нейросетью rembg/u2net (обучен на людях); при недоступности rembg фон сохраняется — конвертация в PNG + ресайз без искажения пропорций; классический flood-fill и вписывание в круг НЕ применяются (съедают светлые участки человека — кожу, белую одежду, блики); AI-функция `PROJECT_LOGO_IMAGE_GENERATE` (логотипная) для фото не вызывается.
- **rembg-этап** (если `hunttech.projectLogo.rembg.enabled=true`): первый шаг AI-конвейера — POST `{url}/api/remove` (multipart, поле `file`) на локальный сервер приложения; u2net возвращает PNG с прозрачным фоном без внешних API и ключей. Недоступен (сервис лежит, таймаут, HTTP-ошибка) → платный AI-этап (логотипы) либо «без удаления фона» (фото кандидата).
- **AI-этап** (если `hunttech.projectLogo.ai.enabled=true`): функция `PROJECT_LOGO_IMAGE_GENERATE` получает изображение и возвращает PNG с прозрачным фоном (OpenAI `images/edits`, модель `gpt-image-2`).
- **AI недоступен** (функция не активна, нет credentials, таймаут/ошибка провайдера) → лог `warn` + классический конвейер: удаление белого фона по порогу 235 (`removeAllWhite=true` — включая замкнутые полости внутри букв) и серого фона (насыщенность ≤ 30, яркость ≥ 40 — фон-градиенты типа логотипа SSP), плавный край белого фона (EDGE_SOFTNESS 24), серый фон — полностью прозрачный.
- **Детерминированный финал** (всегда): ARGB → ресайз до 300px → обрезка по содержимому → квадратный канвас со стороной = диагонали/0.95 → PNG.
- **Не-растровый файл** или пустые данные → исходные байты, `processed=false`.
- **Ошибка legacy-обработки** → сервис/явный клиент применяет собственный исторический error contract. Это не разрешает общему sidebar upload сохранять исходник: его текущий контракт — жёсткий reject с сохранением прежнего значения.

---

## 1. Архитектура и размещение

| Элемент | Путь |
|---------|------|
| Интерфейс Service API | `modules/global/src/com/company/hunttech/app/ProjectLogoImageProcessingService.java` |
| Реализация middleware | `modules/core/src/com/company/hunttech/app/ProjectLogoImageProcessingServiceBean.java` |
| DTO результата | `modules/global/src/com/company/hunttech/app/ProcessedImage.java` (общий с `ImageProcessingService`) |
| Конфигурация | `modules/global/src/com/company/hunttech/config/HunttechProjectLogoConfig.java` |
| AI-функция | `AiFunctionConfiguration` code `PROJECT_LOGO_IMAGE_GENERATE`, capability `IMAGE_GENERATION` |
| Исторический web-клиент | `modules/web/src/com/company/hunttech/web/gui/components/WebProjectLogoFileUploadField.java` (с 2026-09-21 вызывает другой сервис) |
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
| `aiProcessed` | `boolean` | `true` — фон удалён нейросетью (rembg/u2net или AI-функция), а не классикой |
| `aiExecution` | `AiExecutionResult \| null` | Метаданные платного AI-выполнения (модель, провайдер, собственник API) — заполнены только когда фон удалён AI-функцией `PROJECT_LOGO_IMAGE_GENERATE`; `null` при локальном rembg/классике. Используется для нотификации «какая модель что сделала + чей API» (контракт [HRM_HuntTech_AI_User_Notification_Contract](../architecture/HRM_HuntTech_AI_User_Notification_Contract.md)) |

DTO реализует `Serializable` — обязательная часть удалённого контракта web ↔ core.

Исторический web-клиент показывал TRAY-нотификацию по `aiExecution`. Текущий
`WebProjectLogoFileUploadField` вызывает детерминированный `SidebarImageNormalizationService`
и не показывает AI-нотификацию; metadata остаются частью legacy DTO только для явных
потребителей этого сервиса.

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

Историческая цепочка `saveFile → ProjectLogoImageProcessingService.process` заменена общим контрактом `saveFile → SidebarImageNormalizationService.normalize`. В частности, автоматический fallback «при ошибке сохранить исходный файл» для image-binding запрещён: invalid input отклоняется, а прежний bound `FileDescriptor`/preview сохраняется. Legacy API этого сервиса не удалён, но новый общий upload-компонент его не вызывает.

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
| 2026-09-21 | Общий Project/sidebar image upload переведён на нейтральный `SidebarImageNormalizationService`; legacy rembg/AI/flood-fill сервис сохранён отдельно, но `WebProjectLogoFileUploadField` больше не вызывает его и не сохраняет original при ошибке. |
| 2026-08-16 | Контракт пользовательской нотификации: `ProcessedImage` несёт `aiExecution` (модель, провайдер, собственник API) при реальном применении AI-функции; `WebProjectLogoFileUploadField` показывает исчезающую нотификацию «Логотип обработан с помощью AI» с моделью/собственником API |
| 2026-08-15 | **Щадящий режим для фото кандидата** (`JobCandidate.fileImageFace`): новый метод `process(data, fileName, candidatePhoto)`. Для фото людей классический flood-fill/вписывание в круг НЕ применяются (съедали светлые участки — кожу, белую одежду, блики): фон удаляется только нейросетью rembg/u2net (обучен на людях), при недоступности — фон сохраняется (конвертация PNG + ресайз, пропорции не искажаются); AI-функция `PROJECT_LOGO_IMAGE_GENERATE` (логотипная) для фото не используется. Логотипы (`projectLogo`, `fileCompanyLogo`) — прежний конвейер. Фикс повторной загрузки в `WebProjectLogoFileUploadField`: `getComposition().markAsDirty()` после `saveFile` — legacy RPC `continueUploading()` отправляется только при paint, без этого клик по кнопке «Загрузить» после первой загрузки не открывал диалог выбора файла. Тесты: `testCandidatePhotoKeepsLightShirtAndBody`, `testCandidatePhotoKeepsWhiteCavityInsideBody`, `testCandidatePhotoKeepsOriginalAspectRatio` |
| 2026-08-14 | Локальный rembg-этап — первый шаг AI-конвейера: бесплатная нейросеть u2net на сервере приложения (`POST {rembgUrl}/api/remove`, multipart `file`) удаляет фон до платного AI-этапа; недоступность rembg (сервис лежит, таймаут, HTTP-ошибка) → платный AI → классика; конфиг `hunttech.projectLogo.rembg.{enabled,url,timeoutMs}`; тест `ProjectLogoRembgServiceBeanTest` (встроенный `HttpServer`-заглушка, 3 сценария); сервер развёрнут на проде `hr.hunttech.ru` (systemd rembg.service, 127.0.0.1:7000) |
| 2026-08-14 | Классический конвейер удаляет серый фон-градиенты (логотип SSP): пиксели с насыщенностью ≤ 30 и яркостью ≥ 40 (`graySaturationThreshold`/`grayMinChannel`), соединённые с краем, становятся полностью прозрачными; белый фон — как раньше (порог 235, плавный край); тест `testGrayGradientBackgroundBecomesTransparent` |
| 2026-08-13 | Классический конвейер: конфиг `hunttech.projectLogo.removeAllWhite` (default true) — удаление всех белых пикселей по порогу, включая замкнутые полости внутри букв (просвет «А» Альфа-Банка); тест `testWhiteCavityInsideLetterBecomesTransparent` |
| 2026-08-13 | Настроено корпоративное подключение OpenAI: `hunttech.ai.encryptionKey` в `${app.home}/local.app.properties`, admin-конфигурация `HUNTTECH_ADMIN_AI_CONFIGURATION` (openai, gpt-4o, активна), привязка к функциям `PROJECT_LOGO_IMAGE_GENERATE` и `PROJECT_DESCRIPTION_GENERATE`; загрузка логотипа реально вызывает `images/edits` (при 4xx — классический fallback) |
| 2026-08-13 | AI-first: функция `PROJECT_LOGO_IMAGE_GENERATE` (IMAGE_GENERATION, OpenAI `images/edits`) с детерминированным классическим fallback; исправлена интеграция web↔core (запись в `WebRemoteProxyBeanCreator` устранила `NoSuchBeanDefinitionException`); конфиг `hunttech.projectLogo.ai.enabled` |
| 2026-08-12 | Создание сервиса: классический конвейер (PNG, ресайз 300, flood-fill белого фона, вписывание в круг), интеграция с `WebProjectLogoFileUploadField` |
