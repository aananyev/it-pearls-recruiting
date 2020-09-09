package com.company.itpearls.web.screens.jobcandidate;

import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.List;

@UiController("itpearls_JobCanidateDetailScreenFragment")
@UiDescriptor("job-canidate-detail-screen-fragment.xml")
public class JobCanidateDetailScreenFragment extends ScreenFragment {
    @Inject
    private Label<String> lastRecruterLabel;
    @Inject
    private DataManager dataManager;
    @Inject
    private CollectionContainer<JobCandidate> jobCandidatesDc;
    @Inject
    private Label<String> salaryExpectationLabel;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        salaryExpectationLabel.setValue(getStatistics("Зарплатные ожидания").getAddString());
    }

    private IteractionList getStatistics(String iteractionName) {

        String QUERY_LAST_SALARY = "select e from itpearls_IteractionList e where e.iteractionType = " +
                "(select f from itpearls_Iteraction f where f.iterationName like :iteractionName) and " +
                "e.candidate = :candidate";

        IteractionList iteractionList = null;

        try {
            iteractionList = dataManager.load(IteractionList.class)
                    .query(QUERY_LAST_SALARY)
                    .view("iteractionList-view")
                    .parameter("iteractionName", iteractionName)
                    .parameter("candidate", jobCandidatesDc.getItem())
                    .one();
        } catch (Exception e) {}

        return iteractionList;
    }
}