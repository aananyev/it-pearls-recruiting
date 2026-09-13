package com.company.hunttech.service;

import com.company.hunttech.entity.ai.LlmChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Строитель контекста для локального LLM-чата HuntTech.
 * 
 * Обеспечивает:
 * 1. Детерминированное формирование RuntimeContext: текущая дата, время, таймзона,
 *    день недели, математически точные календарные диапазоны (сегодня, завтра,
 *    вчера, эта неделя, следующая неделя, текущий/следующий месяц).
 * 2. Скользящее окно истории (sliding window): сохранение последних релевантных сообщений
 *    диалога в пределах бюджета токенов вместо деструктивного обнуления истории.
 * 3. Форматирование структурированного payload без смешивания ролей.
 */
public class LlmChatContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(LlmChatContextBuilder.class);

    private static final String[] RU_DAYS_OF_WEEK = {
            "", "Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"
    };

    private static final String[] EN_DAYS_OF_WEEK = {
            "", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private static final String[] RU_MONTHS = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    /**
     * Формирует структурированный блок RUNTIME CONTEXT с вычисленными календарными точками отсчёта.
     *
     * @param now      момент времени (если null — текущее системное время)
     * @param timeZone часовой пояс (если null — Europe/Moscow)
     * @param locale   локаль (если null — ru_RU)
     * @return строковый блок с авторитетным контекстом даты и времени
     */
    public static String buildRuntimeContext(Date now, TimeZone timeZone, Locale locale) {
        Date effectiveNow = (now != null) ? now : new Date();
        TimeZone effectiveTz = (timeZone != null) ? timeZone : TimeZone.getTimeZone("Europe/Moscow");
        Locale effectiveLocale = (locale != null) ? locale : new Locale("ru", "RU");

        Calendar cal = Calendar.getInstance(effectiveTz, effectiveLocale);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setTime(effectiveNow);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", effectiveLocale);
        dateFormat.setTimeZone(effectiveTz);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", effectiveLocale);
        timeFormat.setTimeZone(effectiveTz);

        String currentDateStr = dateFormat.format(effectiveNow);
        String currentTimeStr = timeFormat.format(effectiveNow);

        int dayOfWeekIdx = cal.get(Calendar.DAY_OF_WEEK);
        String ruDay = (dayOfWeekIdx >= 1 && dayOfWeekIdx <= 7) ? RU_DAYS_OF_WEEK[dayOfWeekIdx] : "";
        String enDay = (dayOfWeekIdx >= 1 && dayOfWeekIdx <= 7) ? EN_DAYS_OF_WEEK[dayOfWeekIdx] : "";

        // Смещение часового пояса (корректная обработка отрицательных смещений с минутами)
        int offsetMs = effectiveTz.getOffset(effectiveNow.getTime());
        int totalMinutes = offsetMs / (1000 * 60);
        int offsetHours = totalMinutes / 60;
        int offsetMins = Math.abs(totalMinutes % 60);
        String tzOffsetStr = String.format("UTC%+03d:%02d", offsetHours, offsetMins);

        // Вчера (Yesterday)
        Calendar yesterdayCal = (Calendar) cal.clone();
        yesterdayCal.add(Calendar.DAY_OF_MONTH, -1);
        String yesterdayStr = dateFormat.format(yesterdayCal.getTime());

        // Завтра (Tomorrow)
        Calendar tomorrowCal = (Calendar) cal.clone();
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
        String tomorrowStr = dateFormat.format(tomorrowCal.getTime());

        // Послезавтра (Day after tomorrow)
        Calendar afterTomorrowCal = (Calendar) cal.clone();
        afterTomorrowCal.add(Calendar.DAY_OF_MONTH, 2);
        String afterTomorrowStr = dateFormat.format(afterTomorrowCal.getTime());

        // Вычисление текущей недели (Понедельник — Воскресенье)
        // В Java: Sunday = 1, Monday = 2, ..., Saturday = 7.
        int daysFromMonday = (dayOfWeekIdx == Calendar.SUNDAY) ? 6 : (dayOfWeekIdx - Calendar.MONDAY);
        Calendar weekCal = (Calendar) cal.clone();
        weekCal.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        Date mondayThisWeek = weekCal.getTime();
        weekCal.add(Calendar.DAY_OF_MONTH, 6);
        Date sundayThisWeek = weekCal.getTime();
        String thisWeekStr = dateFormat.format(mondayThisWeek) + " — " + dateFormat.format(sundayThisWeek);

        // Вычисление следующей недели (Понедельник — Воскресенье)
        weekCal.setTime(mondayThisWeek);
        weekCal.add(Calendar.DAY_OF_MONTH, 7);
        Date mondayNextWeek = weekCal.getTime();
        weekCal.add(Calendar.DAY_OF_MONTH, 6);
        Date sundayNextWeek = weekCal.getTime();
        String nextWeekStr = dateFormat.format(mondayNextWeek) + " — " + dateFormat.format(sundayNextWeek);

        // Прошлая неделя
        weekCal.setTime(mondayThisWeek);
        weekCal.add(Calendar.DAY_OF_MONTH, -7);
        Date mondayLastWeek = weekCal.getTime();
        weekCal.add(Calendar.DAY_OF_MONTH, 6);
        Date sundayLastWeek = weekCal.getTime();
        String lastWeekStr = dateFormat.format(mondayLastWeek) + " — " + dateFormat.format(sundayLastWeek);

        // Текущий месяц
        Calendar monthCal = (Calendar) cal.clone();
        int monthIdx = monthCal.get(Calendar.MONTH);
        int year = monthCal.get(Calendar.YEAR);
        String currentMonthName = monthCal.getDisplayName(Calendar.MONTH, Calendar.LONG, effectiveLocale);
        if (currentMonthName == null && monthIdx >= 0 && monthIdx < 12) {
            currentMonthName = RU_MONTHS[monthIdx];
        }
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayThisMonth = monthCal.getTime();
        int maxDayThisMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        monthCal.set(Calendar.DAY_OF_MONTH, maxDayThisMonth);
        Date lastDayThisMonth = monthCal.getTime();
        String thisMonthStr = currentMonthName + " " + year + " (" + dateFormat.format(firstDayThisMonth) + " — " + dateFormat.format(lastDayThisMonth) + ")";

        // Следующий месяц
        Calendar nextMonthCal = (Calendar) cal.clone();
        nextMonthCal.add(Calendar.MONTH, 1);
        int nextMonthIdx = nextMonthCal.get(Calendar.MONTH);
        int nextYear = nextMonthCal.get(Calendar.YEAR);
        String nextMonthName = nextMonthCal.getDisplayName(Calendar.MONTH, Calendar.LONG, effectiveLocale);
        if (nextMonthName == null && nextMonthIdx >= 0 && nextMonthIdx < 12) {
            nextMonthName = RU_MONTHS[nextMonthIdx];
        }
        nextMonthCal.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayNextMonth = nextMonthCal.getTime();
        int maxDayNextMonth = nextMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        nextMonthCal.set(Calendar.DAY_OF_MONTH, maxDayNextMonth);
        Date lastDayNextMonth = nextMonthCal.getTime();
        String nextMonthStr = nextMonthName + " " + nextYear + " (" + dateFormat.format(firstDayNextMonth) + " — " + dateFormat.format(lastDayNextMonth) + ")";

        StringBuilder sb = new StringBuilder();
        sb.append("=== АКТУАЛЬНЫЙ RUNTIME CONTEXT СИСТЕМЫ (ИСТОЧНИК ИСТИНЫ) ===\n");
        sb.append("• Текущая дата (Current date): ").append(currentDateStr).append("\n");
        sb.append("• Текущее время (Current time): ").append(currentTimeStr).append("\n");
        sb.append("• Часовой пояс (Timezone): ").append(effectiveTz.getID()).append(" (").append(tzOffsetStr).append(")\n");
        sb.append("• День недели (Day of week): ").append(ruDay).append(" (").append(enDay).append(")\n");
        sb.append("• Стандарт недели: Понедельник (Monday) — 1-й день недели, Воскресенье (Sunday) — последний день.\n\n");

        sb.append("ТОЧНЫЕ КАЛЕНДАРНЫЕ ДИАПАЗОНЫ ДЛЯ ОТНОСИТЕЛЬНЫХ ДАТ:\n");
        sb.append("- Сегодня (Today): ").append(currentDateStr).append("\n");
        sb.append("- Вчера (Yesterday): ").append(yesterdayStr).append("\n");
        sb.append("- Завтра (Tomorrow): ").append(tomorrowStr).append("\n");
        sb.append("- Послезавтра (Day after tomorrow): ").append(afterTomorrowStr).append("\n");
        sb.append("- Текущая неделя (This week): ").append(thisWeekStr).append("\n");
        sb.append("- Следующая неделя (Next week): ").append(nextWeekStr).append("\n");
        sb.append("- Прошлая неделя (Last week): ").append(lastWeekStr).append("\n");
        sb.append("- Текущий месяц (This month): ").append(thisMonthStr).append("\n");
        sb.append("- Следующий месяц (Next month): ").append(nextMonthStr).append("\n\n");

        sb.append("ОБЯЗАТЕЛЬНЫЕ ПРАВИЛА ИНТЕРПРЕТАЦИИ ВРЕМЕНИ:\n");
        sb.append("1. Все относительные временные выражения («сегодня», «завтра», «послезавтра», «вчера», «на этой неделе», «на следующей неделе», «в следующем месяце», «через N дней») рассчитывай ИСКЛЮЧИТЕЛЬНО от Current date (").append(currentDateStr).append(").\n");
        sb.append("2. Выражение «на следующей неделе» означает строго диапазон следующей недели: ").append(nextWeekStr).append(".\n");
        sb.append("3. Выражение «завтра» означает строго дату ").append(tomorrowStr).append(".\n");
        sb.append("4. Выражение «послезавтра» означает строго дату ").append(afterTomorrowStr).append(".\n");
        sb.append("5. Выражение «через N дней» означает прибавление N дней к Current date (").append(currentDateStr).append(").\n");
        sb.append("6. Если в запросе или контексте указана конкретная дата события (например, дата создания вакансии, дата интервью), четко отличай дату события от текущей даты выполнения запроса.\n");
        sb.append("7. Если смысл временного выражения пользователя критически неоднозначен, задай краткий уточняющий вопрос.");

        return sb.toString();
    }

    /**
     * Формирует блок истории диалога с использованием скользящего окна (sliding window).
     * Сохраняет самые последние сообщения диалога, помещающиеся в доступный бюджет токенов,
     * исключая потерю контекста при достижении лимита.
     *
     * @param historyMessages      полный список сообщений диалога, отсортированных по sequenceNo asc
     * @param maxContextTokens     максимальный лимит токенов контекста модели
     * @param currentMessageText   текст текущего пользовательского сообщения
     * @param runtimeContextText   текст runtime-контекста
     * @return строка с блоком [История диалога] или пустая строка, если история отсутствует/не помещается
     */
    public static String buildSlidingHistory(List<LlmChatMessage> historyMessages,
                                             int maxContextTokens,
                                             String currentMessageText,
                                             String runtimeContextText) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return "";
        }

        int currentTokens = estimateTokens(currentMessageText);
        int runtimeTokens = estimateTokens(runtimeContextText);
        int safetyReserveTokens = 150; // Резерв на обвязку и метаданные

        int budget = maxContextTokens - currentTokens - runtimeTokens - safetyReserveTokens;
        if (budget <= 50) {
            log.info("Бюджет токенов для истории исчерпан (макс: {}, текущий: {}, runtime: {}), история сокращена.",
                    maxContextTokens, currentTokens, runtimeTokens);
            return "";
        }

        // Отбираем сообщения с конца (от самых свежих к старым)
        List<LlmChatMessage> selected = new ArrayList<>();
        int accumulatedTokens = 0;

        for (int i = historyMessages.size() - 1; i >= 0; i--) {
            LlmChatMessage msg = historyMessages.get(i);
            String roleCaption = "USER".equalsIgnoreCase(msg.getRole()) ? "Пользователь" : "Ассистент";
            String content = msg.getContent() != null ? msg.getContent() : "";
            String line = roleCaption + ": " + content + "\n";
            int lineTokens = estimateTokens(line);

            if (accumulatedTokens + lineTokens > budget) {
                // Больше старых сообщений не помещается — останавливаемся
                break;
            }
            selected.add(msg);
            accumulatedTokens += lineTokens;
        }

        if (selected.isEmpty()) {
            return "";
        }

        // Возвращаем в хронологический порядок (asc)
        Collections.reverse(selected);

        StringBuilder sb = new StringBuilder();
        sb.append("[История диалога]\n");
        for (LlmChatMessage msg : selected) {
            String roleCaption = "USER".equalsIgnoreCase(msg.getRole()) ? "Пользователь" : "Ассистент";
            String content = msg.getContent() != null ? msg.getContent() : "";
            sb.append(roleCaption).append(": ").append(content).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Объединяет историю диалога и текущее сообщение в структурированный payload.
     */
    public static String combinePayload(String slidingHistoryText, String currentMessageText) {
        if (currentMessageText == null) {
            return "";
        }
        if (slidingHistoryText == null || slidingHistoryText.trim().isEmpty()) {
            return currentMessageText.trim();
        }
        return slidingHistoryText.trim() + "\n\n[Текущий запрос пользователя]\n" + currentMessageText.trim();
    }

    /**
     * Консервативная оценка количества токенов в строке для кириллицы и мультиязычного текста (~0.6 токена на символ).
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int len = text.codePointCount(0, text.length());
        return Math.max(1, (len * 3 + 4) / 5);
    }
}
