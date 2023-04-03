package com.company.itpearls.core;

import com.company.itpearls.entity.EmailKeys;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service(EmailGenerationService.NAME)
public class EmailGenerationServiceBean implements EmailGenerationService {

    @Override
    public HashMap<String, String> generateKeys() {
        HashMap<String, String> emailKeys = new HashMap<>();

        emailKeys.put("ЗарплатаМакс", "$salary_max");
        emailKeys.put("ЗарплатаМин", "$salary_min");
        emailKeys.put("Вакансия", "$vacancy");
        emailKeys.put("Время", "$time");
        emailKeys.put("Дата", "$date");
        emailKeys.put("Департамент", "$departament");
        emailKeys.put("Имя", "$first_name");
        emailKeys.put("Компания", "$company");
        emailKeys.put("ОписаниеВакансии", "$job_description");
        emailKeys.put("Отчество", "$middle_name");
        emailKeys.put("Позиция", "$position");
        emailKeys.put("Проект", "$project");
        emailKeys.put("Ресерчер", "$researcher_name");
        emailKeys.put("Фамилия", "$second_name");

        return emailKeys;
    }
}