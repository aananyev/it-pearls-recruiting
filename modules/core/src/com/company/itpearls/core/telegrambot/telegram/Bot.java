
package com.company.itpearls.core.telegrambot.telegram;

import com.company.itpearls.core.telegrambot.exeptions.UserException;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.commands.service.*;
import com.company.itpearls.entity.RecrutiesTasks;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.AppBeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.NonCommand;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;
import com.company.itpearls.core.telegrambot.telegram.commands.operations.*;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Собственно, бот
 */
public final class Bot extends TelegramLongPollingCommandBot {
    private static final String QUERY_UPDATE = "update ";
    private Logger logger = LoggerFactory.getLogger(Bot.class);

    private final String BOT_NAME;
    private final String BOT_TOKEN;

    private static final Settings defaultSettings = new Settings(3, true);
    private final NonCommand nonCommand;
    private List<String> registeredCommands = new ArrayList<>();


    public static Settings getDefaultSettings() {
        return new Settings(3, true);
    }

    /**
     * Настройки файла для разных пользователей. Ключ - уникальный id чата
     */
    private static Map<Long, Settings> userSettings;

    public Bot(String botName, String botToken) {
        super();
        logger.debug("Конструктор суперкласса отработал");
        this.BOT_NAME = botName;
        this.BOT_TOKEN = botToken;
        logger.debug("Имя и токен присвоены");

        this.nonCommand = new NonCommand();
        logger.debug("Класс обработки сообщения, не являющегося командой, создан");

        register(new VacancyListCommand("allvacancy", "Все вакансии"));
        logger.debug("Команда allvacancy создана");

        register(new SubscribeCommand("subscribers", "Подписки на вакансии"));
        logger.debug("Команда subscribe создана");

        register(new MySubscribeCommand("mysubscribe", "Мои подписки на вакансии"));
        logger.debug("Команда subscribe создана");

        register(new UserSessionTg("usersession", "Пользователи в системе"));
        logger.debug("Команда usersession создана");

        register(new StartCommand("start", "Старт"));
        logger.debug("Команда start создана");

        register(new HelpCommand("help", "Помощь"));
        logger.debug("Команда help создана");

        register(new SettingsCommand("settings", "Мои настройки"));
        logger.debug("Команда settings создана");

        userSettings = new HashMap<>();
        logger.info("Бот создан!");

        setRegisteredCommands();

/*        try {
            setAnswer(getMe().getId(), getBotUsername(), getHelloMessage());
        } catch (TelegramApiException e) {
            logger.debug(String.format("Ошибка создания бота **%s**.", getBotUsername()));
        } */
    }

