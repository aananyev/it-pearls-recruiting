package com.company.itpearls.core.telegrambot.telegram.commands.service;

import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.commands.service.ServiceCommand;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import javax.inject.Inject;
import java.util.List;

public class VacancyListCommand extends ServiceCommand {

    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);


    public VacancyListCommand(String identifier, String description) {
        super(identifier, description);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        String userName = Utils.getUserName(user);
        List<OpenPosition> openPositions;

        try {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                // get EntityManager for the current transaction
                EntityManager em = persistence.getEntityManager();
                Query query = em.createQuery("select e from itpearls_OpenPosition e where not e.openClose = true");

                openPositions = query.getResultList();
                tx.commit();
            }

            int counter = 1;

            for (OpenPosition openPosition : openPositions) {
                StringBuilder sb = new StringBuilder();

                sb.append(counter++)
                        .append(". ")
                        .append(openPosition.getVacansyName())
                        .append("\n");

                sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName, sb.toString());
            }

        } catch (NullPointerException e) {
            logger.debug("ОШИБКА - не загрузились открытые вакансии");
            sendAnswer(absSender, chat.getId(), this.getCommandIdentifier(), userName,
                    "Нет открытых вакансий");
        }

    }
}
