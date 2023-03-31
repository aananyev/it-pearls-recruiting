package com.company.itpearls.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum EmailKeys implements EnumClass<String> {

    DATE("Дата"),
    TIME("Время"),
    FIRST_NAME("Имя"),
    DEPARTAMENT("Департамент"),
    SALARY_MAX("ЗарплатаМакс"),
    PROJECT("Проект"),
    JOB_DESCRIPTION("ОписаниеВакансии"),
    MIDDLE_NAME("Отчество"),
    VACANCY("Вакансия"),
    SALARY_MIN("ЗарплатаМин"),
    SECOND_NAME("Фамилия"),
    RESEARCHER_NAME("Ресерчер"),
    COMPANY("Компания"),
    RECRUTER_NAME("Рекрутер"),
    POSITION("Позиция");

    private String id;

    EmailKeys(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static EmailKeys fromId(String id) {
        for (EmailKeys at : EmailKeys.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}