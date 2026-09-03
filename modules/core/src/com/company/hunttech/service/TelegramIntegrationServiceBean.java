package com.company.hunttech.service;

import com.company.hunttech.core.telegram.TelegramClientProvider;
import com.company.hunttech.service.dto.telegram.*;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Реализация сервиса интеграции с Telegram Bot API с подробным логированием и диагностикой.
 */
@Service(TelegramIntegrationService.NAME)
public class TelegramIntegrationServiceBean implements TelegramIntegrationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramIntegrationServiceBean.class);

    @Inject
    private TelegramClientProvider telegramClientProvider;

    @Inject
    private DataManager dataManager;

    @Inject
    private Metadata metadata;

    @Inject
    private FileLoader fileLoader;

    @Override
    public boolean isConfigured() {
        boolean configured = telegramClientProvider.isConfigured();
        log.debug("TelegramIntegrationService.isConfigured() -> {}", configured);
        return configured;
    }

    @Override
    public TelegramUserProfileDto getUserProfile(Long telegramUserId) {
        if (telegramUserId == null) {
            log.warn("getUserProfile: telegramUserId is null");
            return null;
        }
        log.info("Fetching Telegram user profile for userId={}", telegramUserId);
        try {
            Chat chat = null;
            try {
                chat = telegramClientProvider.getChat(String.valueOf(telegramUserId));
                log.debug("Found Chat for userId={}: type={}, username={}, title={}, firstName={}",
                        telegramUserId, chat.getType(), chat.getUserName(), chat.getTitle(), chat.getFirstName());
            } catch (TelegramApiException e) {
                log.debug("Chat info not directly queryable for userId={}: {}", telegramUserId, e.getMessage());
            }

            UserProfilePhotos userPhotos = null;
            try {
                userPhotos = telegramClientProvider.getUserProfilePhotos(telegramUserId, 0, 1);
                log.debug("UserProfilePhotos for userId={}: totalCount={}",
                        telegramUserId, userPhotos != null ? userPhotos.getTotalCount() : 0);
            } catch (TelegramApiException e) {
                log.debug("User profile photos query failed for userId={}: {}", telegramUserId, e.getMessage());
            }

            int photosCount = userPhotos != null && userPhotos.getTotalCount() != null ? userPhotos.getTotalCount() : 0;
            boolean hasPhoto = photosCount > 0 && !userPhotos.getPhotos().isEmpty();
            String mainPhotoFileId = null;

            if (hasPhoto && !userPhotos.getPhotos().get(0).isEmpty()) {
                List<PhotoSize> sizes = userPhotos.getPhotos().get(0);
                mainPhotoFileId = sizes.get(sizes.size() - 1).getFileId();
            } else if (chat != null && chat.getPhoto() != null && chat.getPhoto().getBigFileId() != null) {
                hasPhoto = true;
                photosCount = 1;
                mainPhotoFileId = chat.getPhoto().getBigFileId();
            }

            TelegramUserProfileDto profile = TelegramUserProfileDto.builder()
                    .id(telegramUserId)
                    .username(chat != null ? chat.getUserName() : null)
                    .firstName(chat != null ? chat.getFirstName() : null)
                    .lastName(chat != null ? chat.getLastName() : null)
                    .bio(chat != null ? (chat.getBio() != null ? chat.getBio() : chat.getDescription()) : null)
                    .isBot(Boolean.FALSE)
                    .hasPhoto(hasPhoto)
                    .totalPhotosCount(photosCount)
                    .mainPhotoFileId(mainPhotoFileId)
                    .build();

            log.info("Telegram user profile resolved successfully for userId={}: username='{}', displayName='{}', hasPhoto={}",
                    telegramUserId, profile.getUsername(), profile.getDisplayName(), profile.getHasPhoto());
            return profile;
        } catch (Exception e) {
            log.error("Failed to get Telegram user profile for userId=" + telegramUserId, e);
            return null;
        }
    }

    @Override
    public TelegramUserProfileDto getUserProfile(String telegramIdOrUsername) {
        Long userId = resolveNumericUserId(telegramIdOrUsername);
        if (userId != null) {
            return getUserProfile(userId);
        }
        log.warn("Could not resolve numeric Telegram userId from identifier '{}'", telegramIdOrUsername);
        return null;
    }

    @Override
    public TelegramPhotoDto getUserProfilePhoto(Long telegramUserId, PhotoResolution resolution) {
        if (telegramUserId == null) {
            log.warn("getUserProfilePhoto: telegramUserId is null");
            return null;
        }
        PhotoResolution targetResolution = resolution != null ? resolution : PhotoResolution.LARGEST_AVAILABLE;
        log.info("Getting Telegram profile photo metadata for userId={}, resolution={}", telegramUserId, targetResolution);

        try {
            UserProfilePhotos userPhotos = null;
            try {
                userPhotos = telegramClientProvider.getUserProfilePhotos(telegramUserId, 0, 1);
            } catch (TelegramApiException e) {
                log.debug("getUserProfilePhotos failed for userId={}: {}", telegramUserId, e.getMessage());
            }

            if (userPhotos != null && userPhotos.getTotalCount() != null && userPhotos.getTotalCount() > 0 && !userPhotos.getPhotos().isEmpty()) {
                List<PhotoSize> availableSizes = userPhotos.getPhotos().get(0);
                if (availableSizes != null && !availableSizes.isEmpty()) {
                    PhotoSize selectedSize = selectPhotoSize(availableSizes, targetResolution);
                    org.telegram.telegrambots.meta.api.objects.File tgFile = telegramClientProvider.getFile(selectedSize.getFileId());

                    TelegramPhotoDto result = TelegramPhotoDto.builder()
                            .fileId(selectedSize.getFileId())
                            .fileUniqueId(selectedSize.getFileUniqueId())
                            .width(selectedSize.getWidth())
                            .height(selectedSize.getHeight())
                            .fileSize(selectedSize.getFileSize())
                            .filePath(tgFile != null ? tgFile.getFilePath() : selectedSize.getFilePath())
                            .resolution(targetResolution)
                            .build();

                    log.info("Photo metadata retrieved: fileId={}, width={}, height={}, filePath={}",
                            result.getFileId(), result.getWidth(), result.getHeight(), result.getFilePath());
                    return result;
                }
            }

            // Fallback to chat photo if available
            log.debug("No photos in getUserProfilePhotos; checking getChat fallback for userId={}", telegramUserId);
            Chat chat = telegramClientProvider.getChat(String.valueOf(telegramUserId));
            if (chat != null && chat.getPhoto() != null) {
                String fileId = targetResolution == PhotoResolution.THUMBNAIL
                        ? (chat.getPhoto().getSmallFileId() != null ? chat.getPhoto().getSmallFileId() : chat.getPhoto().getBigFileId())
                        : (chat.getPhoto().getBigFileId() != null ? chat.getPhoto().getBigFileId() : chat.getPhoto().getSmallFileId());

                if (fileId != null) {
                    org.telegram.telegrambots.meta.api.objects.File tgFile = telegramClientProvider.getFile(fileId);
                    return TelegramPhotoDto.builder()
                            .fileId(fileId)
                            .fileUniqueId(chat.getPhoto().getBigFileUniqueId())
                            .filePath(tgFile != null ? tgFile.getFilePath() : null)
                            .resolution(targetResolution)
                            .build();
                }
            }

            log.info("No profile photo found for Telegram userId={}", telegramUserId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to retrieve Telegram profile photo metadata for userId={}: {}", telegramUserId, e.getMessage());
            return null;
        }
    }

    @Override
    public TelegramPhotoDto getUserProfilePhoto(String telegramIdOrUsername, PhotoResolution resolution) {
        Long userId = resolveNumericUserId(telegramIdOrUsername);
        if (userId != null) {
            return getUserProfilePhoto(userId, resolution);
        }
        log.warn("Could not resolve numeric Telegram userId from identifier '{}'", telegramIdOrUsername);
        return null;
    }

    @Override
    public byte[] downloadUserProfilePhotoBytes(Long telegramUserId, PhotoResolution resolution) {
        log.info("Downloading Telegram photo bytes for userId={}, resolution={}", telegramUserId, resolution);
        TelegramPhotoDto photoDto = getUserProfilePhoto(telegramUserId, resolution);
        if (photoDto == null || photoDto.getFilePath() == null || photoDto.getFilePath().trim().isEmpty()) {
            log.warn("Cannot download photo bytes: no photo metadata or empty filePath for userId={}", telegramUserId);
            return null;
        }
        try {
            byte[] bytes = telegramClientProvider.downloadFileBytes(photoDto.getFilePath());
            log.info("Successfully downloaded {} bytes of photo for userId={}", bytes != null ? bytes.length : 0, telegramUserId);
            return bytes;
        } catch (Exception e) {
            log.error("Failed to download Telegram user photo bytes for userId=" + telegramUserId, e);
            return null;
        }
    }

    @Override
    public byte[] downloadUserProfilePhotoBytes(String telegramIdOrUsername, PhotoResolution resolution) {
        Long userId = resolveNumericUserId(telegramIdOrUsername);
        if (userId != null) {
            return downloadUserProfilePhotoBytes(userId, resolution);
        }
        log.warn("Could not resolve numeric Telegram userId from identifier '{}'", telegramIdOrUsername);
        return null;
    }

    @Override
    public FileDescriptor saveUserProfilePhotoToFileStorage(Long telegramUserId, String customFileName) {
        log.info("saveUserProfilePhotoToFileStorage invoked for userId={}, customFileName='{}'", telegramUserId, customFileName);
        byte[] imageBytes = downloadUserProfilePhotoBytes(telegramUserId, PhotoResolution.LARGEST_AVAILABLE);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("Cannot save Telegram photo to FileStorage: image bytes are empty for userId={}", telegramUserId);
            return null;
        }

        String fileName = customFileName != null && !customFileName.trim().isEmpty()
                ? customFileName.trim()
                : "tg_avatar_" + telegramUserId + "_" + System.currentTimeMillis() + ".jpg";

        FileDescriptor fileDescriptor = metadata.create(FileDescriptor.class);
        fileDescriptor.setName(fileName);
        fileDescriptor.setExtension("jpg");
        fileDescriptor.setSize((long) imageBytes.length);
        fileDescriptor.setCreateDate(new Date());

        try {
            fileLoader.saveStream(fileDescriptor, () -> new ByteArrayInputStream(imageBytes));
            FileDescriptor committedFd = dataManager.commit(fileDescriptor);
            log.info("Telegram avatar successfully stored to FileStorage: id={}, name='{}', size={} bytes",
                    committedFd.getId(), committedFd.getName(), committedFd.getSize());
            return committedFd;
        } catch (FileStorageException e) {
            log.error("Error saving Telegram photo to FileStorage for userId=" + telegramUserId, e);
            return null;
        }
    }

    @Override
    public FileDescriptor saveUserProfilePhotoToFileStorage(String telegramIdOrUsername, String customFileName) {
        log.info("saveUserProfilePhotoToFileStorage invoked with string identifier='{}'", telegramIdOrUsername);
        Long userId = resolveNumericUserId(telegramIdOrUsername);
        if (userId != null) {
            return saveUserProfilePhotoToFileStorage(userId, customFileName);
        }
        log.warn("Could not resolve numeric Telegram userId from identifier '{}'", telegramIdOrUsername);
        return null;
    }

    @Override
    public TelegramChatInfoDto getChatInfo(String chatIdOrUsername) {
        if (chatIdOrUsername == null || chatIdOrUsername.trim().isEmpty()) {
            return null;
        }
        String normalizedChatId = chatIdOrUsername.trim();
        log.info("Getting Telegram chat info for '{}'", normalizedChatId);
        try {
            Chat chat = telegramClientProvider.getChat(normalizedChatId);
            if (chat == null) {
                log.warn("Chat not found for '{}'", normalizedChatId);
                return null;
            }
            Integer memberCount = telegramClientProvider.getChatMemberCount(normalizedChatId);

            String photoSmall = chat.getPhoto() != null ? chat.getPhoto().getSmallFileId() : null;
            String photoBig = chat.getPhoto() != null ? chat.getPhoto().getBigFileId() : null;

            TelegramChatInfoDto chatInfo = TelegramChatInfoDto.builder()
                    .id(chat.getId())
                    .type(chat.getType())
                    .title(chat.getTitle())
                    .username(chat.getUserName())
                    .firstName(chat.getFirstName())
                    .lastName(chat.getLastName())
                    .description(chat.getDescription() != null ? chat.getDescription() : chat.getBio())
                    .inviteLink(chat.getInviteLink())
                    .memberCount(memberCount)
                    .photoSmallFileId(photoSmall)
                    .photoBigFileId(photoBig)
                    .build();

            log.info("Chat info retrieved: id={}, type={}, title='{}', username='{}', memberCount={}",
                    chatInfo.getId(), chatInfo.getType(), chatInfo.getTitle(), chatInfo.getUsername(), chatInfo.getMemberCount());
            return chatInfo;
        } catch (Exception e) {
            log.warn("Failed to get Telegram chat info for '{}': {}", normalizedChatId, e.getMessage());
            return null;
        }
    }

    @Override
    public TelegramSendResult sendMessage(TelegramSendMessageRequest request) {
        if (request == null || request.getTargetChatId() == null || request.getTargetChatId().trim().isEmpty()) {
            log.warn("sendMessage failed: targetChatId is required");
            return TelegramSendResult.fail("targetChatId is required");
        }
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            log.warn("sendMessage failed: message text is empty");
            return TelegramSendResult.fail("message text is empty");
        }

        log.info("Sending Telegram message to targetChatId='{}', length={}",
                request.getTargetChatId(), request.getText().length());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(request.getTargetChatId().trim())
                .text(request.getText().trim())
                .build();

        if (request.getParseMode() != null && !request.getParseMode().trim().isEmpty()) {
            sendMessage.setParseMode(request.getParseMode().trim());
        }
        if (Boolean.TRUE.equals(request.getDisableNotification())) {
            sendMessage.setDisableNotification(true);
        }
        if (Boolean.TRUE.equals(request.getDisableWebPagePreview())) {
            sendMessage.setDisableWebPagePreview(true);
        }
        if (request.getReplyToMessageId() != null) {
            sendMessage.setReplyToMessageId(request.getReplyToMessageId());
        }

        if (request.getInlineKeyboard() != null && !request.getInlineKeyboard().isEmpty()) {
            sendMessage.setReplyMarkup(buildInlineKeyboardMarkup(request.getInlineKeyboard()));
        }

        try {
            Message sentMessage = telegramClientProvider.executeSendMessage(sendMessage);
            log.info("Message successfully sent to '{}', messageId={}", request.getTargetChatId(), sentMessage.getMessageId());
            return TelegramSendResult.ok(sentMessage.getMessageId(), sentMessage.getChatId());
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram message to '{}': {}", request.getTargetChatId(), e.getMessage());
            return TelegramSendResult.fail(e.getMessage());
        }
    }

    @Override
    public TelegramSendResult sendMessage(String targetChatId, String text) {
        return sendMessage(TelegramSendMessageRequest.builder()
                .targetChatId(targetChatId)
                .text(text)
                .parseMode("HTML")
                .build());
    }

    @Override
    public TelegramSendResult sendPhoto(TelegramSendPhotoRequest request) {
        if (request == null || request.getTargetChatId() == null || request.getTargetChatId().trim().isEmpty()) {
            return TelegramSendResult.fail("targetChatId is required");
        }

        log.info("Sending Telegram photo to targetChatId='{}'", request.getTargetChatId());
        SendPhoto.SendPhotoBuilder builder = SendPhoto.builder()
                .chatId(request.getTargetChatId().trim());

        if (request.getCaption() != null && !request.getCaption().trim().isEmpty()) {
            builder.caption(request.getCaption().trim());
        }
        if (request.getParseMode() != null && !request.getParseMode().trim().isEmpty()) {
            builder.parseMode(request.getParseMode().trim());
        }
        if (Boolean.TRUE.equals(request.getDisableNotification())) {
            builder.disableNotification(true);
        }
        if (request.getReplyToMessageId() != null) {
            builder.replyToMessageId(request.getReplyToMessageId());
        }

        if (request.getInlineKeyboard() != null && !request.getInlineKeyboard().isEmpty()) {
            builder.replyMarkup(buildInlineKeyboardMarkup(request.getInlineKeyboard()));
        }

        InputFile inputFile = null;
        if (request.getPhotoBytes() != null && request.getPhotoBytes().length > 0) {
            String fileName = request.getPhotoFileName() != null ? request.getPhotoFileName() : "photo.jpg";
            inputFile = new InputFile(new ByteArrayInputStream(request.getPhotoBytes()), fileName);
        } else if (request.getFileDescriptor() != null) {
            try (InputStream stream = fileLoader.openStream(request.getFileDescriptor())) {
                byte[] bytes = stream.readAllBytes();
                String fileName = request.getFileDescriptor().getName() != null ? request.getFileDescriptor().getName() : "photo.jpg";
                inputFile = new InputFile(new ByteArrayInputStream(bytes), fileName);
            } catch (Exception e) {
                log.error("Failed to read FileDescriptor stream for sendPhoto: {}", e.getMessage(), e);
                return TelegramSendResult.fail("Failed to open FileDescriptor stream: " + e.getMessage());
            }
        } else if (request.getTelegramFileId() != null && !request.getTelegramFileId().trim().isEmpty()) {
            inputFile = new InputFile(request.getTelegramFileId().trim());
        }

        if (inputFile == null) {
            return TelegramSendResult.fail("No photo source provided (photoBytes, fileDescriptor, or telegramFileId)");
        }

        builder.photo(inputFile);
        SendPhoto sendPhoto = builder.build();

        try {
            Message sentMessage = telegramClientProvider.executeSendPhoto(sendPhoto);
            log.info("Photo successfully sent to '{}', messageId={}", request.getTargetChatId(), sentMessage.getMessageId());
            return TelegramSendResult.ok(sentMessage.getMessageId(), sentMessage.getChatId());
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram photo to '{}': {}", request.getTargetChatId(), e.getMessage());
            return TelegramSendResult.fail(e.getMessage());
        }
    }

    /**
     * Преобразует строковый идентификатор (числовой ID или @username) в числовой Telegram User ID.
     */
    private Long resolveNumericUserId(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String clean = identifier.trim();
        // Check if pure numeric
        if (clean.matches("-?\\d+")) {
            try {
                return Long.parseLong(clean);
            } catch (NumberFormatException ignored) {
            }
        }
        // Try getChat with username
        String usernameToQuery = clean.startsWith("@") ? clean : "@" + clean;
        log.debug("Resolving numeric ID via getChat for username '{}'", usernameToQuery);
        try {
            Chat chat = telegramClientProvider.getChat(usernameToQuery);
            if (chat != null && chat.getId() != null) {
                log.debug("Resolved username '{}' to numeric ID {}", usernameToQuery, chat.getId());
                return chat.getId();
            }
        } catch (Exception e) {
            log.debug("Failed to resolve username '{}' to chat: {}", usernameToQuery, e.getMessage());
        }
        return null;
    }

    private PhotoSize selectPhotoSize(List<PhotoSize> sizes, PhotoResolution resolution) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        if (sizes.size() == 1 || resolution == PhotoResolution.LARGEST_AVAILABLE || resolution == PhotoResolution.HIGH_RESOLUTION) {
            return sizes.get(sizes.size() - 1);
        }
        if (resolution == PhotoResolution.THUMBNAIL) {
            return sizes.get(0);
        }
        if (resolution == PhotoResolution.MEDIUM) {
            int middleIndex = sizes.size() / 2;
            return sizes.get(middleIndex);
        }
        return sizes.get(sizes.size() - 1);
    }

    private InlineKeyboardMarkup buildInlineKeyboardMarkup(List<List<TelegramSendMessageRequest.InlineButtonDto>> buttonsMatrix) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (List<TelegramSendMessageRequest.InlineButtonDto> rowDto : buttonsMatrix) {
            if (rowDto == null || rowDto.isEmpty()) {
                continue;
            }
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (TelegramSendMessageRequest.InlineButtonDto btnDto : rowDto) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(btnDto.getText());
                if (btnDto.getUrl() != null && !btnDto.getUrl().trim().isEmpty()) {
                    button.setUrl(btnDto.getUrl().trim());
                } else if (btnDto.getCallbackData() != null && !btnDto.getCallbackData().trim().isEmpty()) {
                    button.setCallbackData(btnDto.getCallbackData().trim());
                } else {
                    log.debug("Skipping inline button without url or callbackData: text='{}'", btnDto.getText());
                    continue;
                }
                row.add(button);
            }
            if (!row.isEmpty()) {
                keyboard.add(row);
            }
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }
}
