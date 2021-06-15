package com.company.itpearls.web.widgets.reports;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.Fragments;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.global.UserSession;
import com.ibm.icu.text.Transliterator;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@UiController("itpearls_MyJobCandidatesWidget")
@UiDescriptor("my-job-candidates-widget.xml")
@DashboardWidget(name = "Last Status") //+
public class MyJobCandidatesWidget extends ScreenFragment {

    @WidgetParam
    @WindowParam
    protected Date startDate;

    @WidgetParam
    @WindowParam
    protected Date endDate;

    @Inject
    private CollectionContainer<Iteraction> iteractionCheckDc;
    @Inject
    private Accordion jobCandidatesIteractionAccordion;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    protected ComponentsFactory componentsFactory;

    static String QUERY_ITERACTIONS =
            "select e " +
            "from itpearls_Iteraction e " +
            "where e.widgetChackJobCandidates = true " +
            "order by e.iterationName";
    @Inject
    private Label<String> widgetTitle;
    @Inject
    private CollectionLoader<JobCandidate> listOfCandidatesDl;
    @Inject
    private UserSession userSession;
    @Inject
    private CollectionContainer<JobCandidate> listOfCandidatesDc;
    @Inject
    private Fragments fragments;

    @Subscribe
    public void onInit(InitEvent event) {
        setDefaultDate();
        setAccordionTabs(event);
        setWidgetTitle();
        myCandidate();
    }

    private void setDefaultDate() {
        GregorianCalendar calendar = new GregorianCalendar();

        if(endDate == null) {
            calendar.setTime(new Date());

            endDate = calendar.getTime();
        }

        if (startDate == null) {
            calendar.add(GregorianCalendar.MONTH, -1);

            startDate = calendar.getTime();

        }
    }

    private void myCandidate() {
        listOfCandidatesDl.removeParameter("user");
//        listOfCandidatesDl.setParameter("user", userSession.getUser());
        listOfCandidatesDl.load();
    }

    private void setWidgetTitle() {
        String title = "Статус кандидатов: ";
        String title_wo_date = "Статус кандидатов: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        if (startDate == null || endDate == null) {
            widgetTitle.setValue(title_wo_date);
        } else {
            widgetTitle.setValue(title + df.format(startDate) + " - " + df.format(endDate));
        }
    }

    private void setAccordionTabs(InitEvent event) {
        List<Iteraction> iteractions = dataManager.load(Iteraction.class)
                .query(QUERY_ITERACTIONS)
                .view("iteraction-view")
                .list();
        String CYRILLIC_TO_LATIN = "Russian-Latin/BGN";
        Transliterator toLatinTrans = Transliterator.getInstance(CYRILLIC_TO_LATIN);

        for (Iteraction itr : iteractions) {
//            GroupBoxLayout groupBoxLayout = componentsFactory.createComponent(GroupBoxLayout.class);
            VBoxLayout groupBoxLayout = uiComponents.create(VBoxLayout.NAME);
            groupBoxLayout.setCaption("Список");

            MyCandidateTableFragment myCandidateTableFragment = fragments.create(this,
                    MyCandidateTableFragment.class);
            myCandidateTableFragment.setUser();
            myCandidateTableFragment.setStartDate(startDate);
            myCandidateTableFragment.setEndDate(endDate);

            myCandidateTableFragment.load();

            Fragment fragment = myCandidateTableFragment.getFragment();
            groupBoxLayout.add(fragment);

            Accordion.Tab tab = jobCandidatesIteractionAccordion.addTab(
                    toLatinTrans.transliterate(itr.getIterationName()),
                    groupBoxLayout);

            tab.setCaption(itr.getIterationName());
            tab.setStyleName("last_status_widget");
        }
    }
}