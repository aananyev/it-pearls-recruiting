package com.company.itpearls.web;

import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.web.App;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.gui.ReportGuiManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component(MemoForCandidatesBean.NAME)
public class MemoForCandidatesBean {
    public static final String NAME = "itpearls_MemoForCandidatesBean";

    @Inject
    private DataManager dataManager;
    @Inject
    private ReportGuiManager reportGuiManager;

    public void runReport(){
        LoadContext<Report> loadContext = LoadContext.create(Report.class)
                .setQuery(LoadContext
                        .createQuery("select p from report$Report p where p.code = 'memoForCandidates'"))
                .setView("report.edit");
        Report report = dataManager.load(loadContext);
        FrameOwner window = App.getInstance().getTopLevelWindow().getFrameOwner();
        reportGuiManager.runReport(report, window);
    }
}