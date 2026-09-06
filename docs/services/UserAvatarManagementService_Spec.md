# UserAvatarManagementService — Архитектурная спецификация и бизнес-логика управления аватарами пользователей

> **Роль составителя**: Аналитик  
> **Статус**: Согласовано  
> **Область применения**: HRM HuntTech (CUBA Platform 7.3 / Vaadin / Core & Web Modules)  
> **Ключевые сущности**: `ExtUser`, `UserSettings`, `FileDescriptor`  
> **Ключевые экраны**: `ExtSettingsWindow` (личный кабинет/настройки), `ExtUserEditor` / `ExtUserEdit` (администрирование пользователей)  

---

## 1. Введение и бизнес-цели (Business & Context Intro)

### 1.1. Проблематика
В HRM HuntTech существуют два независимых контура управления фотографией пользователя:
1. **Пользовательский контур** (`ExtSettingsWindow`): каждый сотрудник в своем личном кабинете на вкладке «Обо мне» может загрузить персональный аватар, отражающий его личные предпочтения и индивидуальность.
2. **Административный контур** (`ExtUserEditor` / `ExtUserEdit`): администратор системы при создании или редактировании учетной записи сотрудника загружает утвержденное официальное фото (корпоративный портрет, фото из личного дела или фото, выгруженное из Telegram).

Кроме того, исторически в системе присутствуют две сущности:
- `ExtUser` (расширение `sec$User`), содержащая поля `userAvatar`, `officialPhoto` и устаревшее `fileImageFace`.
- `UserSettings` (1:1 к `ExtUser`), содержащая поле `fileImageFace`, использовавшееся в ранних версиях платформы.

### 1.2. Бизнес-цели и ключевые принципы
1. **Безусловный приоритет личного выбора сотрудника**: если пользователь загрузил свой персональный аватар, именно он должен отображаться во всех экранах системы (в шапке, меню, карточках кандидатов, реестрах, чатах и авторстве комментариев).
2. **Корпоративный Fallback (страхующая фотография)**: если пользователь еще не загрузил личный аватар или удалил его, система автоматически и бесшовно отображает официальное фото, установленное администратором.
3. **Дефолтный UI-образ (Theme Placeholder)**: если ни пользователь, ни администратор не загрузили изображение (или файл физически отсутствует в хранилище), отображается стандартный овальный силуэт темы оформления (`icons/no-programmer.jpeg`).
4. **Суверенность данных и прозрачность для администратора**:
   - Администратор может загружать и обновлять официальное фото сотрудника, не затирая «втихую» личный аватар пользователя.
   - При попытке администратора принудительно изменить внешний вид пользователя администратору предоставляется явный выбор: обновить только корпоративное фото (останется в резерве) или принудительно синхронизировать и перезаписать личный аватар.
5. **Безопасность файлового хранилища (Zero Storage Leaks & Zero Broken References)**: при смене или удалении аватаров не должно возникать битых ссылок на удаленные файлы и потерянных файлов-сирот в `FileStorage`.

---

## 2. Модель данных и источники изображений

| Источник | Поле сущности | Роль / Назначение | Кто управляет |
|---|---|---|---|
| **Личный аватар (USER_PERSONAL)** | `ExtUser.userAvatar`<br/>(зеркалируется в `UserSettings.fileImageFace`) | Личный аватар сотрудника, высший приоритет отображения. | Пользователь в `ExtSettingsWindow` (или администратор при явном согласии). |
| **Официальное фото (ADMIN_OFFICIAL)** | `ExtUser.officialPhoto` | Корпоративное фото из личного дела или Telegram. Второй приоритет (fallback). | Администратор системы в `ExtUserEditor` / `ExtUserEdit`. |
| **Устаревший аватар (LEGACY_PHOTO)** | `ExtUser.fileImageFace` *(deprecated)* | Историческое фото до внедрения разделения аватаров. Третий приоритет. | Миграционные скрипты БД. |
| **Системная заглушка (THEME_DEFAULT)** | Ресурс темы (`icons/no-programmer.jpeg`) | Векторная/растровая заглушка по умолчанию. Четвертый приоритет. | Тема оформления Vaadin/CUBA. |

---

## 3. Стратегия разрешения аватара (Resolution Strategy Matrix)

Система определяет эффективный аватар (`Effective Avatar`) на основе каскадного алгоритма:

