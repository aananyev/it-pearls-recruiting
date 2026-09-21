package com.company.hunttech.web.gui.components;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.SidebarImageNormalizationService;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.Notifications;
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
 * Общий upload-компонент sidebar/profile изображений.
 *
 * <p>Для известных image-binding содержимое проверяется middleware-сервисом по
 * фактическому raster codec, безопасно уменьшается и всегда сохраняется как PNG.
 * Невалидное изображение не попадает в FileStorage и не заменяет прежнее значение
 * поля. Остальные file upload bindings сохраняют стандартное поведение CUBA.</p>
 */
public class WebProjectLogoFileUploadField extends WebFileUploadField {

    private static final Logger log = LoggerFactory.getLogger(WebProjectLogoFileUploadField.class);

    private static final String PROJECT_LOGO_PROPERTY = "projectLogo";
    private static final String COMPANY_LOGO_PROPERTY = "fileCompanyLogo";
    private static final String CITY_EMBLEM_PROPERTY = "fileCityEmblem";
    private static final String REGION_EMBLEM_PROPERTY = "fileRegionEmblem";
    private static final String CANDIDATE_PHOTO_PROPERTY = "fileImageFace";
    private static final String POSITION_ICON_PROPERTY = "filePositionIcon";
    private static final String SKILL_LOGO_PROPERTY = "fileImageLogo";
    private static final String COUNTRY_FLAG_PROPERTY = "fileFlag";
    private static final String SOCIAL_NETWORK_LOGO_PROPERTY = "logo";
    private static final String OFFICIAL_PHOTO_PROPERTY = "officialPhoto";
    private static final String USER_AVATAR_PROPERTY = "userAvatar";
    private static final String APPLICATION_LOGO_PROPERTY = "applicationLogo";
    private static final String APPLICATION_ICON_PROPERTY = "applicationIcon";

    /** Дескриптор PNG, который должен получить binding после успешной нормализации. */
    private FileDescriptor processedDescriptor;
    /** Не даёт parent succeeded-listener подменить preview отклонённым original-файлом. */
    private boolean uploadRejected;

    @Override
    protected void saveFile(FileDescriptor fileDescriptor) {
        processedDescriptor = null;
        if (!isSidebarImageBinding()) {
            super.saveFile(fileDescriptor);
            return;
        }
        if (fileDescriptor == null) {
            rejectImageUpload(null, null);
            return;
        }

        try {
            FileDescriptor normalized = normalizeImage(fileDescriptor);
            processedDescriptor = normalized;
            super.saveFile(normalized);
            // Повторная загрузка должна оставаться доступной после client RPC succeeded.
            getComposition().markAsDirty();
        } catch (Exception ex) {
            rejectImageUpload(fileDescriptor, ex);
        }
    }

    /** Сбрасывает cached descriptor до вычисления getFileDescriptor() для новой загрузки. */
    @Override
    protected OutputStream receiveUpload(String fileName, String MIMEType) {
        processedDescriptor = null;
        uploadRejected = false;
        return super.receiveUpload(fileName, MIMEType);
    }

    @Override
    public FileDescriptor getFileDescriptor() {
        if (uploadRejected) {
            return getValue();
        }
        return processedDescriptor != null ? processedDescriptor : super.getFileDescriptor();
    }

    /**
     * CUBA fires upload-succeeded after saveFile returns even when our validation rejected the image.
     * Suppression prevents downstream listeners from treating the previous descriptor as a new upload.
     */
    @Override
    protected void fireFileUploadSucceed(String fileName, long contentLength) {
        if (uploadRejected && isSidebarImageBinding()) {
            return;
        }
        super.fireFileUploadSucceed(fileName, contentLength);
    }

