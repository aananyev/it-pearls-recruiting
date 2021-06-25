package com.company.itpearls.web.widgets.reports;

import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.JobCandidate;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_MyCandidateTableFragment")
@UiDescriptor("my-candidate-table-fragment.xml")
public class MyCandidateTableFragment extends ScreenFragment {
    @Inject
    private CollectionContainer<JobCandidate> listOfCandidatesDc;
    @Inject
    private CollectionLoader<JobCandidate> listOfCandidatesDl;
    @Inject
    private UserSession userSession;

    private Date startDate = null;
    private Date endDate =  null;
    private Iteraction iteractionType = null;
    private User user = null;
    @Inject
    private Table<IteractionList> jobCandidatesTable;

    public void setUser(User user) {
        this.user = user;
    }

    public void setUser() {
        user = userSession.getUser();
    }

    public void setStartDate(Date date) {
        this.startDate = date;
    }

    public void setEndDate(Date date) {
        this.endDate = date;
    }

    public void setIteractionType(Iteraction iteraction) {
        this.iteractionType = iteraction;
    }

    public void load() {
        listOfCandidatesDl.setParameter("userName", user);
        listOfCandidatesDl.setParameter("startDate", startDate);
        listOfCandidatesDl.setParameter("endDate", endDate);
        listOfCandidatesDl.setParameter("iteractionType", iteractionType);

        listOfCandidatesDl.load();
    }

    public int getCountCandidates() {
        return listOfCandidatesDc.getItems().size();
    }
}