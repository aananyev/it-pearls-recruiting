package com.company.itpearls.core.telegrambot.telegram.commands.operations;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;
import com.company.itpearls.entity.*;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.View;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectsCommand extends OperationCommand {
    private static Logger logger = LoggerFactory.getLogger(Bot.class);

    public ProjectsCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<Project> projects = getActiveProjetsList(chat);

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
            StringBuilder projectName = new StringBuilder();

            if (subscribeFlag) {
                projectName.append(project.getProjectName());
            } else {
                if (project.getProjectNameForCandidate() != null) {
                    if (!project.getProjectNameForCandidate().equals("")) {
                        projectName.append(project.getProjectNameForCandidate());
                    } else {
                        projectName.append(project.getProjectName());
                    }
                } else {
                    projectName.append(project.getProjectName());
                }
            }

            sb.append(counter++)
                    .append(". <b>")
                    .append(projectName)
                    .append("</b> ");

            if (subscribeFlag) {
                sb.append("(<b>")
                        .append(getProjectsVacancyCount(chat, project))
                        .append("</b> человек)");
            }

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

    public static List<Project> getActiveProjetsList(Chat chat) {
        List<Project> projects = new ArrayList<>();
        Settings settings = Bot.getUserSettings(chat.getId());

        final String QUERY_ACTIVE_PROJECT_LIST
                = "select e from itpearls_Project e where e in " +
                "(select f.projectName from itpearls_OpenPosition f " +
                "where not (f.openClose = true) and f.priority >= :priority)";

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();

                Query query = em.createQuery(QUERY_ACTIVE_PROJECT_LIST);
                query.addView(new View(Project.class)
                        .addProperty("projectName"));
                query.setParameter("priority", settings.getPriorityNotLower());

                projects = (List<Project>) query.getResultList();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return projects;
        }
    }

    public static int getProjectsVacancyCount(Chat chat, Project project) {
        final String QUERY_POSITIONVACANCY_COUNT =
                "select sum(e.numberPosition) from itpearls_OpenPosition e " +
                        "where e.projectName = :project and e.priority >= :priority and not (e.openClose = true)";
        Long count = null;
        Settings settings = Bot.getUserSettings(chat.getId());

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_POSITIONVACANCY_COUNT);
                query.setParameter("priority", settings.getPriorityNotLower());
                query.setParameter("project", project);

                count = (Long) query.getSingleResult();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return Math.toIntExact(count);
        }
    }

    public static String getProjectDescription(User from, String projectId) {
        final String QUERY_GET_PROJECT_DESCRIPTION = "select e from itpearls_Project e where e.id = :uuid";
        Project project = null;
        Boolean subscribeFlag = Utils.isInternalUser(from);

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_GET_PROJECT_DESCRIPTION);
                query.setParameter("uuid", UUID.fromString(projectId));
                query.setView(new View(Project.class).addProperty("projectDepartment",
                        new View(CompanyDepartament.class)
                                .addProperty("departamentRuName")
                                .addProperty("companyName", new View(Company.class)
                                        .addProperty("comanyName"))));

                project = (Project) query.getSingleResult();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            StringBuilder projectDesc = new StringBuilder();

            if (subscribeFlag) {
                projectDesc.append(project.getProjectDescription() != null
                        ? project.getProjectDescription() : "❗Нет описания проекта");
            } else {
                projectDesc.append(project.getProjectDescriptionForCandidate() != null
                        ? project.getProjectDescriptionForCandidate() :
                        (project.getProjectDescription() != null
                        ? project.getProjectDescription() : "❗Нет описания проекта"));
            }

            StringBuilder projectNameSB = new StringBuilder();

            if (subscribeFlag) {
                projectNameSB.append(project.getProjectName());
            } else {
                projectNameSB.append(project.getProjectNameForCandidate() != null ?
                        project.getProjectNameForCandidate() : project.getProjectName());
            }
            return new StringBuilder()
                    .append("<b>Наименование проекта:</b> ")
//                    .append(project.getProjectName())
                    .append(projectNameSB)
                    .append("\n")
                    .append("<b>Наименование компании:</b> ")
                    .append(project.getProjectDepartment().getCompanyName().getComanyName())
                    .append("\n")
                    .append("<b>Департамент:</b> ")
                    .append(project.getProjectDepartment().getDepartamentRuName())
                    .append("\n\n")
                    .append(projectDesc)
                    .toString();
        }
    }

    public static InlineKeyboardMarkup getProjectVacansiesKeyboard(String projectId) {
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton viewProjectOpenPosionsButton = new InlineKeyboardButton();
        viewProjectOpenPosionsButton.setText("View vacansies");
        StringBuilder callBackDataSB = new StringBuilder()
                .append(CallbackData.VIEW_PROJECT_VACANSIES_BUTTON)
                .append(CallbackData.CALLBACK_SEPARATOR)
                .append(projectId);

        viewProjectOpenPosionsButton.setCallbackData(callBackDataSB.toString());

        rowInline.add(viewProjectOpenPosionsButton);

        buttons.add(rowInline);

        markupKeyboard.setKeyboard(buttons);

        return markupKeyboard;
    }


    public static String getProjectUUID(String positionID) {
        final String QUERY_GET_POSITION_NAME = "select e from itpearls_Project e where e.id = :uuid";
        Project project = null;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery(QUERY_GET_POSITION_NAME);
                query.setParameter("uuid", UUID.fromString(positionID));

                project = (Project) query.getSingleResult();
                tx.commit();
            }
        } catch (Exception e) {
            logger.error(String.format("SQL error: %s", e.getMessage()));
        } finally {
            return new StringBuilder()
                    .append(project.getProjectName() != null ? project.getProjectName() : "")
                    .toString();
        }
    }

    private static final String QUERY_LIST_PROJECT_OPEN_POSITION_ID =
            "select e from itpearls_OpenPosition e " +
                    "where e.priority >= %s and not(e.openClose = true) and e.projectName.id = :uuid";

    public static List<OpenPosition> getProjectOpenPosition(Long chatId, String projectCallBack, String positionKey) {
        Settings settings = Bot.getUserSettings(chatId);

        List<OpenPosition> result = Utils.queryListResult(String.format(QUERY_LIST_PROJECT_OPEN_POSITION_ID,
                        settings.getPriorityNotLower()),
                new View(OpenPosition.class)
                        .addProperty("openPositionComments", new View(OpenPositionComment.class)
                                .addProperty("comment"))
                        .addProperty("vacansyName")
                        .addProperty("projectName",
                                new View(Project.class)
                                        .addProperty("projectName")
                                        .addProperty("projectOwner",
                                                new View(Person.class)
                                                        .addProperty("firstName")
                                                        .addProperty("secondName")))
                        .addProperty("positionType",
                                new View(Position.class)
                                        .addProperty("positionEnName")
                                        .addProperty("positionRuName")),
                UUID.fromString(positionKey));

        return result;
    }
}
