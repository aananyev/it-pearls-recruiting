package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.SubscribeCandidateAction;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentValuesLoader;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

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
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private GroupTable<JobCandidate> jobCandidatesTable;

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
                    break;
                case 1:
                    return "icons/resume-red.png";
                case 2:
                    return "icons/resume-yellow.png";
                case 3:
                    return "icons/resume-green.png";
                case 4:
                    return "icons/to-client.png";
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                    return "icons/case-closed.png";
                default:
                    return "";
            }
        }

        return "icons/question-white.png";
    }
/*
    private Integer getPictString(JobCandidate jobCandidate) {
        return jobCandidate.getStatus();
    }
*/

    private Integer getPictString(JobCandidate jobCandidate ) {
        // если только имя и отчество - красный сигнал светофора
        // если имя, день рождения и один из контактов - желтый
        // если больше двух контактов - зеленый, если нет др - все равно желтый
        if( ( jobCandidate.getEmail() != null && jobCandidate.getPhone() != null && jobCandidate.getSkypeName() != null ) ||
                ( jobCandidate.getEmail() != null && jobCandidate.getPhone() != null ) ||
                ( jobCandidate.getEmail() != null && jobCandidate.getSkypeName() != null ) ||
                ( jobCandidate.getPhone() != null && jobCandidate.getSkypeName() != null ) &&
        jobCandidate.getBirdhDate() != null )
            return 3;
        else
            if( jobCandidate.getBirdhDate() != null &&
                    ( jobCandidate.getPhone() != null ||
                            jobCandidate.getEmail() != null ||
                            jobCandidate.getSkypeName() != null ) )
                return 2;
            else
                return 1;

    }

    public void onButtonSubscribeClick() {
        screenBuilders.editor( SubscribeCandidateAction.class, this)
                .newEntity()
                .withInitializer( e -> {
                    e.setCandidate( jobCandidatesTable.getSingleSelected() );
                    e.setSubscriber( userSession.getUser() );
                    e.setStartDate( new Date() );
                })
                .withOpenMode( OpenMode.DIALOG )
                .build()
                .show();
    }
}