package com.company.itpearls.web.widgets;

import com.company.itpearls.entity.IteractionList;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@UiController("itpearls_MonthlyInterviewCountWidget")
@UiDescriptor("monthly-interview-count-widget.xml")
@DashboardWidget(name = "Mountly Iteraction Count")
public class MonthlyInterviewCountWidget extends ScreenFragment {
    @Inject
    private Label<String> labelAssignExternalInterview;
    @Inject
    private Label<String> labelAssignInternalInterview;
    @Inject
    private Label<String> labelCountNewContacts;
    @Inject
    private Label<String> labelPrepareExternalInterview;
    @Inject
    private Label<String> labelPrepareInternalInterview;
    @Inject
    private Label<String> labelTitle;

    private String itemNewContact = "Новый контакт";
    @Inject
    private CollectionLoader<IteractionList> iteractioListDl;
    @Inject
    private CollectionContainer<IteractionList> iteractionListDc;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        labelTitle.setValue( "<b><u>Взаимодействия за месяц</u></b>" );

        Calendar cal = Calendar.getInstance();
        Date firstDay = cal.getTime();
        cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
        Date endDay = cal.getTime();

        iteractioListDl.setParameter( "startDate", firstDay );
        iteractioListDl.setParameter( "endDate", endDay );
        iteractioListDl.setParameter( "iteractionName", itemNewContact + "%" );

        iteractioListDl.load();

        int countItems = 0;
        countItems = iteractionListDc.getItems().size();

/*        for( IteractionList list : iteractionListDc.getItems() ) {
            countItems ++;
        } */

        labelCountNewContacts.setValue( itemNewContact + " " + countItems );
    }


}