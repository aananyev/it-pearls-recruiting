
package com.company.itpearls.core.telegrambot.telegram;

import com.company.itpearls.core.telegrambot.TelegramBotStatus;
import com.company.itpearls.core.telegrambot.exeptions.UserException;
import com.company.itpearls.core.telegrambot.telegram.commands.constant.CallbackData;
import com.company.itpearls.core.telegrambot.telegram.commands.service.*;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.OpenPositionPriority;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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

        register(new PersonPositionCommand("positions", "Должности"));
        logger.debug("Команда positions создана");

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
        TelegramBotStatus.setChatId(chatId);

        if (!update.hasCallbackQuery()) {
            String userName = Utils.getUserName(msg);
            String answer = nonCommand.nonCommandExecute(chatId, userName, msg.getText());
            setAnswer(chatId, userName, answer);
        } else {
            String openPositionId = update.getCallbackQuery().getData();
            String openPositionKey;
            String openPositionUUID = null;

            if (openPositionId.indexOf(CallbackData.CALLBACK_SEPARATOR) != -1) {
                openPositionKey = openPositionId.substring(0,
                        openPositionId.indexOf(CallbackData.CALLBACK_SEPARATOR));

                openPositionUUID = openPositionId.substring(openPositionKey.length() + 1);
            } else {
                openPositionKey = openPositionId;
            }

            switch (openPositionKey) {
                case CallbackData.TRUE_CV_PUBLISH_BUTTON:
                case CallbackData.FALSE_CV_PUBLISH_BUTTON:
                    setSettingCVPublish(openPositionKey.equalsIgnoreCase(CallbackData.TRUE_CV_PUBLISH_BUTTON),
                            chatId);
                    break;
                case CallbackData.PAUSE_PRIORITY_BUTTON:
                case CallbackData.LOW_PRIORITY_BUTTON:
                case CallbackData.NORMAL_PRIORITY_BUTTON:
                case CallbackData.HIHG_PRIORITY_BUTTON:
                case CallbackData.CRITICAL_PRIORITY_BUTTON:
                    setSettingPriority(openPositionKey, chatId);
                    break;
                case CallbackData.SEND_CV_TO_COORDINATOR:
                    sendAnswer(chatId, new StringBuilder("\uD83D\uDCC3Отошлите резюме в формате wod или pdf координатору ")
                            .append(Utils.getCoordinatorTelegram()).toString());
                    break;
                case CallbackData.VIEW_DETAIL_BUTTON:
                    sendAnswer(chatId,
                            Utils.getOpenPositionJobDescription(update.getCallbackQuery().getFrom(),
                                    openPositionId,
                                    CallbackData.VIEW_DETAIL_BUTTON),
                            Utils.getSendSubscribersKeyboard(openPositionUUID));
                    break;
                case CallbackData.COMMENT_VIEW_BUTTON:
                    sendAnswer(chatId,
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
                case CallbackData.VIEW_VACANSIES_BUTTON:
                    positionOpenPositionView(chatId, update.getCallbackQuery());
                    break;
                default:
                    setAnswer(chatId, null, "❌ОШИБКА: Нет действия");
                    break;
            }
        }
    }

    private void setSettingPriority(String openPositionKey, Long chatId) {
        Settings settings = Bot.getUserSettings(TelegramBotStatus.getChatId());

        int priority;
        priority = OpenPositionPriority.fromName(openPositionKey.substring(0, openPositionKey.indexOf("PriorityButton"))).getId();

        settings.setPriorityNotLower(priority);

        Bot.setUserSettings(TelegramBotStatus.getChatId(), settings);
        sendAnswer(chatId, new StringBuilder().append(Utils.getBotName())
                .append("\n")
                .append("⚙\uFE0FНАСТРОЙКИ СОХРАНЕНЫ").toString());
    }

    private void setSettingCVPublish(Boolean publish, Long chatId) {
        Settings settings = Bot.getUserSettings(TelegramBotStatus.getChatId());

        settings.setPublishNewVacancies(publish);

        Bot.setUserSettings(TelegramBotStatus.getChatId(), settings);
        sendAnswer(chatId, new StringBuilder().append(Utils.getBotName())
                .append("\n")
                .append("⚙\uFE0FНАСТРОЙКИ СОХРАНЕНЫ").toString());
    }

    private void positionOpenPositionView(Long chatId, CallbackQuery callbackQuery) {
        String positionId = callbackQuery.getData();
        String positionKey = positionId.substring(
                positionId.indexOf(CallbackData.CALLBACK_SEPARATOR) + 1, positionId.length());
        Boolean subscribeFlag = Utils.isInternalUser(callbackQuery.getFrom());

        List<OpenPosition> openPositions = Utils.getPositonOpenPosition(chatId, positionId, positionKey);
        int counter = 0;

        StringBuilder sb = new StringBuilder(Utils.getBotName())
                .append("\n")
                .append("Список вакансий для должности <b>\"")
                .append(Utils.getPositionUUID(positionId.substring(positionId.indexOf(CallbackData.CALLBACK_SEPARATOR) + 1, positionId.length())))
                .append("\"</b>\n")
                .append("Всего вакансий: <b>")
                .append(openPositions.size())
                .append("</b>\n\n");

        sendAnswer(chatId, sb.toString());

        for (OpenPosition openPosition : openPositions) {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(++counter)
                    .append(". ")
                    .append(openPosition.getVacansyName())
                    .append(" (Количество человек - <b>")
                    .append(openPosition.getNumberPosition())
                    .append("</b>)");
            sendAnswer(chatId, stringBuilder.toString(),
                    VacancyListCommand.setInline(openPosition, subscribeFlag));
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
                .append(String.format("\nРекрутеры подписанные на вакансию (<b>%s</b>): ",
                        recrutiesTasks.get(0).getOpenPosition().getVacansyName()))
                .append("\n\n");

        if (recrutiesTasks.size() > 0) {
            for (RecrutiesTasks rt : recrutiesTasks) {
                sb.append(rt.getReacrutier().getName())
                        .append(" c <b>")
                        .append(sdf.format(rt.getStartDate()))
                        .append("</b> по <b>")
                        .append(sdf.format(rt.getEndDate()))
                        .append("</b>. Отправить резюме:");
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

        if (!Utils.isRecrutiesTaskValide(recrutiesTasks, user)) {
            Persistence persistence = AppBeans.get(Persistence.class);
            try (Transaction tx = persistence.createTransaction()) {
                EntityManager em = persistence.getEntityManager();
                em.merge(recrutiesTasks);
                tx.commit();
            } catch (Exception e) {
                logger.error(String.format("Error: ошибка добавления сущности RecrutiesTask% %s", e.getMessage()));
            }

            setAnswer(chatId, null, String.format("✅User: <b>%s</b> (<b>%s</b>) подписан на вакансию <b>%s</b>", user.getUserName(),
                    Utils.getInternalUser(userName).getName(),
                    Utils.getOpenPosition(openPositionCallBack, openPositionKey).get(0).getVacansyName()));
        } else {
            setAnswer(chatId, null, String.format("\uD83D\uDED1User: <b>%s</b> (<b>%s</b>) не удалось подписаться на вакансию: <b>%s</b>.",
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

        for (String command : commands) {
            registeredCommands.add(new StringBuilder("/").append(command).toString());
        }
    }

    @Override
    protected void processInvalidCommandUpdate(Update update) {
        String command = update.getMessage().getText().substring(1);
        setAnswer(update.getMessage().getChatId(),
                Utils.getUserName(update.getMessage()),
                String.format("❌Некорректная команда [<b>%s</b>], доступные команды: %s"
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
        TelegramBotStatus.setChatId(chatId);
        if (settings == null) {
            userSettings.put(chatId, defaultSettings);
            return defaultSettings;
        }
        return settings;
    }

    public static void setUserSettings(Long chatId, Settings settings) {
        userSettings.get(chatId).setPriorityNotLower(settings.getPriorityNotLower());
        userSettings.get(chatId).setPublishNewVacancies(settings.getPublishNewVacancies());
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
        answer.setText(new StringBuilder()
                .append(text)
                .toString());
        answer.setChatId(chatId.toString());
        answer.setParseMode(ParseMode.HTML);

        Utils.setButtons(answer);

        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(String.format("ОШИБКА %s.\nСообщение %s, не являющееся командой.\nПользователь: %s",
                    e.getMessage(), userName));
            sendAnswer(chatId, "\uD83E\uDD28Ошибка вывода сообщения. Обратитесь к администратору системы.");
            e.printStackTrace();
        }
    }

    void sendAnswer(Long chatId, String text,
                    InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.setParseMode(ParseMode.HTML);
        message.setChatId(chatId.toString());
        message.setText(new StringBuilder()
                .append(text)
                .toString());
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Ошибка %s.", e.getMessage()));
            sendAnswer(chatId, "\uD83E\uDD28Ошибка вывода сообщения. Обратитесь к администратору системы.");
            e.printStackTrace();
        }
    }

    public void sendAnswer(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.setParseMode(ParseMode.HTML);
        message.setChatId(chatId.toString());
        message.setText(new StringBuilder()
                .append(text).toString());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Ошибка %s.", e.getMessage()));
            sendAnswer(chatId, "\uD83E\uDD28Ошибка вывода сообщения. Обратитесь к администратору системы.");
            e.printStackTrace();
        }
    }

    public void sendAnswerWithSettings(Long chatId, String text) {
        if (getUserSettings(chatId).getPublishNewVacancies()) {
            sendAnswer(chatId, text);
        }
    }
}