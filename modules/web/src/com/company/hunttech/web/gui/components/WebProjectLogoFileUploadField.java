package com.company.hunttech.web.gui.components;

import com.company.hunttech.app.ProcessedImage;
import com.company.hunttech.app.ProjectLogoImageProcessingService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.data.ValueSource;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
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
 * логотипа проекта: конвертация в PNG, ресайз до 300x300, удаление белого фона,
 * вписывание в круг (подробности — {@link ProjectLogoImageProcessingService}).
 *
 * <p>Компонент зарегистрирован в {@code cuba-ui-component.xml} под именем {@code upload},
 * поэтому бесшовно заменяет стандартный {@link WebFileUploadField} во всех экранах без
 * правки их XML. Обработка выполняется ТОЛЬКО для полей, привязанных к свойству
 * {@code projectLogo} сущности {@code com.company.hunttech.entity.Project}; все остальные
 * загрузки ведут себя точно так же, как стандартный компонент.</p>
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
     * Дескриптор после обработки: возвращается из {@link #getFileDescriptor()}, чтобы
     * превью в экране и последующий коммит использовали обработанный файл (PNG).
     */
    private FileDescriptor processedDescriptor;

    @Override
    protected void saveFile(FileDescriptor fileDescriptor) {
        // Сбрасываем кэш обработанного дескриптора при каждой новой загрузке.
        processedDescriptor = null;
        // Обрабатываем только логотип проекта; остальные загрузки — стандартное поведение.
        if (isProjectLogoField() && fileDescriptor != null) {
            try {
                FileDescriptor processed = processLogo(fileDescriptor);
                if (processed != null) {
                    processedDescriptor = processed;
                    super.saveFile(processed);
                    return;
                }
            } catch (Exception e) {
                // При любой ошибке обработки сохраняем исходный файл — загрузка не должна ломаться.
                log.warn("Не удалось обработать логотип проекта id={}: {}", fileDescriptor.getId(), e.toString(), e);
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
     * Проверяет, привязано ли поле к свойству {@code projectLogo}.
     */
    private boolean isProjectLogoField() {
        ValueSource<FileDescriptor> valueSource = getValueSource();
        if (valueSource instanceof ContainerValueSource) {
            ContainerValueSource<?, ?> containerSource = (ContainerValueSource<?, ?>) valueSource;
            return PROJECT_LOGO_PROPERTY.equals(
                    containerSource.getMetaPropertyPath().getMetaProperty().getName());
        }
        return false;
    }

    /**
     * Читает принятый во временное хранилище файл, обрабатывает его сервисом
     * {@link ProjectLogoImageProcessingService} и перезаписывает временный файл
     * обработанными байтами. Возвращает дескриптор с актуальными именем/расширением/размером.
     *
     * @return обработанный дескриптор, или {@code null}, если файл не является изображением
     *         (обработка не требуется)
     */
    private FileDescriptor processLogo(FileDescriptor fileDescriptor) throws IOException {
        ProjectLogoImageProcessingService service =
                beanLocator.get(ProjectLogoImageProcessingService.NAME);

        File tempFile = fileUploading.getFile(getFileId());
        byte[] originalBytes;
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            originalBytes = IOUtils.toByteArray(inputStream);
        }

        ProcessedImage processed = service.process(originalBytes, fileDescriptor.getName());
        if (!processed.isProcessed()) {
            return null;
        }

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

        log.debug("Логотип проекта обработан: {} -> {} ({} байт)",
                fileDescriptor.getId(), newName, processed.getData().length);
        return fileDescriptor;
    }
}
