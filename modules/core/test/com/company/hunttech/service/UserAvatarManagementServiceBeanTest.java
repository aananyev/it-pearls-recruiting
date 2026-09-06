package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserSettings;
import com.company.hunttech.service.dto.avatar.AvatarApplyMode;
import com.company.hunttech.service.dto.avatar.AvatarSourceType;
import com.company.hunttech.service.dto.avatar.ResolvedAvatarInfo;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UserAvatarManagementServiceBeanTest {

    private UserAvatarManagementServiceBean service;
    private DataManager dataManager;
    private Metadata metadata;
    private FileLoader fileLoader;

    @Before
    public void setUp() {
        service = new UserAvatarManagementServiceBean();
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        fileLoader = mock(FileLoader.class);

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "metadata", metadata);
        ReflectionTestUtils.setField(service, "fileLoader", fileLoader);
    }

    private FileDescriptor createFileDescriptor(String name) {
        FileDescriptor fd = new FileDescriptor();
        fd.setId(UUID.randomUUID());
        fd.setName(name);
        return fd;
    }

    @Test
    public void resolveEffectiveAvatar_nullUser_returnsThemeDefault() {
        ResolvedAvatarInfo info = service.resolveEffectiveAvatar((ExtUser) null);
        assertNotNull(info);
        assertEquals(AvatarSourceType.THEME_DEFAULT, info.getSourceType());
        assertTrue(info.isFallbackUsed());
        assertNull(info.getFileDescriptor());
        assertEquals("icons/no-programmer.jpeg", info.getFallbackThemePath());
    }

    @Test
    public void resolveEffectiveAvatar_k1_bothUploaded_priorityToUserPersonal() throws Exception {
        ExtUser user = new ExtUser();
        FileDescriptor personal = createFileDescriptor("personal.jpg");
        FileDescriptor official = createFileDescriptor("official.jpg");
        user.setUserAvatar(personal);
        user.setOfficialPhoto(official);

        when(fileLoader.openStream(personal)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(fileLoader.openStream(official)).thenReturn(new ByteArrayInputStream(new byte[]{4, 5, 6}));

        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.USER_PERSONAL, info.getSourceType());
        assertFalse(info.isFallbackUsed());
        assertEquals(personal.getId(), info.getFileDescriptor().getId());
    }

    @Test
    public void resolveEffectiveAvatar_k2_onlyPersonalUploaded_returnsUserPersonal() throws Exception {
        ExtUser user = new ExtUser();
        FileDescriptor personal = createFileDescriptor("personal.jpg");
        user.setUserAvatar(personal);

        when(fileLoader.openStream(personal)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.USER_PERSONAL, info.getSourceType());
        assertFalse(info.isFallbackUsed());
        assertEquals(personal.getId(), info.getFileDescriptor().getId());
    }

    @Test
    public void resolveEffectiveAvatar_k3_onlyOfficialUploaded_returnsAdminOfficialFallback() throws Exception {
        ExtUser user = new ExtUser();
        FileDescriptor official = createFileDescriptor("official.jpg");
        user.setOfficialPhoto(official);

        when(fileLoader.openStream(official)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.ADMIN_OFFICIAL, info.getSourceType());
        assertTrue(info.isFallbackUsed());
        assertEquals(official.getId(), info.getFileDescriptor().getId());
    }

    @Test
    public void resolveEffectiveAvatar_legacyFallback_whenPersonalAndOfficialEmpty() throws Exception {
        ExtUser user = new ExtUser();
        FileDescriptor legacy = createFileDescriptor("legacy.jpg");
        user.setFileImageFace(legacy);

        when(fileLoader.openStream(legacy)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.LEGACY_PHOTO, info.getSourceType());
        assertTrue(info.isFallbackUsed());
        assertEquals(legacy.getId(), info.getFileDescriptor().getId());
    }

    @Test
    public void resolveEffectiveAvatar_k4_noPhotoUploaded_returnsThemeDefault() {
        ExtUser user = new ExtUser();
        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.THEME_DEFAULT, info.getSourceType());
        assertTrue(info.isFallbackUsed());
        assertNull(info.getFileDescriptor());
        assertEquals("icons/no-programmer.jpeg", info.getFallbackThemePath());
    }

    @Test
    public void resolveEffectiveAvatar_k5_brokenPersonalFallsBackToOfficial() throws Exception {
        ExtUser user = new ExtUser();
        FileDescriptor brokenPersonal = createFileDescriptor("broken_personal.jpg");
        FileDescriptor official = createFileDescriptor("official.jpg");
        user.setUserAvatar(brokenPersonal);
        user.setOfficialPhoto(official);

        when(fileLoader.openStream(brokenPersonal)).thenThrow(new FileStorageException(FileStorageException.Type.FILE_NOT_FOUND, "Not found"));
        when(fileLoader.openStream(official)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.ADMIN_OFFICIAL, info.getSourceType());
        assertTrue(info.isFallbackUsed());
        assertEquals(official.getId(), info.getFileDescriptor().getId());
    }

    @Test
    public void resolveEffectiveAvatar_k6_brokenPersonalAndNoOfficial_returnsThemeDefault() throws Exception {
        ExtUser user = new ExtUser();
        FileDescriptor brokenPersonal = createFileDescriptor("broken_personal.jpg");
        user.setUserAvatar(brokenPersonal);

        when(fileLoader.openStream(brokenPersonal)).thenThrow(new FileStorageException(FileStorageException.Type.FILE_NOT_FOUND, "Not found"));

        ResolvedAvatarInfo info = service.resolveEffectiveAvatar(user);
        assertNotNull(info);
        assertEquals(AvatarSourceType.THEME_DEFAULT, info.getSourceType());
        assertTrue(info.isFallbackUsed());
        assertNull(info.getFileDescriptor());
    }

    @Test
    public void applyUserPersonalAvatar_setsAvatarAndSyncsSettings() throws Exception {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor oldAvatar = createFileDescriptor("old.jpg");
        FileDescriptor newAvatar = createFileDescriptor("new.jpg");
        user.setUserAvatar(oldAvatar);

        UserSettings settings = new UserSettings();
        settings.setUser(user);

        FluentLoader loader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserSettings.class)).thenReturn(loader);
        when(loader.query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.of(settings));

        ExtUser result = service.applyUserPersonalAvatar(user, newAvatar);

        assertNotNull(result);
        assertEquals(newAvatar, result.getUserAvatar());
        assertEquals(newAvatar, settings.getFileImageFace());
        verify(dataManager).commit(settings);
        verify(fileLoader).removeFile(oldAvatar);
        verify(dataManager).remove(oldAvatar);
    }

    @Test
    public void applyUserPersonalAvatar_doesNotDeleteOldAvatarIfReferencedByOfficialPhoto() throws Exception {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor sharedPhoto = createFileDescriptor("shared.jpg");
        FileDescriptor newAvatar = createFileDescriptor("new.jpg");
        user.setUserAvatar(sharedPhoto);
        user.setOfficialPhoto(sharedPhoto);

        UserSettings settings = new UserSettings();
        settings.setUser(user);

        FluentLoader loader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserSettings.class)).thenReturn(loader);
        when(loader.query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.of(settings));

        ExtUser result = service.applyUserPersonalAvatar(user, newAvatar);

        assertEquals(newAvatar, result.getUserAvatar());
        verify(fileLoader, never()).removeFile(sharedPhoto);
        verify(dataManager, never()).remove(sharedPhoto);
    }

    @Test
    public void clearUserPersonalAvatar_clearsAvatarAndSettings() throws Exception {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor oldAvatar = createFileDescriptor("old.jpg");
        user.setUserAvatar(oldAvatar);

        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setFileImageFace(oldAvatar);

        FluentLoader loader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserSettings.class)).thenReturn(loader);
        when(loader.query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.of(settings));

        ExtUser result = service.clearUserPersonalAvatar(user);

        assertNotNull(result);
        assertNull(result.getUserAvatar());
        assertNull(settings.getFileImageFace());
        verify(dataManager).commit(settings);
        verify(fileLoader).removeFile(oldAvatar);
        verify(dataManager).remove(oldAvatar);
    }

    @Test
    public void applyAdminOfficialPhoto_modeOfficialOnly_keepsPersonalAvatarUntouched() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor personal = createFileDescriptor("personal.jpg");
        FileDescriptor official = createFileDescriptor("official.jpg");
        user.setUserAvatar(personal);

        ExtUser result = service.applyAdminOfficialPhoto(user, official, AvatarApplyMode.OFFICIAL_ONLY);

        assertEquals(official, result.getOfficialPhoto());
        assertEquals(personal, result.getUserAvatar());
        verify(dataManager, never()).load(UserSettings.class);
    }

    @Test
    public void applyAdminOfficialPhoto_modeOverwriteAll_updatesBothAndSettings() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor personal = createFileDescriptor("personal.jpg");
        FileDescriptor newOfficial = createFileDescriptor("new_official.jpg");
        user.setUserAvatar(personal);

        UserSettings settings = new UserSettings();
        settings.setUser(user);

        FluentLoader loader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserSettings.class)).thenReturn(loader);
        when(loader.query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.of(settings));

        ExtUser result = service.applyAdminOfficialPhoto(user, newOfficial, AvatarApplyMode.OVERWRITE_ALL);

        assertEquals(newOfficial, result.getOfficialPhoto());
        assertEquals(newOfficial, result.getUserAvatar());
        assertEquals(newOfficial, settings.getFileImageFace());
        verify(dataManager).commit(settings);
    }

    @Test
    public void applyAdminOfficialPhoto_modeSmartDefault_withNoPersonal_updatesBoth() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor newOfficial = createFileDescriptor("new_official.jpg");
        user.setUserAvatar(null);

        UserSettings settings = new UserSettings();
        settings.setUser(user);

        FluentLoader loader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserSettings.class)).thenReturn(loader);
        when(loader.query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.of(settings));

        ExtUser result = service.applyAdminOfficialPhoto(user, newOfficial, AvatarApplyMode.SMART_DEFAULT);

        assertEquals(newOfficial, result.getOfficialPhoto());
        assertEquals(newOfficial, result.getUserAvatar());
        assertEquals(newOfficial, settings.getFileImageFace());
        verify(dataManager).commit(settings);
    }

    @Test
    public void applyAdminOfficialPhoto_modeSmartDefault_withExistingPersonal_updatesOnlyOfficial() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor personal = createFileDescriptor("personal.jpg");
        FileDescriptor newOfficial = createFileDescriptor("new_official.jpg");
        user.setUserAvatar(personal);

        ExtUser result = service.applyAdminOfficialPhoto(user, newOfficial, AvatarApplyMode.SMART_DEFAULT);

        assertEquals(newOfficial, result.getOfficialPhoto());
        assertEquals(personal, result.getUserAvatar());
        verify(dataManager, never()).load(UserSettings.class);
    }

    @Test
    public void clearAdminOfficialPhoto_clearsOfficialAndCleansUp() throws Exception {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        FileDescriptor official = createFileDescriptor("official.jpg");
        user.setOfficialPhoto(official);

        ExtUser result = service.clearAdminOfficialPhoto(user);

        assertNull(result.getOfficialPhoto());
        verify(fileLoader).removeFile(official);
        verify(dataManager).remove(official);
    }

    @Test
    public void cleanupUnreferencedFile_skipsWhenReferenced() throws Exception {
        FileDescriptor candidate = createFileDescriptor("candidate.jpg");
        FileDescriptor activeRef = new FileDescriptor();
        activeRef.setId(candidate.getId());

        service.cleanupUnreferencedFile(candidate, activeRef);

        verify(fileLoader, never()).removeFile(any());
        verify(dataManager, never()).remove(any(FileDescriptor.class));
    }

    @Test
    public void cleanupUnreferencedFile_deletesWhenNotReferenced() throws Exception {
        FileDescriptor candidate = createFileDescriptor("candidate.jpg");
        FileDescriptor anotherRef = createFileDescriptor("other.jpg");

        service.cleanupUnreferencedFile(candidate, anotherRef);

        verify(fileLoader).removeFile(candidate);
        verify(dataManager).remove(candidate);
    }
}
