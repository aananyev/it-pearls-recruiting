package com.company.itpearls.web.util;

import com.company.hunttech.app.ImageProcessingService;
import com.company.hunttech.app.ProcessedImage;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.InputStream;

public final class AvatarImageUploadHelper {

    private AvatarImageUploadHelper() {
    }

    public static FileDescriptor processUploadedImage(FileDescriptor descriptor,
                                                      FileLoader fileLoader,
                                                      FileStorageService fileStorageService,
                                                      DataManager dataManager,
                                                      ImageProcessingService imageProcessingService,
                                                      Logger log) {
        if (descriptor == null) {
            return null;
        }
        try (InputStream inputStream = fileLoader.openStream(descriptor)) {
            byte[] originalBytes = IOUtils.toByteArray(inputStream);
            ProcessedImage processed = imageProcessingService.process(originalBytes, buildFileName(descriptor));
            if (!processed.isProcessed()) {
                return descriptor;
            }

            descriptor.setExtension(processed.getExtension());
            descriptor.setSize((long) processed.getData().length);
            fileStorageService.saveFile(descriptor, processed.getData());
            return dataManager.commit(descriptor);
        } catch (FileStorageException e) {
            log.warn("Cannot save processed avatar id={}: {}", descriptor.getId(), e.getMessage());
            return descriptor;
        } catch (Exception e) {
            log.warn("Cannot process uploaded avatar id={}: {}", descriptor.getId(), e.getMessage());
            return descriptor;
        }
    }

    private static String buildFileName(FileDescriptor descriptor) {
        if (StringUtils.isNotBlank(descriptor.getExtension())) {
            return descriptor.getName() + "." + descriptor.getExtension();
        }
        return descriptor.getName();
    }
}
