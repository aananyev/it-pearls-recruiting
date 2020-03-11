package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentValuesLoader;
import com.haulmont.cuba.gui.Dialogs;
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
    private CheckBox checkBoxOnWork;
    @Inject
    private Dialogs dialogs;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if( userSession.getUser().getGroup().getName().equals("Стажер") ) {
            jobCandidatesDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%" );

            checkBoxShowOnlyMy.setValue( true );
            checkBoxShowOnlyMy.setEditable( false );
        }

        checkBoxOnWork.setValue( false );
        jobCandidatesDl.removeParameter( "param1" );
        jobCandidatesDl.removeParameter( "param3" );

        jobCandidatesDl.load();
    }

    @Subscribe("checkBoxOnWork")
    public void onCheckBoxOnWorkValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if( !checkBoxOnWork.getValue() ) {
            jobCandidatesDl.removeParameter( "param1" );
            jobCandidatesDl.removeParameter( "param3" );
        } else {
            jobCandidatesDl.setParameter( "param1", null );
            jobCandidatesDl.setParameter( "param3", 10 );
        }

        jobCandidatesDl.load();
        
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
        Integer s = getPictString( jobCandidate );

        if( s != null ) {
            switch ( s ) {
                case 0:
                    return "icons/clear.png";
                case 1:
                    return "icons/researching.png";
                case 2:
                    return "icons/recruiting.png";
                case 3:
                    return "icons/to-client.png";
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    return "icons/eye-plus.png";
                case 10:
                    return "icons/case-closed.png";
                default:
                    return "";
            }
        }

        return "icons/question-white.png";
    }

    private Integer getPictString(JobCandidate jobCandidate) {
        return jobCandidate.getStatus();
    }

    public void onButtonSubscribeClick() {
        dialogs.createMessageDialog()
                .withCaption("Information")
                .withMessage("Подписка на действия с кандидатом будет реализована позднее")
                .show();
    }
}