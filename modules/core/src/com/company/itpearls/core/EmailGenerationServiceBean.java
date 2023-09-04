package com.company.itpearls.core;

import com.company.itpearls.entity.*;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.crypto.Data;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;

@Service(EmailGenerationService.NAME)
public class EmailGenerationServiceBean implements EmailGenerationService {

    @Inject
    private Logger log;
    @Inject
    private StrSimpleService strSimpleService;

    @Override
    public String preparingMessage(String text,
                                   JobCandidate jobCandidate,
                                   ExtUser user) {
        return preparingMessage(text, jobCandidate, null, user, null);
    }

    @Override
    public String preparingMessage(String text,
                                   ExtUser user,
                                   Data addDate) {
        return preparingMessage(text, null, null, user, addDate);
    }

    @Override
    public String preparingMessage(String text,
                                   JobCandidate jobCandidate,
                                   ExtUser user,
                                   Data addDate) {
        return preparingMessage(text, jobCandidate, null, user, addDate);
    }

    @Override
    public String preparingMessage(String text,
                                   OpenPosition openPosition) {
        return preparingMessage(text, null, openPosition, null, null);
    }

    @Override
    public String preparingMessage(String text,
                                   JobCandidate jobCandidate,
                                   OpenPosition openPosition,
                                   ExtUser user) {
        return preparingMessage(text, jobCandidate, openPosition, user, null);
    }

    @Override
    public String preparingMessage(String text,
                                   JobCandidate jobCandidate,
                                   OpenPosition openPosition,
                                   ExtUser user,
                                   Data addDate) {
        HashMap<String, String> emailKeys = generateKeys();

        if (text != null) {
            if (!text.equals("")) {
                StringBuffer textBuff = new StringBuffer(text);

                String retStr = textBuff.toString();

                try {
                    if (addDate != null) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
                        retStr = strSimpleService.replaceAll(retStr, emailKeys.get(EmailKeys.DATE.getId()),
                                simpleDateFormat.format(addDate));

                        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("hh:mm");
                        simpleDateFormat1.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
                        retStr = strSimpleService.replaceAll(retStr,
                                emailKeys.get(EmailKeys.TIME.getId()),
                                simpleDateFormat1.format(addDate));
                    }

                    if (jobCandidate != null) {
                        if (jobCandidate.getFirstName() != null) {
                            retStr = strSimpleService.replaceAll(retStr,
                                    emailKeys.get(EmailKeys.FIRST_NAME.getId()),
                                    jobCandidate.getFirstName());
                        }

                        if (jobCandidate.getFirstName() != null) {
                            retStr = strSimpleService.replaceAll(retStr,
                                    emailKeys.get(EmailKeys.MIDDLE_NAME.getId()),
                                    jobCandidate.getMiddleName());
                        }

                        if (jobCandidate.getMiddleName() != null) {
                            retStr = strSimpleService.replaceAll(retStr,
                                    emailKeys.get(EmailKeys.SECOND_NAME.getId()),
                                    jobCandidate.getSecondName());
                        }
                    }

                    if (openPosition != null) {
                        retStr = strSimpleService.replaceAll(retStr,
                                emailKeys.get(EmailKeys.JOB_DESCRIPTION.getId()),
                                openPosition.getComment());

                        retStr = strSimpleService.replaceAll(retStr,
                                emailKeys.get(EmailKeys.SALARY_MIN.getId()),
                                openPosition.getSalaryMin().toString());

                        retStr = strSimpleService.replaceAll(retStr,
                                emailKeys.get(EmailKeys.SALARY_MAX.getId()),
                                openPosition.getSalaryMax().toString());

                        if (openPosition.getPositionType() != null) {
                            if (openPosition.getPositionType().getPositionEnName() != null) {
                                retStr = strSimpleService.replaceAll(retStr,
                                        emailKeys.get(EmailKeys.POSITION.getId()),
                                        openPosition.getPositionType().getPositionEnName());
                            }
                        }

                        if (openPosition.getProjectName() != null) {
                            if (openPosition.getProjectName().getProjectName() != null) {
                                retStr = strSimpleService.replaceAll(retStr,
                                        emailKeys.get(EmailKeys.PROJECT.getId()),
                                        openPosition.getProjectName().getProjectName());
                            }


                            if (openPosition.getProjectName().getProjectDepartment() != null) {
                                if (openPosition.getProjectName().getProjectDepartment().getCompanyName() != null) {
                                    if (openPosition.getProjectName().getProjectDepartment().getCompanyName().getComanyName() != null) {
                                        retStr = strSimpleService.replaceAll(retStr,
                                                emailKeys.get(EmailKeys.COMPANY.getId()),
                                                openPosition.getProjectName().getProjectDepartment().getCompanyName().getComanyName());
                                    }
                                }

                                if (openPosition.getProjectName().getProjectDepartment().getDepartamentRuName() != null) {
                                    retStr = strSimpleService.replaceAll(retStr,
                                            emailKeys.get(EmailKeys.DEPARTAMENT.getId()),
                                            openPosition.getProjectName().getProjectDepartment().getDepartamentRuName());
                                }
                            }
                        }
                    }

                    if (user != null) {
                        retStr = strSimpleService.replaceAll(retStr,
                                emailKeys.get(EmailKeys.RESEARCHER_NAME.getId()),
                                user.getName());
                    }
                } catch (NullPointerException e) {
                    log.error("Error", e);
                } finally {
                    return retStr;
                }
            } else {
                return "";
            }
        } else {
            return "";
        }
    }


    @Override
    public String preparingMessage(IteractionList newsItem) {
        HashMap<String, String> emailKeys = generateKeys();

        StringBuffer textBuff = new StringBuffer(newsItem.getIteractionType().getTextEmailToSend());

        String retStr = textBuff.toString();

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            retStr = strSimpleService.replaceAll(retStr, emailKeys.get(EmailKeys.DATE.getId()),
                    simpleDateFormat.format(newsItem.getAddDate()));

            SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("hh:mm");
            simpleDateFormat1.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.TIME.getId()),
                    simpleDateFormat1.format(newsItem.getAddDate()));

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.FIRST_NAME.getId()),
                    newsItem.getCandidate().getFirstName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.MIDDLE_NAME.getId()),
                    newsItem.getCandidate().getMiddleName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.SECOND_NAME.getId()),
                    newsItem.getCandidate().getSecondName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.PROJECT.getId()),
                    newsItem.getVacancy().getProjectName().getProjectName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.COMPANY.getId()),
                    newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName().getComanyName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.DEPARTAMENT.getId()),
                    newsItem.getVacancy().getProjectName().getProjectDepartment().getDepartamentRuName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.PROJECT.getId()),
                    newsItem.getVacancy().getProjectName().getProjectName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.RESEARCHER_NAME.getId()),
                    newsItem.getRecrutier().getName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.JOB_DESCRIPTION.getId()),
                    newsItem.getVacancy().getComment());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.POSITION.getId()),
                    newsItem.getVacancy().getPositionType().getPositionEnName());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.SALARY_MIN.getId()),
                    newsItem.getVacancy().getSalaryMin().toString());

            retStr = strSimpleService.replaceAll(retStr,
                    emailKeys.get(EmailKeys.SALARY_MAX.getId()),
                    newsItem.getVacancy().getSalaryMax().toString());

