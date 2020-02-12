package com.company.itpearls.web.screens.jobcandidate;

import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentValuesLoader;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;

@UiController("itpearls_JobCandidate.browse")
@UiDescriptor("job-candidate-browse.xml")
@LookupComponent("jobCandidatesTable")
@LoadDataBeforeShow
public class JobCandidateBrowse extends StandardLookup<JobCandidate> {
    @Inject
    private CheckBox checkBoxShowOnlyMy;
    @Inject
    private CollectionLoader<JobCandidate> jobCandidatesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( userSession.getUser().getGroup().getName().equals("Стажер") ) {
            jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%" );

            checkBoxShowOnlyMy.setValue( true );
            checkBoxShowOnlyMy.setEditable( false );
        }
    }

    @Subscribe("checkBoxShowOnlyMy")
    public void onCheckBoxShowOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if(checkBoxShowOnlyMy.getValue()) {
           jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        }
        else {
            jobCandidatesDl.removeParameter("userName");
        }

        jobCandidatesDl.load();
    }

    @Install(to = "jobCandidatesTable", subject = "iconProvider")
    private String jobCandidatesTableIconProvider(JobCandidate jobCandidate) {
        String s = getPictString( jobCandidate );

        if( s != null ) {
            switch ( s ) {
                case "0":
                    return "icons/clear.png";
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                case "9":
                    return "icons/eye-plus.png";
                case "10":
                    return "icons/erase.png;";
            }
        }

        return "icons/cancel.png";
    }

    private String getPictString(JobCandidate jobCandidate) {
        String s = null;
        // загрузить последний тип взаимодействия
//        try {

            s = dataManager.loadValue("select e.iteractionType.number " +
                    "from itpearls_iteractionList e " +
                    "where e.candidate = :jobCandidate " +
                    "and e.numberItercation = " +
                    "select max( f.numberIteraction ) " +
                    "from itpearls_itercationList f " +
                    "where candidate = :candidate", String.class )
                    .parameter("jobCandidate", jobCandidate )
                    .one();
//        } catch ( Exception e ) {
//            s = null;
//        }
        return  s;
    }
}