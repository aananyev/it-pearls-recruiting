package com.company.itpearls.web.util;

import com.company.itpearls.entity.StdPictures;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.Resource;
import com.haulmont.cuba.gui.components.ThemeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Safe loading of {@link FileDescriptor} images in UI: checks physical file presence
 * before {@link FileDescriptorResource} to avoid UI thread failures when DB metadata
 * exists but the file is missing from {@code cuba.fileStorageDir}.
 */
public final class FileDescriptorImageHelper {

    private static final Logger log = LoggerFactory.getLogger(FileDescriptorImageHelper.class);

    private FileDescriptorImageHelper() {
    }

    public static boolean fileExists(FileLoader fileLoader, FileDescriptor fileDescriptor) {
        if (fileDescriptor == null || fileLoader == null) {
            return false;
        }
        try {
            return fileLoader.fileExists(fileDescriptor);
        } catch (FileStorageException e) {
            log.warn("Cannot check file existence for descriptor id={}: {}",
                    fileDescriptor.getId(), e.getMessage());
            return false;
        }
    }

    public static void setImageSource(Image image, FileLoader fileLoader,
                                      FileDescriptor fileDescriptor, String fallbackThemePath) {
        image.setValueSource(null);
        if (fileExists(fileLoader, fileDescriptor)) {
            image.setSource(FileDescriptorResource.class).setFileDescriptor(fileDescriptor);
        } else {
            logMissingFile(fileDescriptor);
            image.setSource(ThemeResource.class).setPath(fallbackThemePath);
        }
    }

    public static void setCandidateFace(Image image, FileLoader fileLoader, FileDescriptor fileDescriptor) {
        setImageSource(image, fileLoader, fileDescriptor, StdPictures.NO_CANDIDATE.getId());
    }

    public static void setCompanyLogo(Image image, FileLoader fileLoader, FileDescriptor fileDescriptor) {
        setImageSource(image, fileLoader, fileDescriptor, StdPictures.NO_COMPANY.getId());
    }

    public static Resource createImageResource(Image image, FileLoader fileLoader,
                                               FileDescriptor fileDescriptor, String fallbackThemePath) {
        if (fileExists(fileLoader, fileDescriptor)) {
            return image.createResource(FileDescriptorResource.class).setFileDescriptor(fileDescriptor);
        }
        logMissingFile(fileDescriptor);
        return image.createResource(ThemeResource.class).setPath(fallbackThemePath);
    }

    public static Resource createCompanyLogoResource(Image image, FileLoader fileLoader,
                                                     FileDescriptor fileDescriptor) {
        return createImageResource(image, fileLoader, fileDescriptor, StdPictures.NO_COMPANY.getId());
    }

    public static Resource createCandidateFaceResource(Image image, FileLoader fileLoader,
                                                       FileDescriptor fileDescriptor) {
        return createImageResource(image, fileLoader, fileDescriptor, StdPictures.NO_CANDIDATE.getId());
    }

    /**
     * HTML for Vaadin description tooltip: circular 300×300 preview via dispatch URL or theme placeholder.
     */
    public static String buildCandidateFacePreviewHtml(FileLoader fileLoader, FileDescriptor fileDescriptor) {
        String imageUrl = resolveCandidateFaceImageUrl(fileLoader, fileDescriptor);
        return String.format("<img class=\"candidate-face-preview-tooltip\" src=\"%s\" alt=\"\"/>", imageUrl);
    }

    public static String resolveCandidateFaceImageUrl(FileLoader fileLoader, FileDescriptor fileDescriptor) {
        if (fileExists(fileLoader, fileDescriptor)) {
            return buildDispatchDownloadUrl(fileDescriptor);
        }
        return buildThemeResourceUrl(StdPictures.NO_CANDIDATE.getId());
    }

    public static String buildDispatchDownloadUrl(FileDescriptor fileDescriptor) {
        return String.format("/%s/dispatch/download?f=%s",
                AppContext.getProperty("cuba.webContextName"),
                fileDescriptor.getUuid());
    }

    public static String buildThemeResourceUrl(String themeResourcePath) {
        String theme = AppUI.getCurrent() != null ? AppUI.getCurrent().getTheme() : "hover";
        return String.format("/%s/VAADIN/themes/%s/%s",
                AppContext.getProperty("cuba.webContextName"),
                theme,
                themeResourcePath);
    }

    private static void logMissingFile(FileDescriptor fileDescriptor) {
        if (fileDescriptor != null) {
            log.warn("File missing in storage: id={}, name={}.{}",
                    fileDescriptor.getId(), fileDescriptor.getName(), fileDescriptor.getExtension());
        }
    }
}