```
[Пользователь загрузил userAvatar?]
       │
       ├── ДА ──► [Файл существует в FileStorage?] ──► ДА ──► Отображаем USER_PERSONAL
       │                                            │
       │                                            └── НЕТ (битый) ──┐
       └── НЕТ ───────────────────────────────────────────────────────┤
                                                                      ▼
                                                [Администратор загрузил officialPhoto?]
                                                       │
                                                       ├── ДА ──► [Файл существует в FileStorage?] ──► ДА ──► Отображаем ADMIN_OFFICIAL
                                                       │                                            │
                                                       │                                            └── НЕТ (битый) ──┐
                                                       └── НЕТ ───────────────────────────────────────────────────────┤
                                                                                                                      ▼
                                                                                                [Есть legacy fileImageFace?]
                                                                                                       │
                                                                                                       ├── ДА ──► [Файл существует?] ──► ДА ──► Отображаем LEGACY_PHOTO
                                                                                                       │                                │
                                                                                                       └── НЕТ ─────────────────────────┴──► Отображаем THEME_DEFAULT
```

### 3.1. Полная матрица комбинаций состояний (All Combinations)

| Вариант | Личный аватар (`userAvatar`) | Официальное фото (`officialPhoto`) | Что отображается в UI | Источник (`AvatarSourceType`) | Описание поведения системы |
|:---:|:---:|:---:|:---:|:---:|:---|
| **К-1** | **Загружен** | **Загружен** | `userAvatar` | `USER_PERSONAL` | **Приоритет пользователя**. Личный аватар активен во всех экранных формах. Официальное фото хранится в профиле сотрудника как резервное и кадровое. |
| **К-2** | **Загружен** | **Не загружен** | `userAvatar` | `USER_PERSONAL` | Сотрудник самостоятельно оформил профиль. Кадровое фото отсутствует. Отображается личный аватар. |
| **К-3** | **Не загружен** | **Загружен** | `officialPhoto` | `ADMIN_OFFICIAL` | **Кадровый Fallback**. Сотрудник не загружал свое изображение; автоматически отображается фото, установленное администратором. |
| **К-4** | **Не загружен** | **Не загружен** | Заглушка темы | `THEME_DEFAULT` | Ни одно фото не загружено. Компонент `OvaFallbackImage` отрисовывает нейтральный овальный силуэт `icons/no-programmer.jpeg`. |
| **К-5 (Крайний)** | **Загружен, но удален из диска (битый ID)** | **Загружен и валиден** | `officialPhoto` | `ADMIN_OFFICIAL` (Fallback) | Self-healing: обнаружив отсутствие файла `userAvatar` в `FileStorage`, система не падает с ошибкой, а автоматически переключается на `officialPhoto`. |
| **К-6 (Крайний)** | **Загружен, но битый** | **Не загружен** | Заглушка темы | `THEME_DEFAULT` | Ошибка физического файла перехватывается, система показывает заглушку без поломки верстки. |

---

## 4. Матрица жизненного цикла и событийных переходов (Event Transition Matrix)

### 4.1. Событие: Пользователь загружает личный аватар в `ExtSettingsWindow`
- **Предусловие**: Пользователь находится в экране настроек, перетаскивает или выбирает изображение в `userAvatarUpload`.
- **Действия системы**:
  1. Выполняется валидация формата и размера изображения через `AvatarImageUploadHelper`.
  2. Изображение оптимизируется сервисом `ImageProcessingService` (масштабирование до лимитов `HunttechImageConfig`).
  3. Старый файл `userAvatar` удаляется из `FileStorage`, если на него больше нет ссылок из `officialPhoto`.
  4. Полю `ExtUser.userAvatar` присваивается новый `FileDescriptor`.
  5. Синхронизируется сущность `UserSettings` текущего пользователя (`userSettings.setFileImageFace(newAvatar)`).
  6. Сайдбар `ExtSettingsWindow` и шапка системы мгновенно перерисовывают аватар.
- **Влияние на `officialPhoto`**: Поле `officialPhoto` **НЕ изменяется**. Корпоративное фото сохраняется нетронутым.

### 4.2. Событие: Пользователь удаляет личный аватар в `ExtSettingsWindow` (кнопка «Очистить»)
- **Предусловие**: Пользователь нажимает кнопку очистки поля загрузки аватара.
- **Действия системы**:
  1. Старый `userAvatar` удаляется из `FileStorage`, если на него нет ссылки из `officialPhoto`.
  2. `ExtUser.userAvatar` устанавливается в `null`.
  3. `UserSettings.fileImageFace` устанавливается в `null`.
  4. Происходит вызов `refreshProfilePhoto()`.
- **Результат в UI**:
  - Если у пользователя **было** `officialPhoto`, система моментально начинает отображать `officialPhoto` (мягкий откат).
  - Если `officialPhoto` **не было**, отображается заглушка темы `icons/no-programmer.jpeg`.

