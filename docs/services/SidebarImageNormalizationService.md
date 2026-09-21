# SidebarImageNormalizationService (`hunttech_SidebarImageNormalizationService`)

## Назначение и бизнес-смысл (What & Why)

HRM HuntTech принимает изображения из распространённых пользовательских raster-форматов, но нормализует их в компактный единый внутренний формат PNG. Это обеспечивает предсказуемое отображение, сокращает объём файлов и ускоряет sidebar-интерфейсы. Проверяется фактический codec содержимого, а не только расширение файла.

## UI Context & Navigation

Сервис предназначен для всех подтверждённых пользовательских graphics-upload контролов HRM HuntTech: логотипов, иконок, флагов, эмблем, фотографий и аватаров. Пятнадцать контролов используют прежний `FileDescriptor`/FileStorage contract через общий upload-компонент; логотип корпоративного AI-подключения хранится в BLOB и вызывает тот же normalizer через отдельный MANUAL adapter. Документы, оригиналы CV, PDF, Excel и вложения общего назначения в этот pipeline не входят.

## Behavior Summary

`Пользователь загружает изображение → проверяются byte/dimension/pixel limits → ImageIO определяет реальный codec → декодируется первый кадр → при необходимости изображение уменьшается в пределах 512×512 без upscale → к уменьшенному JPEG применяется EXIF Orientation → alpha сохраняется в ARGB → результат кодируется в lossless PNG с умеренным быстрым сжатием → web-слой сохраняет PNG штатным механизмом.` Если проверка или декодирование не прошли, original не сохраняется, предыдущее bound-значение/preview не меняется, временный upload удаляется.

## Архитектура и API

| Элемент | Путь |
|---|---|
| Service API | `modules/global/src/com/company/hunttech/app/SidebarImageNormalizationService.java` |
| Middleware bean | `modules/core/src/com/company/hunttech/app/SidebarImageNormalizationServiceBean.java` |
| Result DTO | `modules/global/src/com/company/hunttech/app/ProcessedImage.java` |

```java
ProcessedImage normalize(byte[] data, String fileName);
```

Сервис не доверяет extension/MIME имени. Зафиксированный runtime-контракт: PNG, JPG/JPEG, GIF, BMP, WBMP, WebP и TIFF. WebP reader поставляется в middleware зависимостью `com.twelvemonkeys.imageio:imageio-webp:3.13.1`; TIFF reader зафиксирован на используемом JDK 11. Для GIF читается кадр с индексом `0`. SVG и документы не принимаются без отдельного безопасного rasterizer/parser.

## Потребители и границы

| Экран | Binding | Хранение после нормализации |
|---|---|---|
| ProjectEdit | `Project.projectLogo` | прежний `FileDescriptor` / FileStorage |
| CompanyEdit | `Company.fileCompanyLogo` | прежний `FileDescriptor` / FileStorage |
| CityEdit | `City.fileCityEmblem` | прежний `FileDescriptor` / FileStorage |
| RegionEdit | `Region.fileRegionEmblem` | прежний `FileDescriptor` / FileStorage |
| JobCandidateEdit | `JobCandidate.fileImageFace` | прежний `FileDescriptor` / FileStorage |
| CandidateCVEdit | `CandidateCV.fileImageFace` | прежний `FileDescriptor` / FileStorage |
| PersonEdit | `Person.fileImageFace` | прежний `FileDescriptor` / FileStorage |
| PositionEdit | `Position.filePositionIcon` | прежний `FileDescriptor` / FileStorage |
| SkillTreeEdit | `SkillTree.fileImageLogo` | прежний `FileDescriptor` / FileStorage |
| CountryEdit | `Country.fileFlag` | прежний `FileDescriptor` / FileStorage |
| SocialNetworkTypeEdit | `SocialNetworkType.logo` | прежний `FileDescriptor` / FileStorage |
| ExtUserEdit | `ExtUser.officialPhoto` | legacy datasource + прежний `FileDescriptor` / FileStorage |
| ExtSettingsWindow | `ExtUser.userAvatar` | legacy datasource + `UserAvatarManagementService`, без повторной image processing |
| ApplicationSetup | `applicationLogo` | прежний `FileDescriptor` / FileStorage |
| ApplicationSetup | `applicationIcon` | прежний `FileDescriptor` / FileStorage |
| AdminAiConfigurationEdit | `AdminAiConfiguration.logoImage` | MANUAL adapter → нормализованные PNG bytes в существующий BLOB |

