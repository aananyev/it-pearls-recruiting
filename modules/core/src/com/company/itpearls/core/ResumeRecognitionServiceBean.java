package com.company.itpearls.core;

import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service(ResumeRecognitionService.NAME)
public class ResumeRecognitionServiceBean implements ResumeRecognitionService {

    @Inject
    private DataManager dataManager;
    @Inject
    private ParseCVService parseCVService;
    @Inject
    private Logger log;

    @Override
    public Set<String> scanSocialNetworksFromCVs(CandidateCV candidateCV) {
        Set<String> newSocial = new HashSet<>();

        try {
            List<String> urls = parseCVService
                    .extractUrls(Jsoup.parse(candidateCV.getTextCV())
                            .text());
            Set<String> setUrls = new HashSet<>(urls);

            urls.clear();
            newSocial.addAll(setUrls);
        } catch (NullPointerException e) {
            log.error("Error", e);
        }

        return newSocial;
    }


    @Override
    public Set<String> scanSocialNetworksFromCVs(String candidateCV) {
        Set<String> newSocial = new HashSet<>();

        try {
            List<String> urls = parseCVService
                    .extractUrls(Jsoup.parse(candidateCV)
                            .text());
            Set<String> setUrls = new HashSet<>(urls);

            urls.clear();
            newSocial.addAll(setUrls);
        } catch (NullPointerException e) {
            log.error("Error", e);
        }

        return newSocial;
    }

    @Override
    public String parseFirstName(String cvText) {
        String QUERY_NAMES_LIST = "select distinct e.firstName from itpearls_Candidates";

        List<String> firstNames = dataManager.loadValue(QUERY_NAMES_LIST, String.class)
                .list();

        for (String name : firstNames) {
            if(cvText.contains(name)) {
                return name;
            }
        }

        return null;
    }

    @Override
    public String parseSecondName(String cvText) {
        String QUERY_NAMES_LIST = "select distinct e.secondName from itpearls_Candidates";

        List<String> secondNames = dataManager.loadValue(QUERY_NAMES_LIST, String.class)
                .list();

        for (String name : secondNames) {
            if(cvText.contains(name)) {
                return name;
            }
        }

        return null;
    }

    @Override
    public String parseMiddleName(String cvText) {
        String QUERY_NAMES_LIST = "select distinct e.middleame from itpearls_Candidates";

        List<String> middleNames = dataManager.loadValue(QUERY_NAMES_LIST, String.class)
                .list();

        for (String name : middleNames) {
            if(cvText.contains(name)) {
                return name;
            }
        }

        return null;
    }

    @Override
    public String setTemplateLetter(OpenPosition openPosition) {
        StringBuilder retSb = new StringBuilder();

        if (openPosition != null) {
            if (openPosition.getProjectName() != null) {
                if (openPosition.getProjectName().getProjectDepartment() != null) {
                    if (openPosition
                            .getProjectName()
                            .getProjectDepartment()
                            .getTemplateLetter() != null) {
                        retSb.append(openPosition.getProjectName().getProjectDepartment().getTemplateLetter())
                                .append("\n<br>");
                    }
                }
            }

            if (openPosition.getProjectName().getTemplateLetter() != null) {
                retSb.append(openPosition.getProjectName().getTemplateLetter())
                        .append("\n<br>");
            }

            if (openPosition.getTemplateLetter() != null) {
                retSb.append(openPosition.getTemplateLetter())
                        .append("\n<br>");
            }
        }


        return retSb.toString();
    }
}