    public static Map<Long, Settings> getUserSettings() {
        return userSettings;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    /**
     * Ответ на запрос, не являющийся командой
     */
    @Override
    public void processNonCommandUpdate(Update update) {
        Message msg = update.getMessage();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (!update.hasCallbackQuery()) {
            String userName = Utils.getUserName(msg);
            String answer = nonCommand.nonCommandExecute(chatId, userName, msg.getText());
            setAnswer(chatId, userName, answer);
        } else {
            String openPositionId = update.getCallbackQuery().getData();
            String openPositionKey = openPositionId.substring(0,
                    openPositionId.indexOf(CallbackData.CALLBACK_SEPARATOR));

            switch (openPositionKey) {
                case CallbackData.VIEW_DETAIL_BUTTON:
                    setAnswer(chatId, null,
                            Utils.getOpenPositionJobDescription(openPositionId,
                                    CallbackData.VIEW_DETAIL_BUTTON));
                    break;
                case CallbackData.COMMENT_VIEW_BUTTON:
                    setAnswer(chatId, null,
                            Utils.getOpenPositionComments(openPositionId,
                                    CallbackData.COMMENT_VIEW_BUTTON));
                    break;
                case CallbackData.SUBSCRIBE_BUTTON:
                    openPositionSubscribe(chatId, update.getCallbackQuery().getFrom().getUserName(),
                            update.getCallbackQuery().getFrom(), openPositionKey, openPositionId);
                    break;

                case CallbackData.SUBSCRIBERS_BUTTON:
                    openPositionSubscribersRecruter(chatId, openPositionKey, openPositionId);
                    break;
                default:
                    setAnswer(chatId, null, "❌ОШИБКА: Нет действия");
                    break;
            }
        }
    }

    /**
     * @param chatId
     * @param openPositionKey
     * @param openPositionCallBack
     */
    private void openPositionSubscribersRecruter(Long chatId,
                                                 String openPositionKey,
                                                 String openPositionCallBack) {
        List<RecrutiesTasks> recrutiesTasks = Utils.isOpenPositionSubscribers(openPositionKey, openPositionCallBack);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        StringBuilder sb = new StringBuilder()
                .append(String.format("\nРекрутеры подписанные на вакансию (%s): ",
                        recrutiesTasks.get(0).getOpenPosition().getVacansyName()))
                .append("\n\n");

        if (recrutiesTasks.size() > 0) {
            for (RecrutiesTasks rt : recrutiesTasks) {
                sb.append(rt.getReacrutier().getName())
                        .append(" c ")
                        .append(sdf.format(rt.getStartDate()))
                        .append(" по ")
                        .append(sdf.format(rt.getEndDate()))
                        .append(". Отправить резюме:");
                if (rt.getReacrutier().getEmail() != null)
                    sb.append(" ")
                            .append(rt.getReacrutier().getEmail());
                if (rt.getReacrutier().getTelegram() != null)
                    sb.append(" ")
                            .append(rt.getReacrutier().getTelegram().substring(0, 1).equals("@") ?
                                    rt.getReacrutier().getTelegram() :
                                    "@" + rt.getReacrutier().getTelegram());

                sb.append("\n");
            }

            setAnswer(chatId, null, sb.toString());
        }
    }

    private void openPositionSubscribe(Long chatId, String userName, User user, String openPositionKey,
                                       String openPositionCallBack) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        gregorianCalendar.add(7, GregorianCalendar.DAY_OF_MONTH);

        RecrutiesTasks recrutiesTasks = new RecrutiesTasks();
        recrutiesTasks.setSubscribe(false);
        recrutiesTasks.setOpenPosition(Utils.getOpenPosition(openPositionCallBack, openPositionKey).get(0));
        recrutiesTasks.setClosed(false);
        recrutiesTasks.setRecrutierName(user.getUserName());
        recrutiesTasks.setReacrutier(Utils.getInternalUser(userName));
        recrutiesTasks.setStartDate(new Date());
        recrutiesTasks.setEndDate(gregorianCalendar.getTime());
        recrutiesTasks.setCreatedBy(user.getUserName());
        recrutiesTasks.setCreateTs(new Date());
        recrutiesTasks.setId(UUID.randomUUID());

        if (!Utils.isRecrutiesTaskValide(recrutiesTasks,user)) {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                EntityManager em = persistence.getEntityManager();
                em.merge(recrutiesTasks);
                tx.commit();
            } catch (Exception e) {
                logger.error(String.format("Error: ошибка добавления сущности RecrutiesTask% %s", e.getMessage()));
            }

            setAnswer(chatId, null, String.format("✅User: %s (%s) подписан на вакансию %s", user.getUserName(),
                    Utils.getInternalUser(userName).getName(),
                    Utils.getOpenPosition(openPositionCallBack, openPositionKey).get(0).getVacansyName()));
        } else {
            setAnswer(chatId, null, String.format("\uD83D\uDED1User: %s (%s) не удалось подписаться на вакансию: %s.",
                    user.getUserName(),
                    Utils.getInternalUser(userName).getName(),
                    Utils.getOpenPosition(openPositionCallBack, openPositionKey).get(0).getVacansyName()));
        }
    }

    public void sendImage(Long chatId, String path) throws UserException {
        try {
            SendPhoto photo = new SendPhoto();
            photo.setPhoto(new InputFile(new File(path)));
            photo.setChatId(chatId.toString());
            execute(photo);
        } catch (TelegramApiException e) {
            logger.error(String.format("Sending image error: %s", e.getMessage()));
            throw new UserException("Ошибка отправки изображения");
        }
    }

    private void setRegisteredCommands() {
        List<String> commands = new ArrayList<>();

        commands = getRegisteredCommands()
                .stream()
                .map(IBotCommand::getCommandIdentifier)
                .collect(Collectors.toList());

        for(String command : commands) {
            registeredCommands.add(new StringBuilder("/").append(command).toString());
        }
    }

    @Override
    protected void processInvalidCommandUpdate(Update update) {
        String command = update.getMessage().getText().substring(1);
        setAnswer(update.getMessage().getChatId(),
                Utils.getUserName(update.getMessage()),
                String.format("❌Некорректная команда [%s], доступные команды: %s"
                        , command
                        , registeredCommands.toString()));
    }

    /**
     * Получение настроек по id чата. Если ранее для этого чата в ходе сеанса работы бота настройки не были установлены,
     * используются настройки по умолчанию
     */
    public static Settings getUserSettings(Long chatId) {
        Map<Long, Settings> userSettings = Bot.getUserSettings();
        Settings settings = userSettings.get(chatId);
        if (settings == null) {
            return defaultSettings;
        }
        return settings;
    }

    /**
     * Отправка ответа
     *
     * @param chatId   id чата
     * @param userName имя пользователя
     * @param text     текст ответа
     */
    private void setAnswer(Long chatId, String userName, String text) {
        SendMessage answer = new SendMessage();
        answer.setText(text);
        answer.setChatId(chatId.toString());
        answer.setParseMode(ParseMode.HTML);

        Utils.setButtons(answer);

        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(String.format("*ОШИБКА* %s.\nСообщение %s, не являющееся командой.\nПользователь: **%s**",
                    e.getMessage(), userName));
            e.printStackTrace();
        }
    }
}