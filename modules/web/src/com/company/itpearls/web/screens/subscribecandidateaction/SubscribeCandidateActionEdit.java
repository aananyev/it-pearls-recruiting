package com.company.itpearls.web.screens.subscribecandidateaction;

import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SubscribeCandidateAction;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.text.*;

@UiController("itpearls_SubscribeCandidateAction.edit")
@UiDescriptor("subscribe-candidate-action-edit.xml")
@EditedEntityContainer("subscribeCandidateActionDc")
@LoadDataBeforeShow
public class SubscribeCandidateActionEdit extends StandardEditor<SubscribeCandidateAction> {
    @Inject
    private DateField<Date> startDateField;
    @Inject
    private LookupPickerField<User> subscriberField;
    @Inject
    private UserSession userSession;
    @Inject
    private Dialogs dialogs;
    @Inject
    private LookupPickerField<JobCandidate> candidateField;
    @Inject
    private DateField<Date> endDateField;
    @Inject
    private DataManager dataManager;
    @Inject
    private RadioButtonGroup radioButtonGroupDates;
    @Inject
    private Notifications notifications;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
       startDateField.setValue( new Date() );
       subscriberField.setValue( userSession.getUser() );
    }

    private static DateFormatSymbols ruDateFormatSymbols = new DateFormatSymbols(){

        @Override
        public String[] getMonths() {
            return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        }

    };

    public void onOkButtonClick() {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "dd MMMM YYYY",
                ruDateFormatSymbols );

        // проверить, а вдруг вы уже подписаны на эту вакансию?
        if( checkSubscribePosition() ) {
            dialogs.createMessageDialog()
                    .withCaption( "Внимание" )
                    .withModal( true )
                    .withMessage( "Вы уже подписаны на кандидата " + candidateField.getValue().getFullName() +
                            "\n c " + dateFormat.format( startDateField.getValue() ) +
                            " по " + dateFormat.format( endDateField.getValue() ) )
                    .show();

//            closeWithDiscard();

            close(WINDOW_DISCARD_AND_CLOSE_ACTION);
        } else {
            notifications.create().withDescription( "Подписка" )
                    .withCaption( "Вы подписались на действия с кандидатом " + candidateField.getValue().getFullName() )
                    .show();
            
            closeWithCommit();
        }
//            close(WINDOW_COMMIT_AND_CLOSE_ACTION);
    }

    private Boolean checkSubscribePosition() {
        Integer countSubscrine = dataManager
                .loadValue( "select count(e.subscriber) from itpearls_SubscribeCandidateAction e " +
                        "where e.subscriber = :subscriber and " +
                        "e.candidate = :candidate and " +
                        ":nowDate between e.startDate and e.endDate", Integer.class )
                .parameter( "subscriber", getEditedEntity().getSubscriber() )
                .parameter( "candidate", getEditedEntity().getCandidate() )
                .parameter( "nowDate", new Date() )
                .one();

        // если нет соответствия, значит нет еще подписки, значит можно подписаться,
        // если уже есть, то не надо подписываться
        return countSubscrine > 0 ? true : false;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        Map<String, Integer> map = new LinkedHashMap<>();
        
        map.put("Неделя", 1);
        map.put("Месяц", 2);
        map.put("Три месяца", 3);
        map.put("Полугодие", 4);
        map.put("Год", 5);
        map.put("Произвольный интервал", 0);
        
        radioButtonGroupDates.setOptionsMap(map);
    }

    @Subscribe("radioButtonGroupDates")
    public void onRadioButtonGroupDatesValueChange(HasValue.ValueChangeEvent event) {
        Date    curDate = new Date();
        Calendar    calDate = Calendar.getInstance();

        startDateField.setEnabled( false );
        endDateField.setEnabled( false );

        switch ( radioButtonGroupDates.getValue().hashCode() ) {
            case 1:
                calDate.add( Calendar.DAY_OF_WEEK, 7 );

                break;
            case 2:
                calDate.add( Calendar.DAY_OF_MONTH, 30 );

                break;
            case 3:
                calDate.add( Calendar.DAY_OF_MONTH, 90 );

                break;
            case 4:
                calDate.add( Calendar.DAY_OF_MONTH, 180 );

                break;
            case 5:
                calDate.add( Calendar.DAY_OF_YEAR, 365 );

                break;
            case 0:
                endDateField.setEnabled( true );
                break;
        }

        endDateField.setValue( calDate.getTime() );
    }
}