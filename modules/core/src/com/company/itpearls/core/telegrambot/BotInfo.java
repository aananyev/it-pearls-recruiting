package com.company.itpearls.core.telegrambot;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.entity.OpenPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.User;

public class BotInfo {
    private static Logger logger = LoggerFactory.getLogger(Bot.class);

    public static String botShortDescription(User user) {
        StringBuilder sb = new StringBuilder(Utils.getBotName())
                .append("\n");

        if (Utils.isInternalUser(user)) {
            return sb.append("\nУважаемый рекрутер IT Pearls, ")
                    .append(user.getFirstName())
                    .append(". ")
                    .append("\n")
                    .append("Вы можете использовать этот бот как помощник для работы. Бот может показать вам актуальные вакансии, подписаться на них для работы, показать описание проектов и т.п.")
                    .append("\n")
                    .toString();
        } else {
            return sb.append("\nУважаемый соискатель, ")
                    .append(user.getFirstName())
                    .append(". ")
                    .append("\n")
                    .append("Вы можете подобрать вакансии или проекты для себя по душе, посмотреть контактную информацию специалиста, который поможет вам с информацией об конкретной вакансии и проекте.")
                    .append("\n")
                    .toString();
        }
    }

    public static String getProjectName(OpenPosition openPosition, User user) {
        if (Utils.isInternalUser(user)) {
            return openPosition.getProjectName().getProjectName();
        } else {
            return openPosition.getProjectName().getProjectNameForCandidate();
        }
    }

    public static String getProjectName(OpenPosition openPosition, String user) {
        if (Utils.isInternalUser(user)) {
            return openPosition.getProjectName().getProjectName();
        } else {
            return openPosition.getProjectName().getProjectNameForCandidate();
        }
    }

    public static String getVacancyName(OpenPosition openPosition, User user) {
        if (Utils.isInternalUser(user)) {
            return openPosition.getVacansyName();
        } else {
            return new StringBuilder("<b>")
                    .append(openPosition.getPositionType().getPositionRuName())
                    .append(" / ")
                    .append(openPosition.getPositionType().getPositionEnName())
                    .append("</b> в проект <b>")
                    .append(BotInfo.getProjectName(openPosition, user))
                    .append("</b>")
                    .toString();
        }
    }

    public static String getVacancyName(OpenPosition openPosition, String user) {
        if (Utils.isInternalUser(user)) {
            return openPosition.getVacansyName();
        } else {
            return new StringBuilder("<b>")
                    .append(openPosition.getPositionType().getPositionRuName())
                    .append(" / ")
                    .append(openPosition.getPositionType().getPositionEnName())
                    .append("</b> в проект <b>")
                    .append(BotInfo.getProjectName(openPosition, user))
                    .append("</b>")
                    .toString();
        }
    }
}
