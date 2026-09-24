package com.company.hunttech.web.widgets.recruiterdashboard;

import com.company.hunttech.entity.Iteraction;

import java.util.Locale;

/**
 * Нормализованные этапы рабочего процесса рекрутера для dashboard.
 *
 * Этап определяется по бизнес-признакам справочника Iteraction; текстовое
 * название используется только как совместимый fallback для legacy-записей.
 */
public enum RecruiterDashboardStage {
    NEW("Новые", "recruiter-stage-new", 0),
    RECRUITER_INTERVIEW("Собеседование рекрутера", "recruiter-stage-recruiter", 1),
    CLIENT("У заказчика", "recruiter-stage-client", 2),
    CLIENT_INTERVIEW("Интервью заказчика", "recruiter-stage-client-interview", 3),
    OFFER("Оффер / финал", "recruiter-stage-offer", 4),
    OUTCOME("Исход", "recruiter-stage-outcome", 5),
    RESERVE("Кадровый резерв", "recruiter-stage-reserve", -1);

    private final String caption;
    private final String styleName;
    private final int order;

    RecruiterDashboardStage(String caption, String styleName, int order) {
        this.caption = caption;
        this.styleName = styleName;
        this.order = order;
    }

    public String getCaption() {
        return caption;
    }

    public String getStyleName() {
        return styleName;
    }

    public int getOrder() {
        return order;
    }

    public static RecruiterDashboardStage resolve(Iteraction type) {
        if (type == null) {
            return NEW;
        }
        if (Boolean.TRUE.equals(type.getSignPersonalReserve())
                || Boolean.TRUE.equals(type.getSignPersonalReservePut())) {
            return RESERVE;
        }
        if (Boolean.TRUE.equals(type.getSignEndCase())) {
            return OUTCOME;
        }
        String name = type.getIterationName() == null
                ? ""
                : type.getIterationName().toLowerCase(Locale.ROOT);
        if (Boolean.TRUE.equals(type.getSignStartCase()) || name.contains("оффер") || name.contains("offer")) {
            return OFFER;
        }
        if (Boolean.TRUE.equals(type.getSignClientInterview())) {
            return CLIENT_INTERVIEW;
        }
        if (Boolean.TRUE.equals(type.getSignSendToClient())) {
            return CLIENT;
        }
        if (Boolean.TRUE.equals(type.getSignOurInterviewAssigned())
                || Boolean.TRUE.equals(type.getSignOurInterview())) {
            return RECRUITER_INTERVIEW;
        }
        if (name.contains("отказ")) {
            return OUTCOME;
        }
        return NEW;
    }
}