    /**
     * Делегирует декодирование middleware, затем заменяет временный upload-файл
     * нормализованными PNG-байтами, сохраняя прежнюю FileDescriptor-модель.
     */
    private FileDescriptor normalizeImage(FileDescriptor fileDescriptor) throws IOException {
        File tempFile = fileUploading.getFile(getFileId());
        if (tempFile == null) {
            throw new IOException("Temporary upload file is unavailable");
        }

        byte[] originalBytes;
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            originalBytes = IOUtils.toByteArray(inputStream);
        }

        SidebarImageNormalizationService service =
                beanLocator.get(SidebarImageNormalizationService.NAME);
        ProcessedImage processed = service.normalize(originalBytes, fileDescriptor.getName());
        if (processed == null || !processed.isProcessed() || processed.getData() == null) {
            throw new IOException("Sidebar image normalization returned no PNG data");
        }

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            outputStream.write(processed.getData());
        }

        String normalizedName = processed.getName() + ".png";
        fileDescriptor.setName(normalizedName);
        fileDescriptor.setExtension("png");
        fileDescriptor.setSize((long) processed.getData().length);
        fileName = normalizedName;
        return fileDescriptor;
    }

    /**
     * Удаляет отвергнутый temporary upload и оставляет текущее bound-значение без
     * изменений. Исходник намеренно не передаётся в super.saveFile().
     */
    private void rejectImageUpload(FileDescriptor fileDescriptor, Exception cause) {
        processedDescriptor = null;
        uploadRejected = true;
        FileDescriptor currentValue = getValue();
        fileName = currentValue == null ? null : currentValue.getName();
        try {
            if (getFileId() != null) {
                fileUploading.deleteFile(getFileId());
            }
        } catch (Exception cleanupError) {
            log.warn("Не удалось удалить отклонённый временный image upload", cleanupError);
        }

        if (cause == null) {
            log.warn("Отклонён пустой sidebar image upload");
        } else {
            log.warn("Отклонён небезопасный sidebar image upload id={}",
                    fileDescriptor == null ? null : fileDescriptor.getId(), cause);
        }

        AppUI appUI = AppUI.getCurrent();
        if (appUI != null) {
            appUI.getNotifications()
                    .create(Notifications.NotificationType.ERROR)
                    .withCaption("Изображение не загружено")
                    .withDescription("Выберите PNG, JPEG, GIF, BMP, WBMP, WebP или TIFF размером до "
                            + (SidebarImageNormalizationService.MAX_INPUT_BYTES / (1024 * 1024))
                            + " МБ и разрешением до "
                            + (SidebarImageNormalizationService.MAX_PIXELS / 1_000_000L)
                            + " млн пикселей.")
                    .show();
        }
        getComposition().markAsDirty();
    }

    /** Определяет только image-specific bindings; документы и вложения не затрагиваются. */
    private boolean isSidebarImageBinding() {
        // DatasourceComponent.getMetaProperty() одинаково поддерживает современный
        // ContainerValueSource и legacy DatasourceValueSource (ExtUser/settings).
        MetaProperty metaProperty = getMetaProperty();
        if (metaProperty == null) {
            return false;
        }
        String property = metaProperty.getName();
        return PROJECT_LOGO_PROPERTY.equals(property)
                || COMPANY_LOGO_PROPERTY.equals(property)
                || CITY_EMBLEM_PROPERTY.equals(property)
                || REGION_EMBLEM_PROPERTY.equals(property)
                || CANDIDATE_PHOTO_PROPERTY.equals(property)
                || POSITION_ICON_PROPERTY.equals(property)
                || SKILL_LOGO_PROPERTY.equals(property)
                || COUNTRY_FLAG_PROPERTY.equals(property)
                || SOCIAL_NETWORK_LOGO_PROPERTY.equals(property)
                || OFFICIAL_PHOTO_PROPERTY.equals(property)
                || USER_AVATAR_PROPERTY.equals(property)
                || APPLICATION_LOGO_PROPERTY.equals(property)
                || APPLICATION_ICON_PROPERTY.equals(property);
    }
}
