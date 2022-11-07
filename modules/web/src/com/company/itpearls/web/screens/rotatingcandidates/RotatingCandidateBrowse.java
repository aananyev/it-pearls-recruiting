package com.company.itpearls.web.screens.rotatingcandidates;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_RotatingCandidate.browse")
@UiDescriptor("rotating-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class RotatingCandidateBrowse extends StandardLookup<JobCandidate> {
    @Inject
    private RadioButtonGroup<Integer> recruterRadioButtonsGroup;

    private Map<String, Integer> recruterRadioButtonsGroupMap = new LinkedHashMap<>();
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private UserSession userSession;

    @Subscribe
    public void onInit(InitEvent event) {
        recruterRadioButtonsGroupMap.put("Все", 0);
        recruterRadioButtonsGroupMap.put("Только мои", 1);
        recruterRadioButtonsGroup.setOptionsMap(recruterRadioButtonsGroupMap);
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        recruterRadioButtonsGroup.setValue(1);
    }

    @Install(to = "jobCandidatesTable.candidate", subject = "columnGenerator")
    private String jobCandidatesTableCandidateColumnGenerator(DataGrid.ColumnGeneratorEvent<OpenPosition> event) {
        return null;
    }

    @Subscribe("recruterRadioButtonsGroup")
    public void onRecruterRadioButtonsGroupValueChange(HasValue.ValueChangeEvent<Integer> event) {
        switch (event.getValue()) {
            case 0:
                jobCandidatesDl.removeParameter("recrutier");
                break;
            case 1:
                jobCandidatesDl.setParameter("recrutier", userSession.getUser());
                break;
            default:
                break;
        }

        jobCandidatesDl.load();
    }


}