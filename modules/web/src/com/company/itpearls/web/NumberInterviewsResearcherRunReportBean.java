package com.company.itpearls.web;

import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.web.App;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.gui.ReportGuiManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component(NumberInterviewsResearcherRunReportBean.NAME)
public class NumberInterviewsResearcherRunReportBean {
    public static final String NAME = "itpearls_NumberInterviewsResearcherRunReportBean";

    @Inject
    private ReportGuiManager reportGuiManager;

    @Inject
    private DataManager dataManager;

    public void runReport(){
        LoadContext<Report> loadContext = LoadContext.create(Report.class)
                .setQuery(LoadContext.createQuery("select p from report$Report p where p.code = 'numberInterviewsResearcher'"))
                .setView("report.edit");

        Report report = dataManager.load(loadContext);

//        Frame window  = App.getInstance().getTopLevelWindow();
        FrameOwner window = App.getInstance().getTopLevelWindow().getFrameOwner();
        reportGuiManager.runReport(report, window);
    }
}