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
    NEW("Новые", "recruiter-stage-new"),
    RECRUITER_INTERVIEW("Собеседование рекрутера", "recruiter-stage-recruiter"),
    CLIENT("У заказчика", "recruiter-stage-client"),
    CLIENT_INTERVIEW("Интервью заказчика", "recruiter-stage-client-interview"),
    OFFER("Оффер / финал", "recruiter-stage-offer"),
    OUTCOME("Исход", "recruiter-stage-outcome"),
    RESERVE("Кадровый резерв", "recruiter-stage-reserve");

    private final String caption;
    private final String styleName;

    RecruiterDashboardStage(String caption, String styleName) {
        this.caption = caption;
        this.styleName = styleName;
    }

    public String getCaption() {
        return caption;
    }

    public String getStyleName() {
        return styleName;
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