### 4.3. Событие: Администратор загружает фото в карточке пользователя `ExtUserEditor`
- **Предусловие**: Администратор редактирует пользователя и загружает изображение в `officialPhotoUpload`.
- **Сценарии ветвления**:
  - **Сценарий А: У пользователя НЕТ личного аватара (`userAvatar == null`)**:
    - Новое фото сохраняется в `ExtUser.officialPhoto`.
    - Для обратной совместимости и немедленной видимости система также проставляет `ExtUser.userAvatar = newPhoto` и синхронизирует `UserSettings.fileImageFace`.
    - Пользователь сразу видит это фото в своем профиле.
  - **Сценарий Б: У пользователя УЖЕ ЕСТЬ личный аватар (`userAvatar != null`)**:
    - Система обнаруживает коллизию и выводит модальный диалог выбора стратегии:
      > **Заголовок**: Изменение фото пользователя  
      > **Сообщение**: «У пользователя **{ФИО}** уже установлен персональный аватар в личных настройках. Как применить загруженное фото?»  
      > **Опция 1 («Сохранить как официальное, не трогая личный аватар» — Рекомендуется)**:
      > - `officialPhoto` = новое фото.
      > - `userAvatar` остается без изменений. В системе продолжает отображаться личный аватар пользователя, а официальное фото сохранено в кадровых данных.
      > **Опция 2 («Принудительно перезаписать и личный аватар»)**:
      > - `officialPhoto` = новое фото.
      > - `userAvatar` = новое фото.
      > - `UserSettings.fileImageFace` = новое фото.
      > **Опция 3 («Отмена»)**:
      > - Загрузка отменяется, файл удаляется.

### 4.4. Событие: Администратор удаляет официальное фото в `ExtUserEditor`
- **Действия системы**:
  1. `ExtUser.officialPhoto` устанавливается в `null`.
  2. Файл физически удаляется из `FileStorage` только в том случае, если на него не ссылается `userAvatar`.
  3. `ExtUser.userAvatar` **НЕ затрагивается** (личный аватар сотрудника остается неприкосновенным).

### 4.5. Событие: Импорт фото из Telegram (`fetchTelegramPhoto`)
- **Предусловие**: Администратор нажимает кнопку «Загрузить фото из Telegram».
- **Действия системы**:
  - Сервис загружает фото профиля из Telegram API в `FileDescriptor`.
  - Действуют те же правила, что и в п. 4.3: если у пользователя уже был личный аватар, Telegram-фото назначается как `officialPhoto`, а перезапись `userAvatar` запрашивается через подтверждение.

---

## 5. Архитектура сервиса: `UserAvatarManagementService`

### 5.1. Интерфейс сервиса (`modules/global/src/.../service/UserAvatarManagementService.java`)

```java
package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.service.dto.avatar.AvatarApplyMode;
import com.company.hunttech.service.dto.avatar.ResolvedAvatarInfo;
import com.haulmont.cuba.core.entity.FileDescriptor;

import java.util.UUID;

public interface UserAvatarManagementService {
    String NAME = "hunttech_UserAvatarManagementService";

    /**
     * Разрешает эффективный аватар пользователя с учетом приоритетов и проверки целостности в FileStorage.
     */
    ResolvedAvatarInfo resolveEffectiveAvatar(ExtUser user);

    /**
     * Разрешает аватар по UUID пользователя (для фоновых задач, REST API и сервисов уведомлений).
     */
    ResolvedAvatarInfo resolveEffectiveAvatar(UUID userId);

    /**
     * Сохранение персонального аватара пользователем (из ExtSettingsWindow).
     */
    ExtUser applyUserPersonalAvatar(ExtUser user, FileDescriptor uploadedDescriptor);

    /**
     * Удаление персонального аватара пользователем (откат к officialPhoto или дефолту).
     */
    ExtUser clearUserPersonalAvatar(ExtUser user);

    /**
     * Сохранение фото администратором (из ExtUserEditor) с явным указанием режима применения.
     */
    ExtUser applyAdminOfficialPhoto(ExtUser user, FileDescriptor uploadedDescriptor, AvatarApplyMode mode);

    /**
     * Удаление официального фото администратором.
     */
    ExtUser clearAdminOfficialPhoto(ExtUser user);

    /**
     * Удаление старого файла из FileStorage при условии отсутствия других ссылок.
     */
    void cleanupUnreferencedFile(FileDescriptor candidateForDeletion, FileDescriptor... activeReferences);
}
```

### 5.2. DTO и перечисления (`modules/global/src/.../service/dto/avatar/`)

