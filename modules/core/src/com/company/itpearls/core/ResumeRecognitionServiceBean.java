package com.company.itpearls.core;

import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(ResumeRecognitionService.NAME)
public class ResumeRecognitionServiceBean implements ResumeRecognitionService {

    @Inject
    private DataManager dataManager;

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
}