Все 16 graphics controls перечисляют PNG, JPG/JPEG, GIF, BMP, WBMP, WebP и TIFF и ограничивают upload 20 MiB. Фильтр не заменяет middleware-проверку: даже файл с разрешённым расширением отклоняется, если ImageIO не подтверждает фактический raster codec. Общий `WebProjectLogoFileUploadField` fail-closed нормализует только whitelist свойств; остальные FileUpload bindings сохраняют стандартное поведение CUBA.

Специализированное исключение: `ExtSettingsWindowMainBackground.mainScreenBackgroundUpload` не входит в эти 16 контролов. Это отдельный фон рабочего стола с собственным JPEG/background pipeline, bounding box 2560×1440, лимитом 15 MiB и отдельным storage contract. Документы вакансий/компаний, оригиналы CV, SomeFiles, LaborAgreement и прочие generic attachments также не проходят через image normalizer.

## Ограничения безопасности и качества

- размер входного файла: максимум 20 MiB;
- разрешение: максимум 25 000 000 пикселей (включая 5000×5000), проверяется через reader metadata до полного decode;
- bounding box: 512×512;
- crop, растягивание, фон и рамки не применяются;
- маленькие изображения не увеличиваются;
- output extension/MIME contract: `png` / `image/png`, PNG signature обязательна;
- compression: lossless, средний профиль; скорость UI request важнее предельной экономии нескольких килобайт.
- failure contract: отсутствует fallback на исходные байты; invalid/oversized/unsupported input не попадает в FileStorage и не заменяет текущее значение поля;
- storage/layout contract: схема сущностей, `FileDescriptor`, FileStorage, размеры и расположение sidebar/preview не меняются.

JPEG EXIF Orientation считывается существующим Thumbnailator `ExifUtils`; при отсутствии orientation в metadata reader используется безопасный raw APP1 fallback. Сначала исходник уменьшается до 512 px по большей стороне, затем orientation filter применяется к маленькому изображению; это сохраняет правильную ориентацию телефона без второй полноразмерной копии. Повреждённый необязательный EXIF не отменяет декодирование валидного raster и трактуется как `TOP_LEFT`.

## Тестирование

`SidebarImageNormalizationServiceBeanTest` проверяет JPEG/BMP/GIF/PNG/WebP/TIFF, EXIF Orientation 6, 5000×5000 resize, горизонтальный и вертикальный resize, no-upscale, alpha, PNG signature и отклонение поддельного/слишком большого файла. `SidebarImageUploadContractTest` проверяет регистрацию remote proxy, whitelist generic properties, legacy datasource bindings, 15 FileDescriptor controls, отдельный Admin AI BLOB adapter, отсутствие fallback на original и фильтры всех 16 graphics controls.

## Deployment и migration

DB migration: N/A. Перед web-вызовом интерфейс должен быть зарегистрирован в `WebRemoteProxyBeanCreator` как `hunttech_SidebarImageNormalizationService`.

Production-safe precheck и verification:

1. До развёртывания убедиться, что core runtime содержит ImageIO readers для PNG/JPEG/GIF/BMP/WBMP/TIFF/WebP и PNG writer; изменение БД/данных не требуется.
2. После локального deploy проверить валидные горизонтальные/вертикальные изображения, alpha PNG, EXIF-oriented JPEG, WebP, TIFF и первый кадр GIF; выход должен иметь PNG signature и размеры не более 512×512.
3. Проверить файл с поддельным extension, повреждённый raster, >20 MiB и metadata >25 млн пикселей: новое значение не сохраняется, прежний preview остаётся.
4. Проверить document/CV attachment upload: он не должен попасть в image pipeline.

Rollback приложения — возврат предыдущей версии web/core без миграции данных. Уже сохранённые PNG остаются обычными `FileDescriptor` и не требуют обратной конвертации. Production в рамках задачи не изменяется.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-21 | Создан безопасный общий PNG pipeline для 16 graphics controls (15 FileDescriptor + Admin AI BLOB adapter); зафиксированы WebP/TIFF runtime support, JPEG EXIF Orientation, лимиты 20 MiB/25 млн пикселей, no-fallback, сохранение прежнего preview, исключение main-screen background/documents и production-safe verification/rollback. |
