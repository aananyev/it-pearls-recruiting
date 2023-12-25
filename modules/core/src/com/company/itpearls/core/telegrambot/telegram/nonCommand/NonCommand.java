package com.company.itpearls.core.telegrambot.telegram.nonCommand;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.company.itpearls.core.telegrambot.exeptions.IllegalSettingsException;

/**
 * Обработка сообщения, не являющегося командой (т.е. обычного текста не начинающегося с "/")
 */
public class NonCommand {
    private Logger logger = LoggerFactory.getLogger(NonCommand.class);

    public String nonCommandExecute(Long chatId, String userName, String text) {
        logger.debug(String.format("Пользователь %s. Начата обработка сообщения \"%s\", не являющегося командой",
                userName, text));

        Settings settings;
        String answer;
        try {
            logger.debug(String.format("Пользователь %s. Пробуем создать объект настроек из сообщения \"%s\"",
                    userName, text));
            settings = createSettings(text);
            saveUserSettings(chatId, settings);
            logger.debug(String.format("Пользователь %s. Объект настроек из сообщения \"%s\" создан и сохранён",
                    userName, text));
            answer = String.format("Настройки обновлены. Вы всегда можете их посмотреть с помощью /settings.");
        } catch (IllegalSettingsException e) {
            logger.debug(String.format("Пользователь %s. Не удалось создать объект настроек из сообщения \"%s\". " +
                    "%s", userName, text, e.getMessage()));
            answer = e.getMessage() +
                    "\n\n❗ Настройки не были изменены. Вы всегда можете их посмотреть с помощью /settings";
        } catch (Exception e) {
            logger.debug(String.format("Пользователь %s. Не удалось создать объект настроек из сообщения \"%s\". " +
                    "%s. %s", userName, text, e.getClass().getSimpleName(), e.getMessage()));
            answer = "Простите, я не понимаю Вас. Похоже, что Вы ввели сообщение, не соответствующее формату.\n" +
                    "Возможно, Вам поможет /help";
        }

        logger.debug(String.format("Пользователь %s. Завершена обработка сообщения \"%s\", не являющегося командой",
                userName, text));
        return answer;
    }

    /**
     * Создание настроек из полученного пользователем сообщения
     * @param text текст сообщения
     * @throws IllegalArgumentException пробрасывается, если сообщение пользователя не соответствует формату
     */
    private Settings createSettings(String text) throws IllegalArgumentException {
        //отсекаем файлы, стикеры, гифки и прочий мусор
        if (text == null) {
            throw new IllegalArgumentException("Сообщение не является текстом");
        }

        text = text.replaceAll(", ", ",")//меняем ошибочный разделитель "запятая+пробел" на запятую
                .replaceAll(" ", ",");
        String[] parameters = text.split(",");

        if (parameters.length != 2) {
            throw new IllegalArgumentException(String.format("Не удалось разбить сообщение \"%s\" на 2 составляющих",
                    text));
        }

        int priority = Integer.parseInt(parameters[0]);
        Boolean publichNewVacancies = parameters[1].toLowerCase().equals("true")
                ? true : (parameters[1].toLowerCase().equals("false") ? false : null);


        validateSettings(publichNewVacancies, parameters);
        return new Settings(priority, publichNewVacancies);
    }

    /**
     * Валидация настроек
     */
    private void validateSettings(Boolean publichNewVacancies, String[] parameters) {
        if (publichNewVacancies == null)
            throw new IllegalArgumentException(
                    String.format("Значение второго параметра %s, а должно быть либо **true**, либо **false**",
                            parameters[1]));
    }

    /**
     * Добавление настроек пользователя в мапу, чтобы потом их использовать для этого пользователя при генерации файла
     * Если настройки совпадают с дефолтными, они не сохраняются, чтобы впустую не раздувать мапу
     * @param chatId   id чата
     * @param settings настройки
     */
    private void saveUserSettings(Long chatId, Settings settings) {
        if (!settings.equals(Bot.getDefaultSettings())) {
            Bot.getUserSettings().put(chatId, settings);
        } else {
            Bot.getUserSettings().remove(chatId);
        }
    }

    /**
     * Формирование оповещения о некорректных настройках
     */
    private String createSettingWarning(Settings settings) {
        return "Ошибка Settings";
    }
}