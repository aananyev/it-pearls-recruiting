package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.entity.OpenPositionPriority;
import com.company.itpearls.entity.Project;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

public class ProjectsCommand extends OperationCommand {
    public ProjectsCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<Project> projects = Utils.getActiveProjetsList(chat);

        String userName = Utils.getUserName(user);
        int counter = 1;
        Boolean subscribeFlag = Utils.isInternalUser(user);

        sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                new StringBuilder()
                        .append(Utils.getBotName())
                        .append("\n")
                        .append("Приоритет не ниже: <b>")
                        .append(OpenPositionPriority.fromId(Utils.getPriority(chat)))
                        .append("</b>\n")
                        .append("Всего открыто вакансий: <b>")
                        .append(projects.size())
                        .append("</b>\n\n")
                        .toString());

        for (Project project : projects) {
            StringBuilder sb = new StringBuilder();

            sb.append(counter++)
                    .append(". <b>")
                    .append(project.getProjectName())
                    .append("</b> (<b>")
                    .append(Utils.getProjectsVacancyCount(chat, project))
                    .append("</b> человек)");

            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    sb.toString(), setInline(project, subscribeFlag));
        }
    }

    private InlineKeyboardMarkup setInline(Project project, Boolean subscribeFlag) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton viewDetailsButton = new InlineKeyboardButton();
        viewDetailsButton.setText("Details");
        viewDetailsButton.setCallbackData(CallbackData.VIEW_PROJECTS_DETAIL_BUTTON
                + CallbackData.CALLBACK_SEPARATOR
                + project.getId().toString());

        InlineKeyboardButton viewProjectVacansiesButton = new InlineKeyboardButton();
        viewProjectVacansiesButton.setText("Vacansies");
        viewProjectVacansiesButton.setCallbackData(CallbackData.VIEW_PROJECT_VACANSIES_BUTTON
                + CallbackData.CALLBACK_SEPARATOR
                + project.getId().toString());

        rowInline.add(viewDetailsButton);
        rowInline.add(viewProjectVacansiesButton);

        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;

    }
}
