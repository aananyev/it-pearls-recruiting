package com.company.hunttech.web.gui.components;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.gui.components.WebFileUploadField;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Загрузчик файлов, который перед записью в файловое хранилище обрабатывает изображение
 * логотипа проекта ({@code Project.projectLogo}), логотипа компании
 * ({@code Company.fileCompanyLogo}) или фотографии кандидата
 * ({@code JobCandidate.fileImageFace}): конвертация в PNG, ресайз до 300x300, удаление
 * белого фона, вписывание в круг (подробности — {@link ProjectLogoImageProcessingService}).
 *
 * <p>Компонент зарегистрирован в {@code cuba-ui-component.xml} под именем {@code upload},
 * поэтому бесшовно заменяет стандартный {@link WebFileUploadField} во всех экранах без
 * правки их XML. Обработка выполняется ТОЛЬКО для полей, привязанных к свойству
 * {@code projectLogo} сущности {@code com.company.hunttech.entity.Project},
 * {@code fileCompanyLogo} сущности {@code com.company.hunttech.entity.Company} или
 * {@code fileImageFace} сущности {@code com.company.hunttech.entity.JobCandidate}; все
 * остальные загрузки ведут себя точно так же, как стандартный компонент.</p>
 *
 * <p>Если фотография кандидата ({@code fileImageFace}) реально обработана нейросетью
 * (rembg/AI удалили фон), пользователю показывается исчезающая TRAY-нотификация
 * «Фотография обработана с помощью AI» (стандартный механизм CUBA). Для логотипов
 * нотификация не показывается — там фон может быть удалён классическим flood-fill.</p>
 *
 * <p>Точка перехвата — {@link #saveFile(FileDescriptor)} в режиме
 * {@link FileStoragePutMode#IMMEDIATE}: к этому моменту файл уже принят во временное
 * хранилище ({@code FileUploadingAPI}), но ещё не сохранён в {@code FileStorage}.</p>
 */
public class WebProjectLogoFileUploadField extends WebFileUploadField {

    private static final Logger log = LoggerFactory.getLogger(WebProjectLogoFileUploadField.class);

    /**
     * Имя свойства сущности Project, для которого выполняется обработка логотипа.
     */
    private static final String PROJECT_LOGO_PROPERTY = "projectLogo";

    /**
     * Имя свойства сущности Company, для которого выполняется обработка логотипа.
     */
    private static final String COMPANY_LOGO_PROPERTY = "fileCompanyLogo";

    /**
     * Имя свойства сущности JobCandidate, для которого выполняется обработка фотографии.
     */
    private static final String CANDIDATE_PHOTO_PROPERTY = "fileImageFace";

    /**
     * Дескриптор после обработки: возвращается из {@link #getFileDescriptor()}, чтобы
     * превью в экране и последующий коммит использовали обработанный файл (PNG).
     */
    private FileDescriptor processedDescriptor;

    /**
     * Флаг последней загрузки: фон изображения удалён нейросетью (rembg/AI-функция).
     * Выставляется в {@link #processLogo(FileDescriptor, ProcessingMode)} и используется
     * для уведомления пользователя об AI-обработке фотографии кандидата.
     */
    private boolean processedByAi;

    @Override
    protected void saveFile(FileDescriptor fileDescriptor) {
        // Сбрасываем кэш обработанного дескриптора при каждой новой загрузке.
        processedDescriptor = null;
        processedByAi = false;
        // Обрабатываем только изображения (логотипы проекта/компании, фото кандидата);
        // остальные загрузки — стандартное поведение.
        ProcessingMode mode = resolveProcessingMode();
        if (mode != ProcessingMode.NONE && fileDescriptor != null) {
            try {
                FileDescriptor processed = processLogo(fileDescriptor, mode);
                if (processed != null) {
                    processedDescriptor = processed;
                    super.saveFile(processed);
                    // Принудительная перерисовка виджета загрузки: клиентский RPC
                    // continueUploading() (снимает блокировку jquery-file-upload после
                    // загрузки) для legacy-компонента CubaFileUpload отправляется в браузер
                    // ТОЛЬКО при paint. Без этого повторный клик по кнопке «Загрузить»
                    // не открывает диалог выбора файла (повторная загрузка невозможна).
                    getComposition().markAsDirty();
                    if (mode == ProcessingMode.CANDIDATE_PHOTO && processedByAi) {
                        showAiProcessedNotification();
                    }
                    return;
                }
            } catch (Exception e) {
                // При любой ошибке обработки сохраняем исходный файл — загрузка не должна ломаться.
                log.warn("Не удалось обработать логотип id={}: {}", fileDescriptor.getId(), e.toString(), e);
            }
        }
        super.saveFile(fileDescriptor);
    }

    /**
     * Сбрасывает кэш обработанного дескриптора в момент начала новой загрузки.
     *
     * <p>Родительский succeeded-листенер вызывает {@code saveFile(getFileDescriptor())},
     * причём {@link #getFileDescriptor()} вычисляется ДО входа в {@link #saveFile}.
     * Без сброса здесь вторая (и последующие) загрузки логотипа в том же экране
     * получали бы дескриптор ПЕРВОЙ загрузки (тот же UUID): повторный
     * {@code putFileIntoStorage} на уже существующий файл не сохранялся,
     * {@code setValue} не обновлял контейнер, и превью/алгоритм не срабатывали.</p>
     */
    @Override
    protected OutputStream receiveUpload(String fileName, String MIMEType) {
        processedDescriptor = null;
        processedByAi = false;
        return super.receiveUpload(fileName, MIMEType);
    }

    /**
     * Возвращает обработанный дескриптор (PNG), если логотип уже был трансформирован,
     * иначе — стандартный дескриптор загруженного файла.
     */
    @Override
    public FileDescriptor getFileDescriptor() {
        if (processedDescriptor != null) {
            return processedDescriptor;
        }
        return super.getFileDescriptor();
    }

    /**
     * Режим обработки поля загрузки: какой конвейер применять к изображению.
     */
    private enum ProcessingMode {
        /** Обычная загрузка без обработки (поле не привязано к обрабатываемому свойству). */
        NONE,
        /** Логотип проекта/компании — классический конвейер (flood-fill, круг). */
        LOGO,
        /** Фото кандидата — щадящий режим (только нейросеть rembg + ресайз, без flood-fill). */
        CANDIDATE_PHOTO
    }

    /**
     * Определяет режим обработки по привязке поля: {@code projectLogo} у Project
     * или {@code fileCompanyLogo} у Company — {@link ProcessingMode#LOGO};
     * {@code fileImageFace} у JobCandidate — {@link ProcessingMode#CANDIDATE_PHOTO}.
     */
    private ProcessingMode resolveProcessingMode() {
        ValueSource<FileDescriptor> valueSource = getValueSource();
        if (valueSource instanceof ContainerValueSource) {
            ContainerValueSource<?, ?> containerSource = (ContainerValueSource<?, ?>) valueSource;
            String property = containerSource.getMetaPropertyPath().getMetaProperty().getName();
            if (PROJECT_LOGO_PROPERTY.equals(property) || COMPANY_LOGO_PROPERTY.equals(property)) {
                return ProcessingMode.LOGO;
            }
            if (CANDIDATE_PHOTO_PROPERTY.equals(property)) {
                return ProcessingMode.CANDIDATE_PHOTO;
            }
        }
        return ProcessingMode.NONE;
    }

    /**
     * Читает принятый во временное хранилище файл, обрабатывает его сервисом
     * {@link ProjectLogoImageProcessingService} и перезаписывает временный файл
     * обработанными байтами. Возвращает дескриптор с актуальными именем/расширением/размером.
     *
     * @return обработанный дескриптор, или {@code null}, если файл не является изображением
     *         (обработка не требуется)
     */
    private FileDescriptor processLogo(FileDescriptor fileDescriptor, ProcessingMode mode) throws IOException {
        ProjectLogoImageProcessingService service =
                beanLocator.get(ProjectLogoImageProcessingService.NAME);

        File tempFile = fileUploading.getFile(getFileId());
        byte[] originalBytes;
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            originalBytes = IOUtils.toByteArray(inputStream);
        }

        ProcessedImage processed = service.process(originalBytes, fileDescriptor.getName(),
                mode == ProcessingMode.CANDIDATE_PHOTO);
        if (!processed.isProcessed()) {
            return null;
        }
        processedByAi = processed.isAiProcessed();

        // Перезаписываем временный файл обработанными байтами — дальше стандартный конвейер
        // (putFileIntoStorage + commit) сохранит именно обработанное изображение.
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            outputStream.write(processed.getData());
        }

        // Обновляем метаданные дескриптора под новый формат (PNG).
        String newName = processed.getName() + "." + processed.getExtension();
        fileDescriptor.setName(newName);
        fileDescriptor.setExtension(processed.getExtension());
        fileDescriptor.setSize((long) processed.getData().length);

        log.debug("Изображение обработано: {} -> {} ({} байт)",
                fileDescriptor.getId(), newName, processed.getData().length);
        return fileDescriptor;
    }

    /**
     * Показывает исчезающую (TRAY) нотификацию о том, что фотография кандидата
     * обработана нейросетью: фон удалён автоматически (rembg/u2net или AI-функция).
     *
     * <p>Вызывается только для {@link ProcessingMode#CANDIDATE_PHOTO} при реальном
     * нейросетевом удалении фона — для логотипов обработка может быть классической
     * (flood-fill), и утверждение «обработано с помощью AI» было бы некорректным.
     * Нотификация исчезает автоматически (стандартный механизм CUBA, TRAY).</p>
     */
    private void showAiProcessedNotification() {
        AppUI appUI = AppUI.getCurrent();
        if (appUI == null) {
            log.debug("AppUI недоступен, нотификация об AI-обработке не показана");
            return;
        }
        appUI.getNotifications()
                .create(Notifications.NotificationType.TRAY)
                .withPosition(Notifications.Position.BOTTOM_RIGHT)
                .withCaption("Фотография обработана с помощью AI")
                .withDescription("Фон удалён автоматически нейросетью")
                .show();
    }
}
