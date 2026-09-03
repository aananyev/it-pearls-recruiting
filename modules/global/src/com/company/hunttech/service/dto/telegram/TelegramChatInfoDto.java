package com.company.hunttech.service.dto.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO информации о чате, группе, супергруппе или канале Telegram.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramChatInfoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Числовой ID чата (для групп и каналов часто отрицательный)
     */
    private Long id;

    /**
     * Тип чата: "private", "group", "supergroup", "channel"
     */
    private String type;

    /**
     * Название группы / канала
     */
    private String title;

    /**
     * Username канала / группы / пользователя (без '@')
     */
    private String username;

    /**
     * Имя собеседника (для private чатов)
     */
    private String firstName;

    /**
     * Фамилия собеседника (для private чатов)
     */
    private String lastName;

    /**
     * Описание канала / группы или Bio пользователя
     */
    private String description;

    /**
     * Ссылка-приглашение (invite link)
     */
    private String inviteLink;

    /**
     * Количество участников (member count)
     */
    private Integer memberCount;

    /**
     * Telegram file_id аватара чата/канала
     */
    private String photoSmallFileId;
    private String photoBigFileId;

    public boolean isChannel() {
        return "channel".equalsIgnoreCase(type);
    }

    public boolean isGroup() {
        return "group".equalsIgnoreCase(type) || "supergroup".equalsIgnoreCase(type);
    }

    public boolean isPrivate() {
        return "private".equalsIgnoreCase(type);
    }
}
