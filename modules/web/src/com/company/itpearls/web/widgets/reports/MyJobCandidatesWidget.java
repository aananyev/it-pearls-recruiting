package com.company.itpearls.web.widgets.reports;

import com.company.itpearls.entity.Iteraction;
import com.haulmont.addon.dashboard.web.annotation.DashboardWidget;
import com.haulmont.addon.dashboard.web.annotation.WidgetParam;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.ibm.icu.text.Transliterator;
import com.sun.org.apache.bcel.internal.generic.TABLESWITCH;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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

    static String QUERY_ITERACTIONS = "select e from itpearls_Iteraction e where e.widgetChackJobCandidates = true";
    @Inject
    private Label<String> widgetTitle;

    @Subscribe
    public void onInit(InitEvent event) {
        setAccordionTabs(event);
        setWidgetTitle();
    }

    private void setWidgetTitle() {
        String title = "Статус кандидатов: ";
        String title_wo_date = "Статус кандидатов: ";
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        if(startDate == null || endDate == null) {
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



        for(Iteraction itr : iteractions) {

            GroupBoxLayout groupBoxLayout = componentsFactory.createComponent(GroupBoxLayout.class);

            Accordion.Tab tab = jobCandidatesIteractionAccordion.addTab(
                    toLatinTrans.transliterate(itr.getIterationName()),
                    groupBoxLayout);

            tab.setCaption(itr.getIterationName());
        }
    }
}