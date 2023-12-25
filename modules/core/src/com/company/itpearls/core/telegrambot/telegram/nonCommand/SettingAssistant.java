package com.company.itpearls.core.telegrambot.telegram.nonCommand;

import com.company.itpearls.core.telegrambot.exeptions.IllegalSettingsException;
import lombok.Getter;

public class SettingAssistant {

    @Getter
    private Settings defaultSettings = new Settings(3, true);

    static int priorityNotLower(int priorityNotLower) {
        if (priorityNotLower >= 0 && priorityNotLower <= 5) {
            return priorityNotLower;
        } else {
            throw new IllegalSettingsException("\\uD83D\\uDCA9 Приоритет должен быть в интервале от 0 до 5");
        }
    }

    static Boolean publishNewVacancy(Boolean publishNewVacancy) {
        return publishNewVacancy;
    }
}
