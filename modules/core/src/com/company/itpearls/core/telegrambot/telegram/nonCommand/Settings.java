package com.company.itpearls.core.telegrambot.telegram.nonCommand;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Пользовательские настройки
 */
@Getter
@EqualsAndHashCode
public class Settings {

    private int priorityNotLower;
    private Boolean publishNewVacancies;

    public int getPriorityNotLower() {
        return this.priorityNotLower;
    }

    public Boolean getPublishNewVacancies() {
        return this.publishNewVacancies;
    }

    public Settings() {
        this.priorityNotLower = 3;
        this.publishNewVacancies = true;
    }
    public Settings(Settings settings) {
        this.priorityNotLower = settings.priorityNotLower;
        this.publishNewVacancies = settings.publishNewVacancies;
    }
    public Settings(int priorityNotLower, Boolean publishNewVacancies) {
        this.priorityNotLower = SettingAssistant.priorityNotLower(priorityNotLower);
        this.publishNewVacancies = SettingAssistant.publishNewVacancy(publishNewVacancies);
    }
}