/*            if (newsItem.getVacancy() != null)
                if (newsItem.getVacancy().getProjectName() != null)
                    if (newsItem.getVacancy().getProjectName().getProjectDepartment() != null)
                        if (newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName() != null)
                            if (newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName().getComanyName() != null)
                                retStr = retStr.replaceAll(emailKeys.get(EmailKeys.COMPANY.getId()), newsItem.getVacancy().getProjectName().getProjectDepartment().getCompanyName().getComanyName()); */
/*            if (newsItem.getVacancy().getProjectName() != null)
                if (newsItem.getVacancy().getProjectName().getProjectName() != null)
                    retStr = retStr.replaceAll(emailKeys.get(EmailKeys.DEPARTAMENT.getId()), newsItem.getVacancy().getProjectName().getProjectName()); */
/*            if (newsItem.getRecrutier() != null)
                if (newsItem.getRecrutier().getName() != null)
                    retStr = retStr.replaceAll(emailKeys.get(EmailKeys.RESEARCHER_NAME.getId()), newsItem.getRecrutier().getName()); */
/*            if (newsItem.getVacancy() != null)
                if (newsItem.getVacancy().getComment() != null)
                    retStr = retStr.replaceAll(emailKeys.get(EmailKeys.JOB_DESCRIPTION.getId()), newsItem.getVacancy().getComment()); */
/*            if (newsItem.getVacancy().getPositionType() != null)
                if (newsItem.getVacancy().getPositionType().getPositionEnName() != null)
                    retStr = retStr.replaceAll(emailKeys.get(EmailKeys.POSITION.getId()),
                            newsItem.getVacancy().getPositionType().getPositionEnName()); */
/*            if (newsItem.getVacancy().getSalaryMin() != null)
                retStr = retStr.replaceAll(emailKeys.get(EmailKeys.SALARY_MIN.getId()),
                        newsItem.getVacancy().getSalaryMin().toString()); */
/*            if (newsItem.getVacancy().getSalaryMax() != null)
                retStr = retStr.replaceAll(emailKeys.get(EmailKeys.SALARY_MAX.getId()),
                        newsItem.getVacancy().getSalaryMax().toString()); */

        } catch (NullPointerException e) {
            log.error("Error", e);
        } finally {
            return retStr;
        }
    }


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