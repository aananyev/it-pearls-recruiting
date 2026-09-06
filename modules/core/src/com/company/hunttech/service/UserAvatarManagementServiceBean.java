package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.service.dto.avatar.AvatarApplyMode;
import com.company.hunttech.service.dto.avatar.AvatarSourceType;
import com.company.hunttech.service.dto.avatar.ResolvedAvatarInfo;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Реализация сервиса управления и разрешения аватаров пользователей HRM HuntTech.
 */
@Service(UserAvatarManagementService.NAME)
public class UserAvatarManagementServiceBean implements UserAvatarManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserAvatarManagementServiceBean.class);

    @Inject
    private DataManager dataManager;

    @Inject
    private Metadata metadata;

    @Inject
    private FileLoader fileLoader;

    @Override
    public ResolvedAvatarInfo resolveEffectiveAvatar(ExtUser user) {
        if (user == null) {
            return new ResolvedAvatarInfo(null, AvatarSourceType.THEME_DEFAULT, true, ResolvedAvatarInfo.DEFAULT_THEME_FALLBACK_PATH);
        }

        FileDescriptor userAvatar = user.getUserAvatar();
        if (userAvatar != null && isFileAvailable(userAvatar)) {
            return new ResolvedAvatarInfo(userAvatar, AvatarSourceType.USER_PERSONAL, false, null);
        }

        FileDescriptor officialPhoto = user.getOfficialPhoto();
        if (officialPhoto != null && isFileAvailable(officialPhoto)) {
            return new ResolvedAvatarInfo(officialPhoto, AvatarSourceType.ADMIN_OFFICIAL, true, null);
        }

        FileDescriptor legacyPhoto = user.getFileImageFace();
        if (legacyPhoto != null && isFileAvailable(legacyPhoto)) {
            return new ResolvedAvatarInfo(legacyPhoto, AvatarSourceType.LEGACY_PHOTO, true, null);
        }

        return new ResolvedAvatarInfo(null, AvatarSourceType.THEME_DEFAULT, true, ResolvedAvatarInfo.DEFAULT_THEME_FALLBACK_PATH);
    }

    @Override
    public ResolvedAvatarInfo resolveEffectiveAvatar(UUID userId) {
        if (userId == null) {
            return new ResolvedAvatarInfo(null, AvatarSourceType.THEME_DEFAULT, true, ResolvedAvatarInfo.DEFAULT_THEME_FALLBACK_PATH);
        }
        ExtUser user = dataManager.load(ExtUser.class)
                .id(userId)
                .view("extUser-picker-view")
                .optional()
                .orElse(null);
        return resolveEffectiveAvatar(user);
    }

    @Override
    public ExtUser applyUserPersonalAvatar(ExtUser user, FileDescriptor uploadedDescriptor) {
        if (user == null) {
            return null;
        }
        if (uploadedDescriptor == null) {
            return clearUserPersonalAvatar(user);
        }
        FileDescriptor oldPersonal = user.getUserAvatar();
        user.setUserAvatar(uploadedDescriptor);

        syncUserSettingsAvatar(user, uploadedDescriptor);

        if (oldPersonal != null) {
            cleanupUnreferencedFile(oldPersonal, user.getOfficialPhoto(), uploadedDescriptor);
        }

        return user;
    }

    @Override
    public ExtUser clearUserPersonalAvatar(ExtUser user) {
        if (user == null) {
            return null;
        }
        FileDescriptor oldPersonal = user.getUserAvatar();
        user.setUserAvatar(null);

        syncUserSettingsAvatar(user, null);

        if (oldPersonal != null) {
            cleanupUnreferencedFile(oldPersonal, user.getOfficialPhoto());
        }

        return user;
    }

    @Override
    public ExtUser applyAdminOfficialPhoto(ExtUser user, FileDescriptor uploadedDescriptor, AvatarApplyMode mode) {
        if (user == null) {
            return null;
        }
        if (uploadedDescriptor == null) {
            return clearAdminOfficialPhoto(user);
        }
        if (mode == null) {
            mode = AvatarApplyMode.SMART_DEFAULT;
        }

        FileDescriptor oldOfficial = user.getOfficialPhoto();
        user.setOfficialPhoto(uploadedDescriptor);

        boolean overwritePersonal = false;
        if (mode == AvatarApplyMode.OVERWRITE_ALL) {
            overwritePersonal = true;
        } else if (mode == AvatarApplyMode.SMART_DEFAULT) {
            overwritePersonal = (user.getUserAvatar() == null);
        }

        FileDescriptor oldPersonal = user.getUserAvatar();
        if (overwritePersonal) {
            user.setUserAvatar(uploadedDescriptor);
            syncUserSettingsAvatar(user, uploadedDescriptor);
            if (oldPersonal != null) {
                cleanupUnreferencedFile(oldPersonal, oldOfficial, uploadedDescriptor);
            }
        }

        if (oldOfficial != null) {
            cleanupUnreferencedFile(oldOfficial, user.getUserAvatar(), uploadedDescriptor);
        }

        return user;
    }

    @Override
    public ExtUser clearAdminOfficialPhoto(ExtUser user) {
        if (user == null) {
            return null;
        }
        FileDescriptor oldOfficial = user.getOfficialPhoto();
        user.setOfficialPhoto(null);

        if (oldOfficial != null) {
            cleanupUnreferencedFile(oldOfficial, user.getUserAvatar());
        }

        return user;
    }

    @Override
    public void cleanupUnreferencedFile(FileDescriptor candidateForDeletion, FileDescriptor... activeReferences) {
        if (candidateForDeletion == null || candidateForDeletion.getId() == null) {
            return;
        }

        if (activeReferences != null) {
            for (FileDescriptor activeRef : activeReferences) {
                if (activeRef != null && Objects.equals(candidateForDeletion.getId(), activeRef.getId())) {
                    log.debug("Файл id={} не удаляется, так как все еще используется в ссылке {}",
                            candidateForDeletion.getId(), activeRef.getId());
                    return;
                }
            }
        }

        try {
            if (fileLoader != null) {
                fileLoader.removeFile(candidateForDeletion);
                log.info("Файл id={} успешно удален из FileStorage", candidateForDeletion.getId());
            }
        } catch (Exception e) {
            log.warn("Не удалось удалить файл id={} из FileStorage: {}", candidateForDeletion.getId(), e.getMessage());
        }

        try {
            if (dataManager != null) {
                dataManager.remove(candidateForDeletion);
                log.info("FileDescriptor id={} успешно удален из базы данных", candidateForDeletion.getId());
            }
        } catch (Exception e) {
            log.warn("Не удалось удалить FileDescriptor id={} из базы данных: {}", candidateForDeletion.getId(), e.getMessage());
        }
    }

    private void syncUserSettingsAvatar(ExtUser user, FileDescriptor newAvatar) {
        if (user == null || user.getId() == null || dataManager == null) {
            return;
        }
        try {
            UserSettings settings = dataManager.load(UserSettings.class)
                    .query("select s from hunttech_UserSettings s where s.user.id = :userId")
                    .parameter("userId", user.getId())
                    .optional()
                    .orElse(null);

            if (settings == null) {
                if (newAvatar == null) {
                    return;
                }
                if (metadata != null) {
                    settings = metadata.create(UserSettings.class);
                    settings.setUser(user);
                } else {
                    return;
                }
            }

            settings.setFileImageFace(newAvatar);
            dataManager.commit(settings);
            log.debug("Синхронизировано UserSettings.fileImageFace для пользователя {}", user.getLogin());
        } catch (Exception e) {
            log.error("Не удалось синхронизировать UserSettings для пользователя id={}: {}", user.getId(), e.getMessage(), e);
        }
    }

    protected boolean isFileAvailable(FileDescriptor fd) {
        if (fd == null || fileLoader == null) {
            return false;
        }
        try (InputStream stream = fileLoader.openStream(fd)) {
            return stream != null;
        } catch (Exception e) {
            log.debug("Файл id={} недоступен в FileStorage: {}", fd.getId(), e.getMessage());
            return false;
        }
    }
}