```java
package com.company.hunttech.service.dto.avatar;

public enum AvatarSourceType {
    USER_PERSONAL,   // Загружен пользователем (высший приоритет)
    ADMIN_OFFICIAL,  // Официальное фото администратора / Telegram (fallback)
    LEGACY_PHOTO,    // Историческое поле fileImageFace
    THEME_DEFAULT    // Стандартный силуэт темы оформления
}
```

```java
package com.company.hunttech.service.dto.avatar;

public enum AvatarApplyMode {
    OFFICIAL_ONLY,          // Обновить только официальное фото (личный аватар не трогать)
    OVERWRITE_ALL,          // Принудительно обновить и официальное фото, и личный аватар
    SMART_DEFAULT           // Если у пользователя нет личного аватара -> обновить оба; если есть -> спросить
}
```

```java
package com.company.hunttech.service.dto.avatar;

import com.haulmont.cuba.core.entity.FileDescriptor;
import java.io.Serializable;

public class ResolvedAvatarInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FileDescriptor fileDescriptor;
    private final AvatarSourceType sourceType;
    private final boolean fallbackUsed;
    private final String fallbackThemePath;

    public ResolvedAvatarInfo(FileDescriptor fileDescriptor, AvatarSourceType sourceType,
                              boolean fallbackUsed, String fallbackThemePath) {
        this.fileDescriptor = fileDescriptor;
        this.sourceType = sourceType;
        this.fallbackUsed = fallbackUsed;
        this.fallbackThemePath = fallbackThemePath;
    }

    public FileDescriptor getFileDescriptor() { return fileDescriptor; }
    public AvatarSourceType getSourceType() { return sourceType; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public String getFallbackThemePath() { return fallbackThemePath; }
}
```

---

## 6. Безопасность и целостность файлового хранилища (Storage Integrity)

Для исключения ситуаций удаления используемого файла (когда один и тот же `FileDescriptor` назначен и как `userAvatar`, и как `officialPhoto`), удаление выполняется строго через метод:

```java
public void cleanupUnreferencedFile(FileDescriptor candidateForDeletion, FileDescriptor... activeReferences) {
    if (candidateForDeletion == null) return;
    
    // Проверяем, не совпадает ли удаляемый файл с активными ссылками
    for (FileDescriptor activeRef : activeReferences) {
        if (activeRef != null && Objects.equals(candidateForDeletion.getId(), activeRef.getId())) {
            log.debug("Файл id={} не удаляется, так как все еще используется в ссылке {}", candidateForDeletion.getId(), activeRef);
            return;
        }
    }
    
    try {
        fileStorageService.removeFile(candidateForDeletion);
        dataManager.remove(candidateForDeletion);
        log.info("Файл id={} успешно удален из FileStorage", candidateForDeletion.getId());
    } catch (FileStorageException e) {
        log.warn("Не удалось удалить файл id={}: {}", candidateForDeletion.getId(), e.getMessage());
    }
}
```

---

## 7. План внедрения и изменения в кодовой базе

1. **Создание сервиса в модуле `global` и `core`**:
   - Интерфейс `UserAvatarManagementService` + DTO `ResolvedAvatarInfo`, `AvatarSourceType`, `AvatarApplyMode`.
   - Реализация `UserAvatarManagementServiceBean` в `modules/core`.
   - Регистрация в `web-spring.xml` для доступа из UI-слоя.
2. **Рефакторинг `ExtUser.java`**:
   - Метод `resolveProfilePhoto()` делегирует логику проверки приоритетов (сначала `userAvatar`, затем `officialPhoto`, затем `fileImageFace`).
3. **Рефакторинг `ExtSettingsWindow.java`**:
   - Методы `onUserAvatarUploaded()` и `onUserAvatarCleared()` используют `UserAvatarManagementService`.
4. **Рефакторинг `ExtUserEditor.java` и `ExtUserEdit.java`**:
   - `fetchTelegramPhoto()` переключается на стратегию `AvatarApplyMode`.
   - `officialPhotoUpload` использует `UserAvatarManagementService` и диалог подтверждения при наличии личного аватара у пользователя.
5. **Автотесты (Data View & Strategy Integrity)**:
   - Модульный тест `UserAvatarManagementServiceTest` в `modules/core/test`, проверяющий все 4 комбинации состояний матрицы, откат (fallback), обработку битых дескрипторов и очистку неиспользуемых файлов.

---

## 8. Итоги и заключение
Предложенная стратегия:
- Гарантирует уважение выбора пользователя (личный аватар всегда в приоритете).
- Предоставляет надежный кадровый fallback в виде официального фото администратора.
- Исключает случайное уничтожение личных настроек сотрудника административными действиями.
- Обеспечивает полную целостность данных в соответствии с контрактом Data View Integrity и стандартами HRM HuntTech.
