package com.company.hunttech.web.util;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.StdPictures;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Safe loading of {@link FileDescriptor} images in UI: checks that the physical file
 * can actually be opened before {@link FileDescriptorResource} is created. This keeps
 * stale metadata or temporarily unavailable storage from breaking the Vaadin UI thread.
 */
public final class FileDescriptorImageHelper {

    private static final Logger log = LoggerFactory.getLogger(FileDescriptorImageHelper.class);

    private FileDescriptorImageHelper() {
    }

    public static boolean fileExists(FileLoader fileLoader, FileDescriptor fileDescriptor) {
        if (fileDescriptor == null || fileLoader == null) {
            return false;
        }
        /*
         * FileDescriptor confirms only metadata in the database. FileLoader.openStream()
         * verifies the same path that FileDescriptorResource will read and therefore also
         * catches unavailable storage, missing physical files and broken storage routing.
         * The stream is not copied into memory and is closed immediately after validation.
         */
        try (InputStream stream = fileLoader.openStream(fileDescriptor)) {
            return stream != null;
        } catch (FileStorageException | IOException e) {
            log.warn("Cannot open file from storage for descriptor id={}: {}",
                    fileDescriptor.getId(), e.getMessage());
            return false;
        }
    }

    public static void setImageSource(Image image, FileLoader fileLoader,
                                      FileDescriptor fileDescriptor, String fallbackThemePath) {
        image.setValueSource(null);
        if (fileDescriptor != null && fileExists(fileLoader, fileDescriptor)) {
            if (isSameFileDescriptorSource(image, fileDescriptor)) {
                return;
            }
            try {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(fileDescriptor);
                return;
            } catch (RuntimeException e) {
                log.warn("Cannot set FileDescriptorResource for id={}: {}. Falling back to theme.",
                        fileDescriptor.getId(), e.getMessage());
            }
        }
        if (isSameThemeSource(image, fallbackThemePath)) {
            return;
        }
        logMissingFile(fileDescriptor);
        image.setSource(ThemeResource.class).setPath(fallbackThemePath);
    }

    private static boolean isSameFileDescriptorSource(Image image, FileDescriptor fileDescriptor) {
        Resource source = image.getSource();
        if (!(source instanceof FileDescriptorResource)) {
            return false;
        }
        FileDescriptor current = ((FileDescriptorResource) source).getFileDescriptor();
        return current != null && fileDescriptor != null
                && Objects.equals(current.getId(), fileDescriptor.getId());
    }

    private static boolean isSameThemeSource(Image image, String themePath) {
        Resource source = image.getSource();
        return source instanceof ThemeResource
                && Objects.equals(themePath, ((ThemeResource) source).getPath());
    }

    public static void setCandidateFace(Image image, FileLoader fileLoader, FileDescriptor fileDescriptor) {
        setImageSource(image, fileLoader, fileDescriptor, StdPictures.NO_CANDIDATE.getId());
    }

    public static void setUserProfilePhoto(Image image, FileLoader fileLoader, ExtUser user) {
        FileDescriptor photo = user != null ? user.resolveProfilePhoto() : null;
        if (photo != null && !fileExists(fileLoader, photo) && user != null) {
            if (user.getOfficialPhoto() != null && fileExists(fileLoader, user.getOfficialPhoto())) {
                photo = user.getOfficialPhoto();
            } else if (user.getFileImageFace() != null && fileExists(fileLoader, user.getFileImageFace())) {
                photo = user.getFileImageFace();
            }
        }
        setImageSource(image, fileLoader, photo, StdPictures.NO_CANDIDATE.getId());
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
            log.warn("File missing or unreadable in storage: id={}, name={}.{}",
                    fileDescriptor.getId(), fileDescriptor.getName(), fileDescriptor.getExtension());
        }
    }
}
