package com.company.itpearls.core;

import com.company.itpearls.entity.EmailKeys;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service(EmailGenerationService.NAME)
public class EmailGenerationServiceBean implements EmailGenerationService {

    @Override
    public HashMap<String, String> generateKeys() {
        HashMap<String, String> emailKeys = new HashMap<>();

        emailKeys.put("Имя", "$first_name");
        emailKeys.put("Отчество", "$middle_name");
        emailKeys.put("Фамилия", "$second_name");
        emailKeys.put("Вакансия", "$vacancy");
        emailKeys.put("Проект", "$project");
        emailKeys.put("Компания", "$company");
        emailKeys.put("Департамент", "$departament");
        emailKeys.put("Дата", "$date");
        emailKeys.put("Время", "$time");
        emailKeys.put("Ресерчер", "$researcher_name");
        emailKeys.put("ОписаниеВакансии", "$job_description");
        emailKeys.put("Позиция", "$position");
        emailKeys.put("ЗарплатаМин", "$salary_min");
        emailKeys.put("ЗарплатаМакс", "$salary_max");

        return emailKeys;
    }
}