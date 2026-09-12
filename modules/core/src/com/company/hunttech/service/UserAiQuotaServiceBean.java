package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.ai.AiCallLog;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.LlmChatQuotaPeriod;
import com.company.hunttech.entity.ai.LlmUserQuotaOverride;
import com.company.hunttech.service.dto.ai.ActiveUserQuotaSummary;
import com.company.hunttech.service.dto.ai.UserAiQuotaInfo;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

@Service(UserAiQuotaService.NAME)
public class UserAiQuotaServiceBean implements UserAiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(UserAiQuotaServiceBean.class);
    private static final String FUNCTION_CODE = "LLM_CHAT";
    private static final int FALLBACK_DEFAULT_QUOTA = 100_000;

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;

    @Override
    public UserAiQuotaInfo getUserQuota(UUID userId) {
        if (userId == null) {
            return new UserAiQuotaInfo(loadDefaultMonthlyQuota(), 0, 0, 0, false);
        }

        Date today = truncateToDate(new Date());
        Date periodStart = monthStart(today);

        // 1. Загрузка персонального override
        List<LlmUserQuotaOverride> overrides = dataManager.load(LlmUserQuotaOverride.class)
                .query("select e from hunttech_LlmUserQuotaOverride e " +
                        "where e.user.id = :userId and e.effectiveFrom <= :today " +
                        "and (e.effectiveTo is null or e.effectiveTo >= :today) " +
                        "and e.deleteTs is null " +
                        "order by e.effectiveFrom desc")
                .parameter("userId", userId)
                .parameter("today", today)
                .view("llm-user-quota-override-view")
                .list();

        Integer allocatedTokens;
        boolean customOverride = false;
        if (!overrides.isEmpty()) {
            allocatedTokens = overrides.get(0).getMonthlyQuotaTokens();
            customOverride = true;
        } else {
            allocatedTokens = loadDefaultMonthlyQuota();
        }

        // 2. Загрузка расхода за текущий месяц из периода
        List<LlmChatQuotaPeriod> periods = dataManager.load(LlmChatQuotaPeriod.class)
                .query("select e from hunttech_LlmChatQuotaPeriod e " +
                        "where e.user.id = :userId and e.periodStart = :periodStart " +
                        "and e.deleteTs is null")
                .parameter("userId", userId)
                .parameter("periodStart", periodStart)
                .view("llm-chat-quota-period-view")
                .list();

        int consumed = 0;
        int reserved = 0;
        int pending = 0;

        if (!periods.isEmpty()) {
            LlmChatQuotaPeriod period = periods.get(0);
            consumed = period.getConsumedTokens() != null ? period.getConsumedTokens() : 0;
            reserved = period.getReservedTokens() != null ? period.getReservedTokens() : 0;
            pending = period.getPendingTokens() != null ? period.getPendingTokens() : 0;
        } else {
            // Если период еще не заведен, проверим вызовы за месяц из AiCallLog через агрегатный запрос
            Date nextMonth = nextMonthStart(periodStart);
            Number sumTokens = dataManager.loadValue(
                    "select coalesce(sum(e.totalTokens), 0) from hunttech_AiCallLog e " +
                    "where e.user.id = :userId and e.callTime >= :periodStart and e.callTime < :nextMonth and e.deleteTs is null", Number.class)
                    .parameter("userId", userId)
                    .parameter("periodStart", periodStart)
                    .parameter("nextMonth", nextMonth)
                    .optional()
                    .orElse(0);
            consumed = sumTokens != null ? sumTokens.intValue() : 0;
        }

        return new UserAiQuotaInfo(allocatedTokens, consumed, reserved, pending, customOverride);
    }

    @Override
    public void setMonthlyQuota(UUID userId, Integer monthlyQuotaTokens, String reason) {
        if (userId == null) {
            return;
        }

        ExtUser user = dataManager.load(ExtUser.class)
                .id(userId)
                .view("extUser-view")
                .optional()
                .orElse(null);
        if (user == null) {
            log.warn("Не удалось найти пользователя ID: {} для установки квоты", userId);
            return;
        }

        Date today = truncateToDate(new Date());
        Date periodStart = monthStart(today);

        List<LlmUserQuotaOverride> activeOverrides = dataManager.load(LlmUserQuotaOverride.class)
                .query("select e from hunttech_LlmUserQuotaOverride e " +
                        "where e.user.id = :userId and e.effectiveFrom <= :today " +
                        "and (e.effectiveTo is null or e.effectiveTo >= :today) " +
                        "order by e.effectiveFrom desc")
                .parameter("userId", userId)
                .parameter("today", today)
                .view("llm-user-quota-override-view")
                .list();

        CommitContext commitContext = new CommitContext();

        if (monthlyQuotaTokens == null) {
            // Сброс на дефолт: удаляем сегодняшние overrides, а прошлые закрываем вчерашней датой
            for (LlmUserQuotaOverride override : activeOverrides) {
                if (override.getEffectiveFrom() != null && !override.getEffectiveFrom().before(today)) {
                    commitContext.addInstanceToRemove(override);
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(today);
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    override.setEffectiveTo(cal.getTime());
                    commitContext.addInstanceToCommit(override);
                }
            }
        } else {
            if (!activeOverrides.isEmpty()) {
                LlmUserQuotaOverride current = activeOverrides.get(0);
                current.setMonthlyQuotaTokens(monthlyQuotaTokens);
                if (reason != null && !reason.trim().isEmpty()) {
                    current.setReason(reason.trim());
                }
                commitContext.addInstanceToCommit(current);
            } else {
                LlmUserQuotaOverride newOverride = metadata.create(LlmUserQuotaOverride.class);
                newOverride.setUser(user);
                newOverride.setMonthlyQuotaTokens(monthlyQuotaTokens);
                newOverride.setEffectiveFrom(today);
                newOverride.setEffectiveTo(null);
                newOverride.setReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Установлено администратором");
                commitContext.addInstanceToCommit(newOverride);
            }
        }

        // Обновляем текущий период квоты
        List<LlmChatQuotaPeriod> periods = dataManager.load(LlmChatQuotaPeriod.class)
                .query("select e from hunttech_LlmChatQuotaPeriod e " +
                        "where e.user.id = :userId and e.periodStart = :periodStart")
                .parameter("userId", userId)
                .parameter("periodStart", periodStart)
                .view("llm-chat-quota-period-view")
                .list();

        int effectiveQuota = monthlyQuotaTokens != null ? monthlyQuotaTokens : loadDefaultMonthlyQuota();
        if (!periods.isEmpty()) {
            LlmChatQuotaPeriod period = periods.get(0);
            period.setQuotaTokens(effectiveQuota);
            commitContext.addInstanceToCommit(period);
        }

        dataManager.commit(commitContext);
        log.info("Успешно обновлена квота токенов для пользователя {}: {}", user.getLogin(), monthlyQuotaTokens);
    }

    @Override
    public Map<UUID, UserAiQuotaInfo> getUsersQuotaMap(List<UUID> userIds) {
        Map<UUID, UserAiQuotaInfo> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        Date today = truncateToDate(new Date());
        Date periodStart = monthStart(today);
        int defaultQuota = loadDefaultMonthlyQuota();

        List<LlmUserQuotaOverride> overrides = dataManager.load(LlmUserQuotaOverride.class)
                .query("select e from hunttech_LlmUserQuotaOverride e " +
                        "where e.user.id in :userIds and e.effectiveFrom <= :today " +
                        "and (e.effectiveTo is null or e.effectiveTo >= :today) " +
                        "order by e.effectiveFrom desc")
                .parameter("userIds", userIds)
                .parameter("today", today)
                .view("llm-user-quota-override-view")
                .list();
        Map<UUID, Integer> overrideMap = new HashMap<>();
        for (LlmUserQuotaOverride o : overrides) {
            if (o.getUser() != null && !overrideMap.containsKey(o.getUser().getId())) {
                overrideMap.put(o.getUser().getId(), o.getMonthlyQuotaTokens());
            }
        }

        List<LlmChatQuotaPeriod> periods = dataManager.load(LlmChatQuotaPeriod.class)
                .query("select e from hunttech_LlmChatQuotaPeriod e " +
                        "where e.user.id in :userIds and e.periodStart = :periodStart")
                .parameter("userIds", userIds)
                .parameter("periodStart", periodStart)
                .view("llm-chat-quota-period-view")
                .list();
        Map<UUID, LlmChatQuotaPeriod> periodMap = new HashMap<>();
        for (LlmChatQuotaPeriod p : periods) {
            if (p.getUser() != null) {
                periodMap.put(p.getUser().getId(), p);
            }
        }

        for (UUID uid : userIds) {
            Integer allocated = overrideMap.get(uid);
            boolean custom = allocated != null;
            if (allocated == null) {
                allocated = defaultQuota;
            }
            LlmChatQuotaPeriod p = periodMap.get(uid);
            int consumed = p != null && p.getConsumedTokens() != null ? p.getConsumedTokens() : 0;
            int reserved = p != null && p.getReservedTokens() != null ? p.getReservedTokens() : 0;
            int pending = p != null && p.getPendingTokens() != null ? p.getPendingTokens() : 0;
            result.put(uid, new UserAiQuotaInfo(allocated, consumed, reserved, pending, custom));
        }

        return result;
    }

    @Override
    public List<ActiveUserQuotaSummary> getActiveUsersQuotaSummary(Date from, Date to) {
        // 1. Загрузка ВСЕХ активных пользователей
        List<ExtUser> activeUsers = dataManager.load(ExtUser.class)
                .query("select u from hunttech_ExtUser u where u.active = true order by u.name")
                .view("extUser-view")
                .list();

        if (activeUsers.isEmpty()) {
            return Collections.emptyList();
        }

        Date today = truncateToDate(new Date());
        Date periodStart = monthStart(today);
        int defaultQuota = loadDefaultMonthlyQuota();

        // 2. Предзагрузка всех активных overrides
        List<LlmUserQuotaOverride> allOverrides = dataManager.load(LlmUserQuotaOverride.class)
                .query("select e from hunttech_LlmUserQuotaOverride e " +
                        "where e.effectiveFrom <= :today " +
                        "and (e.effectiveTo is null or e.effectiveTo >= :today) " +
                        "order by e.effectiveFrom desc")
                .parameter("today", today)
                .view("llm-user-quota-override-view")
                .list();
        Map<UUID, Integer> overrideMap = new HashMap<>();
        for (LlmUserQuotaOverride o : allOverrides) {
            if (o.getUser() != null && !overrideMap.containsKey(o.getUser().getId())) {
                overrideMap.put(o.getUser().getId(), o.getMonthlyQuotaTokens());
            }
        }

        // 3. Предзагрузка периодов текущего месяца
        List<LlmChatQuotaPeriod> allPeriods = dataManager.load(LlmChatQuotaPeriod.class)
                .query("select e from hunttech_LlmChatQuotaPeriod e where e.periodStart = :periodStart")
                .parameter("periodStart", periodStart)
                .view("llm-chat-quota-period-view")
                .list();
        Map<UUID, Integer> periodConsumedMap = new HashMap<>();
        Map<UUID, Integer> periodTotalUsedMap = new HashMap<>();
        for (LlmChatQuotaPeriod p : allPeriods) {
            if (p.getUser() != null) {
                int consumed = p.getConsumedTokens() != null ? p.getConsumedTokens() : 0;
                int reserved = p.getReservedTokens() != null ? p.getReservedTokens() : 0;
                int pending = p.getPendingTokens() != null ? p.getPendingTokens() : 0;
                periodConsumedMap.put(p.getUser().getId(), consumed);
                periodTotalUsedMap.put(p.getUser().getId(), consumed + reserved + pending);
            }
        }

        // 4. Предзагрузка логов за запрошенный период (для calls, cost, errorCount, lastCallTime)
        List<AiCallLog> periodLogs = Collections.emptyList();
        if (from != null && to != null) {
            periodLogs = dataManager.load(AiCallLog.class)
                    .query("select e from hunttech_AiCallLog e where e.callTime >= :from and e.callTime <= :to")
                    .parameter("from", from)
                    .parameter("to", to)
                    .view("ai-call-log-browse-view")
                    .list();
        }

        Map<UUID, List<AiCallLog>> userLogsMap = new HashMap<>();
        for (AiCallLog logItem : periodLogs) {
            if (logItem.getUser() != null) {
                userLogsMap.computeIfAbsent(logItem.getUser().getId(), k -> new ArrayList<>()).add(logItem);
            }
        }

        // 5. Формирование итогового списка по каждому активному пользователю
        List<ActiveUserQuotaSummary> result = new ArrayList<>();
        for (ExtUser u : activeUsers) {
            ActiveUserQuotaSummary summary = new ActiveUserQuotaSummary();
            summary.setUserId(u.getId());
            summary.setUserLogin(u.getLogin());
            summary.setUserName(u.getName() != null && !u.getName().trim().isEmpty() ? u.getName() : u.getLogin());
            summary.setActive(Boolean.TRUE.equals(u.getActive()));

            Integer quota = overrideMap.get(u.getId());
            if (quota != null) {
                summary.setAllocatedTokens(quota);
                summary.setCustomOverride(true);
            } else {
                summary.setAllocatedTokens(defaultQuota);
                summary.setCustomOverride(false);
            }

            boolean isUnlimited = summary.getAllocatedTokens() != null &&
                    (summary.getAllocatedTokens() == -1 || summary.getAllocatedTokens() == Integer.MAX_VALUE);
            summary.setUnlimited(isUnlimited);

            int monthConsumed = periodConsumedMap.getOrDefault(u.getId(), 0);
            int monthTotalUsed = periodTotalUsedMap.getOrDefault(u.getId(), 0);
            summary.setConsumedTokens(monthConsumed);

            if (isUnlimited) {
                summary.setRemainingTokens(null);
            } else if (summary.getAllocatedTokens() == null) {
                summary.setRemainingTokens(0);
            } else {
                summary.setRemainingTokens(Math.max(0, summary.getAllocatedTokens() - monthTotalUsed));
            }

            // Статистика логов за фильтрованный период
            List<AiCallLog> uLogs = userLogsMap.getOrDefault(u.getId(), Collections.emptyList());
            summary.setTotalCalls(uLogs.size());
            BigDecimal cost = BigDecimal.ZERO;
            int errors = 0;
            Date lastTime = null;

            for (AiCallLog logItem : uLogs) {
                if (logItem.getEstimatedCost() != null) {
                    cost = cost.add(logItem.getEstimatedCost());
                }
                if (!"SUCCESS".equalsIgnoreCase(logItem.getStatus())) {
                    errors++;
                }
                if (logItem.getCallTime() != null) {
                    if (lastTime == null || logItem.getCallTime().after(lastTime)) {
                        lastTime = logItem.getCallTime();
                    }
                }
            }

            summary.setEstimatedCost(cost);
            summary.setErrorCount(errors);
            summary.setLastCallTime(lastTime);

            result.add(summary);
        }

        return result;
    }

    @Override
    public int loadDefaultMonthlyQuota() {
        try {
            return dataManager.load(AiFunctionConfiguration.class)
                    .query("select e from hunttech_AiFunctionConfiguration e where e.code = :code and e.active = true and e.deleteTs is null")
                    .parameter("code", FUNCTION_CODE)
                    .view("ai-function-configuration-browse-view")
                    .optional()
                    .map(AiFunctionConfiguration::getDefaultMonthlyTokenQuota)
                    .filter(q -> q != null && (q == -1 || q == Integer.MAX_VALUE || q > 0))
                    .orElse(FALLBACK_DEFAULT_QUOTA);
        } catch (Exception e) {
            log.warn("Не удалось прочитать дефолтную квоту для {}, используется fallback: {}", FUNCTION_CODE, FALLBACK_DEFAULT_QUOTA);
            return FALLBACK_DEFAULT_QUOTA;
        }
    }

    private Date truncateToDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date monthStart(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date nextMonthStart(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
