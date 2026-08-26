package com.company.hunttech.service;

import com.company.hunttech.core.telegram.TelegramClientProvider;
import com.company.hunttech.service.dto.telegram.*;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class TelegramIntegrationServiceBeanTest {

    private TelegramIntegrationServiceBean service;
    private MockTelegramClientProvider mockClientProvider;
    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private FileLoader mockFileLoader;

    private final AtomicBoolean fileSaved = new AtomicBoolean(false);
    private final AtomicBoolean dataCommitted = new AtomicBoolean(false);

    @Before
    public void setUp() throws Exception {
        service = new TelegramIntegrationServiceBean();
        mockClientProvider = new MockTelegramClientProvider();
        fileSaved.set(false);
        dataCommitted.set(false);

        mockMetadata = (Metadata) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Metadata.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("create".equals(method.getName()) && args != null && args.length == 1 && args[0] == FileDescriptor.class) {
                            return new FileDescriptor();
                        }
                        return null;
                    }
                }
        );

        mockDataManager = (DataManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DataManager.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("commit".equals(method.getName()) && args != null && args.length >= 1) {
                            dataCommitted.set(true);
                            return args[0];
                        }
                        return null;
                    }
                }
        );

        mockFileLoader = (FileLoader) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{FileLoader.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("saveStream".equals(method.getName())) {
                            fileSaved.set(true);
                            return null;
                        }
                        return null;
                    }
                }
        );

        setField(service, "telegramClientProvider", mockClientProvider);
        setField(service, "dataManager", mockDataManager);
        setField(service, "metadata", mockMetadata);
        setField(service, "fileLoader", mockFileLoader);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testGetUserProfilePhoto_Resolutions() {
        Long userId = 123456789L;
        PhotoSize small = createPhotoSize("file_small", "uniq_small", 160, 160, 1024);
        PhotoSize medium = createPhotoSize("file_med", "uniq_med", 320, 320, 4096);
        PhotoSize large = createPhotoSize("file_large", "uniq_large", 640, 640, 16384);

        UserProfilePhotos photos = new UserProfilePhotos();
        photos.setTotalCount(1);
        photos.setPhotos(Collections.singletonList(Arrays.asList(small, medium, large)));

        mockClientProvider.userProfilePhotos = photos;
        mockClientProvider.file = createFile("file_large", "photos/file_large.jpg");

        // High resolution / largest
        TelegramPhotoDto photoLarge = service.getUserProfilePhoto(userId, PhotoResolution.LARGEST_AVAILABLE);
        assertNotNull(photoLarge);
        assertEquals("file_large", photoLarge.getFileId());
        assertEquals(Integer.valueOf(640), photoLarge.getWidth());

        // Thumbnail
        mockClientProvider.file = createFile("file_small", "photos/file_small.jpg");
        TelegramPhotoDto photoSmall = service.getUserProfilePhoto(userId, PhotoResolution.THUMBNAIL);
        assertNotNull(photoSmall);
        assertEquals("file_small", photoSmall.getFileId());
        assertEquals(Integer.valueOf(160), photoSmall.getWidth());

        // Medium
        mockClientProvider.file = createFile("file_med", "photos/file_med.jpg");
        TelegramPhotoDto photoMed = service.getUserProfilePhoto(userId, PhotoResolution.MEDIUM);
        assertNotNull(photoMed);
        assertEquals("file_med", photoMed.getFileId());
        assertEquals(Integer.valueOf(320), photoMed.getWidth());
    }

    @Test
    public void testGetUserProfilePhoto_NoPhotos_ReturnsNull() {
        Long userId = 999999L;
        UserProfilePhotos emptyPhotos = new UserProfilePhotos();
        emptyPhotos.setTotalCount(0);
        emptyPhotos.setPhotos(Collections.emptyList());

        mockClientProvider.userProfilePhotos = emptyPhotos;

        TelegramPhotoDto result = service.getUserProfilePhoto(userId, PhotoResolution.LARGEST_AVAILABLE);
        assertNull(result);
    }

    @Test
    public void testGetUserProfile_FullMapping() {
        Long userId = 777L;
        Chat chat = new Chat();
        chat.setId(userId);
        chat.setUserName("johndoe");
        chat.setFirstName("John");
        chat.setLastName("Doe");
        chat.setBio("Senior Java Developer");

        PhotoSize photo = createPhotoSize("file_avatar", "uniq_avatar", 640, 640, 2048);
        UserProfilePhotos photos = new UserProfilePhotos();
        photos.setTotalCount(1);
        photos.setPhotos(Collections.singletonList(Collections.singletonList(photo)));

        mockClientProvider.chat = chat;
        mockClientProvider.userProfilePhotos = photos;

        TelegramUserProfileDto profile = service.getUserProfile(userId);
        assertNotNull(profile);
        assertEquals(userId, profile.getId());
        assertEquals("johndoe", profile.getUsername());
        assertEquals("John", profile.getFirstName());
        assertEquals("Doe", profile.getLastName());
        assertEquals("John Doe", profile.getDisplayName());
        assertEquals("Senior Java Developer", profile.getBio());
        assertTrue(profile.getHasPhoto());
        assertEquals("file_avatar", profile.getMainPhotoFileId());
    }

    @Test
    public void testSaveUserProfilePhotoToFileStorage() {
        Long userId = 555L;
        PhotoSize photo = createPhotoSize("file_save", "uniq_save", 640, 640, 100);
        UserProfilePhotos photos = new UserProfilePhotos();
        photos.setTotalCount(1);
        photos.setPhotos(Collections.singletonList(Collections.singletonList(photo)));

        mockClientProvider.userProfilePhotos = photos;
        mockClientProvider.file = createFile("file_save", "photos/file_save.jpg");
        mockClientProvider.downloadedBytes = new byte[]{1, 2, 3, 4, 5};

        FileDescriptor fd = service.saveUserProfilePhotoToFileStorage(userId, "custom_avatar.jpg");
        assertNotNull(fd);
        assertEquals("custom_avatar.jpg", fd.getName());
        assertEquals("jpg", fd.getExtension());
        assertEquals(Long.valueOf(5), fd.getSize());
        assertTrue(fileSaved.get());
        assertTrue(dataCommitted.get());

        // Test with string identifier (numeric string)
        FileDescriptor fdStr = service.saveUserProfilePhotoToFileStorage("555", "custom_avatar_str.jpg");
        assertNotNull(fdStr);
        assertEquals("custom_avatar_str.jpg", fdStr.getName());

        // Test with username identifier
        Chat userChat = new Chat();
        userChat.setId(555L);
        userChat.setUserName("candidate_tg");
        mockClientProvider.chat = userChat;

        FileDescriptor fdUser = service.saveUserProfilePhotoToFileStorage("@candidate_tg", "candidate_avatar.jpg");
        assertNotNull(fdUser);
        assertEquals("candidate_avatar.jpg", fdUser.getName());
    }

    @Test
    public void testSendMessage_SuccessAndValidation() {
        // Validation failure
        TelegramSendResult failRes = service.sendMessage("", "Hello");
        assertFalse(failRes.isSuccess());
        assertNotNull(failRes.getFailureReason());

        TelegramSendResult emptyTextRes = service.sendMessage("12345", "");
        assertFalse(emptyTextRes.isSuccess());

        // Success
        Message sentMsg = new Message();
        sentMsg.setMessageId(101);
        Chat chat = new Chat();
        chat.setId(12345L);
        sentMsg.setChat(chat);
        mockClientProvider.sentMessage = sentMsg;

        TelegramSendMessageRequest request = TelegramSendMessageRequest.builder()
                .targetChatId("12345")
                .text("<b>Привет!</b>")
                .parseMode("HTML")
                .inlineKeyboard(Collections.singletonList(
                        Collections.singletonList(
                                TelegramSendMessageRequest.InlineButtonDto.builder()
                                        .text("Открыть профиль")
                                        .url("https://hunttech.example.com")
                                        .build()
                        )
                ))
                .build();

        TelegramSendResult successRes = service.sendMessage(request);
        assertTrue(successRes.isSuccess());
        assertEquals(Integer.valueOf(101), successRes.getMessageId());
        assertEquals(Long.valueOf(12345L), successRes.getChatId());
    }

    @Test
    public void testSendPhoto_Success() {
        Message sentMsg = new Message();
        sentMsg.setMessageId(202);
        Chat chat = new Chat();
        chat.setId(999L);
        sentMsg.setChat(chat);
        mockClientProvider.sentPhotoMessage = sentMsg;

        TelegramSendPhotoRequest req = TelegramSendPhotoRequest.builder()
                .targetChatId("999")
                .photoBytes(new byte[]{10, 20, 30})
                .photoFileName("candidate.jpg")
                .caption("Кандидат на вакансию")
                .build();

        TelegramSendResult res = service.sendPhoto(req);
        assertTrue(res.isSuccess());
        assertEquals(Integer.valueOf(202), res.getMessageId());
    }

    private PhotoSize createPhotoSize(String fileId, String fileUniqueId, int width, int height, int fileSize) {
        PhotoSize ps = new PhotoSize();
        ps.setFileId(fileId);
        ps.setFileUniqueId(fileUniqueId);
        ps.setWidth(width);
        ps.setHeight(height);
        ps.setFileSize(fileSize);
        return ps;
    }

    private org.telegram.telegrambots.meta.api.objects.File createFile(String fileId, String filePath) {
        org.telegram.telegrambots.meta.api.objects.File f = new org.telegram.telegrambots.meta.api.objects.File();
        f.setFileId(fileId);
        f.setFilePath(filePath);
        return f;
    }

    private static class MockTelegramClientProvider extends TelegramClientProvider {
        UserProfilePhotos userProfilePhotos;
        org.telegram.telegrambots.meta.api.objects.File file;
        Chat chat;
        byte[] downloadedBytes = new byte[]{1, 2, 3};
        Message sentMessage;
        Message sentPhotoMessage;

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public UserProfilePhotos getUserProfilePhotos(Long userId, Integer offset, Integer limit) {
            return userProfilePhotos;
        }

        @Override
        public org.telegram.telegrambots.meta.api.objects.File getFile(String fileId) {
            return file;
        }

        @Override
        public Chat getChat(String chatId) {
            return chat;
        }

        @Override
        public byte[] downloadFileBytes(String filePath) {
            return downloadedBytes;
        }

        @Override
        public Message executeSendMessage(SendMessage sendMessage) throws TelegramApiException {
            if (sentMessage != null) {
                return sentMessage;
            }
            throw new TelegramApiException("Mock send error");
        }

        @Override
        public Message executeSendPhoto(SendPhoto sendPhoto) throws TelegramApiException {
            if (sentPhotoMessage != null) {
                return sentPhotoMessage;
            }
            throw new TelegramApiException("Mock photo send error");
        }
    }
}
