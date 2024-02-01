package com.company.itpearls.core;

import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@Service(StandartMapsService.NAME)
public class StandartMapsServiceBean implements StandartMapsService {
    @Inject
    private StarsAndOtherService starsAndOtherService;

    @Override
    public Map<String, Integer> setPriorityMap() {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("None", StdPriority.PRIORITY_NONE);
        priorityMap.put("Draft", StdPriority.PRIORITY_DRAFT);
        priorityMap.put("Paused", StdPriority.PRIORITY_PAUSED);
        priorityMap.put("Low", StdPriority.PRIORITY_LOW);
        priorityMap.put("Normal", StdPriority.PRIORITY_NORMAL);
        priorityMap.put("High", StdPriority.PRIORITY_HIGH);
        priorityMap.put("Critical", StdPriority.PRIORITY_CRITICAL);

        /*        priorityMap.put(StandartPriorityVacancy.DRAFT_STR,
                StandartPriorityVacancy.DRAFT_INT);
        priorityMap.put(StandartPriorityVacancy.PAUSED_STR,
                StandartPriorityVacancy.PAUSED_INT);
        priorityMap.put(StandartPriorityVacancy.LOW_STR,
                StandartPriorityVacancy.LOW_INT);
        priorityMap.put(StandartPriorityVacancy.NORMAL_STR,
                StandartPriorityVacancy.NORMAL_INT);
        priorityMap.put(StandartPriorityVacancy.HIGH_STR,
                StandartPriorityVacancy.HIGH_INT);
        priorityMap.put(StandartPriorityVacancy.CRITICAL_STR,
                StandartPriorityVacancy.CRITICAL_INT); */

        return priorityMap;
    }

    @Override
    public Map<String, Integer> setPaymentsTypeMap() {
        Map<String, Integer> paymentsType = new LinkedHashMap<>();
        paymentsType.put("Фиксированная оплата", 0);
        paymentsType.put("Процент от годового оклада", 1);
        paymentsType.put("Процент от месячной зарплаты", 2);

        return paymentsType;
    }


    @Override
            public Map<String, Integer> setResearcherSalaryMap() {
        Map<String, Integer> researcherSalary = new LinkedHashMap<>();
        researcherSalary.put("Фиксированная комиссия", 0);
        researcherSalary.put("Процент комиссии компании, 20%", 1);
        researcherSalary.put("Процент комиссии компании", 2);

        return researcherSalary;
    }

    @Override
    public Map<String, Integer> setRecruterSalaryMap() {
        Map<String, Integer> recrutierSalary = new LinkedHashMap<>();
        recrutierSalary.put("Фиксированная комиссия", 0);
        recrutierSalary.put("Процент комиссии компании, 10%", 1);
        recrutierSalary.put("Процент комиссии компании", 2);

        return recrutierSalary;
    }

    @Override
    public Map<String, Integer> setRemoteWorkMap() {
        /*        remoteWork.put(messageBundle.getMessage("msgUndefined"), -1);
        remoteWork.put(messageBundle.getMessage("msgWorkInOffice"), 0);
        remoteWork.put(messageBundle.getMessage("msgRemoteWork"), 1);
        remoteWork.put(messageBundle.getMessage("msgHybridWork"), 2); */

        Map<String, Integer> remoteWork = new LinkedHashMap<>();
        remoteWork.put("Нет", 0);
        remoteWork.put("Удаленная работа", 1);
        remoteWork.put("Частично 50/50", 2);

        return remoteWork;
    }

    @Override
    public Map<String, Integer> setOnlyOpenedPositionMap() {
        Map<String, Integer> onlyOpenedPositionMap = new LinkedHashMap<>();

        onlyOpenedPositionMap.put("Все вакансии", 2);
        onlyOpenedPositionMap.put("В подписке", 1);
        onlyOpenedPositionMap.put("Не в подписке", 0);
        onlyOpenedPositionMap.put("Свободные", 3);
        onlyOpenedPositionMap.put("Открытые за 3 дня", 4);
        onlyOpenedPositionMap.put("Открытые за неделю", 5);
        onlyOpenedPositionMap.put("Открытые за месяц", 6);
        onlyOpenedPositionMap.put("На паузе", 7);

        return onlyOpenedPositionMap;
    }

    @Override
    public Map<String, Integer> setRatingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(1))
                .append(" Полный негатив")
                .toString(), 0);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(2))
                .append(" Сомнительно")
                .toString(), 1);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(3))
                .append(" Нейтрально")
                .toString(), 2);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(4))
                .append(" Положительно")
                .toString(), 3);
        map.put(new StringBuilder()
                .append(starsAndOtherService.setStars(5))
                .append(" Отлично!")
                .toString(), 4);

        return map;
    }

    @Override
    public Map<String, Integer> setCommunicationTypeMap() {
        Map<String, Integer> communicationTypeMap = new LinkedHashMap<>();

        communicationTypeMap.put("Email", 1);
        communicationTypeMap.put("Phone", 2);
        communicationTypeMap.put("Telegramm", 3);
        communicationTypeMap.put("Skype", 4);
        communicationTypeMap.put("Viber", 5);
        communicationTypeMap.put("WhatsApp", 6);
        communicationTypeMap.put("Social Network", 7);
        communicationTypeMap.put("Other", 9);

        return communicationTypeMap;
    }

    @Override
    public Map<String, Integer> setWorkStatusMap() {
        Map<String, Integer> workStatusMap = new LinkedHashMap<>();
        workStatusMap.put("В проекте", 0);
        workStatusMap.put("На бенче", 1);
        workStatusMap.put("Ранее работал на проектах фуллтайм", 2);
        workStatusMap.put("Ранее работал на проектах парттайм", 3);

        return workStatusMap;
    }
}