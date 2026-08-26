package com.company.hunttech.service.dto.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO профиля пользователя Telegram.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUserProfileDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Числовой ID пользователя Telegram
     */
    private Long id;

    /**
     * Имя пользователя (username без символа '@')
     */
    private String username;

    /**
     * Имя (first name)
     */
    private String firstName;

    /**
     * Фамилия (last name)
     */
    private String lastName;

    /**
     * Код языка (IETF language tag, e.g. "ru", "en")
     */
    private String languageCode;

    /**
     * Флаг бота
     */
    private Boolean isBot;

    /**
     * Биография / описание пользователя (если получено через getChat)
     */
    private String bio;

    /**
     * Наличие хотя бы одной фотографии профиля
     */
    private Boolean hasPhoto;

    /**
     * Общее количество фотографий в профиле
     */
    private Integer totalPhotosCount;

    /**
     * Telegram file_id главной фотографии профиля (если есть)
     */
    private String mainPhotoFileId;

    /**
     * Полное отображаемое имя (First + Last или Username)
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            sb.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lastName.trim());
        }
        if (sb.length() == 0 && username != null && !username.trim().isEmpty()) {
            sb.append("@").append(username.trim());
        }
        return sb.toString();
    }
}
