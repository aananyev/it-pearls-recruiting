package com.company.hunttech.web.screens.jobhistory;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.JobHistory;
import com.company.hunttech.entity.Position;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.screen.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@UiController("hunttech_JobHistory.edit")
@UiDescriptor("job-history-edit.xml")
@EditedEntityContainer("jobHistoryDc")
@LoadDataBeforeShow
public class JobHistoryEdit extends StandardEditor<JobHistory> {

    private static final Logger log = LoggerFactory.getLogger(JobHistoryEdit.class);

    @Inject
    private SuggestionPickerField<JobCandidate> candidateField;

    @Subscribe
    public void onInit(InitEvent event) {
        logMemState("onInit");
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        JobHistory item = getEditedEntity();
        JobCandidate candidate = item.getCandidate();
        Company company = item.getCurrentCompany();
        Position position = item.getCurrentPosition();

        log.info("JobHistoryEdit onBeforeShow: entityId={}, candidate={}, company='{}' (raw='{}'), position='{}' (raw='{}')",
                item.getId(),
                candidate != null ? candidate.getFullName() + " [" + candidate.getId() + "]" : "none",
                company != null ? company.getComanyName() : "none",
                item.getRawCompanyName(),
                position != null ? position.getPositionRuName() : "none",
                item.getRawPositionName());
        logMemState("onBeforeShow");

        if (candidate != null) {
            candidateField.setEditable(false);
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        logMemState("onAfterShow");
        log.info("JobHistoryEdit успешно отрендерен: entityId={}", getEditedEntity().getId());
    }

    private void logMemState(String phase) {
        Runtime rt = Runtime.getRuntime();
        long freeMb = rt.freeMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long usedMb = totalMb - freeMb;
        log.info("JobHistoryEdit memory state [{}]: used={}MB, free={}MB, total={}MB, max={}MB",
                phase, usedMb, freeMb, totalMb, maxMb);
    }
}