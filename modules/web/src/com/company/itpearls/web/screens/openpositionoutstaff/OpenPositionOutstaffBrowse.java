package com.company.itpearls.web.screens.openpositionoutstaff;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.entity.Grade;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.StaffCurrent;
import com.company.itpearls.entity.StaffingTable;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TreeDataGrid;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.openposition.OpenPositionBrowse;

import javax.inject.Inject;
import java.util.List;

@UiController("itpearls_OpenPositionOutstaff.browse")
@UiDescriptor("open-position-outstaff-browse.xml")
public class OpenPositionOutstaffBrowse extends OpenPositionBrowse {
    private static final String QUERY_GET_DEFAULT_GRADE = "select e from itpearls_Grade e where e.gradeName like 'Default'";

    @Inject
    private Metadata metadata;
    @Inject
    private TreeDataGrid<OpenPosition> openPositionsTable;
    @Inject
    private DataManager dataManager;
    @Inject
    private Button addToStaffTableButton;
    @Inject
    private Notifications notifications;
    @Inject
    private Events events;

    @Subscribe
    public void onBeforeShow1(BeforeShowEvent event) {
        addAddToStaffTableButtonEnable();
    }

    private void addAddToStaffTableButtonEnable() {
        openPositionsTable.addSelectionListener(e -> {
                addToStaffTableButton.setEnabled(openPositionsTable.getSingleSelected() != null);
        });
    }

    public void attToStaffTableButtonInvoke() {
        if (openPositionsTable.getSingleSelected() != null) {
            if (isNotDublicateOpenPosition(openPositionsTable.getSingleSelected())) {
                StaffingTable staffingTable = metadata.create(StaffingTable.class);

                staffingTable.setActive(true);
                staffingTable.setNumberOfStaff(openPositionsTable.getSingleSelected().getNumberPosition());
                staffingTable.setSalaryMin(openPositionsTable.getSingleSelected().getSalaryMin());
                staffingTable.setSalaryMax(openPositionsTable.getSingleSelected().getSalaryMax());
                staffingTable.setOpenPosition(openPositionsTable.getSingleSelected());
                staffingTable.setGrade(openPositionsTable.getSingleSelected().getGrade() != null
                        ? openPositionsTable.getSingleSelected().getGrade()
                        : dataManager.load(Grade.class)
                        .query(QUERY_GET_DEFAULT_GRADE)
                        .view("grade-view")
                        .one());

                CommitContext commitContext = new CommitContext();
                commitContext.addInstanceToCommit(staffingTable);
                dataManager.commit(commitContext);

                String message = "Вакансия "
                        + openPositionsTable.getSingleSelected().getVacansyName()
                        + " добавлена в штатное расписание";

                notifications.create(Notifications.NotificationType.TRAY)
                        .withDescription(message)
                        .withPosition(Notifications.Position.TOP_RIGHT)
                        .show();

                events.publish(new UiNotificationEvent(this, message));
            } else {
                notifications.create(Notifications.NotificationType.ERROR)
                        .withDescription("Вакансия уже добавлена в штатное расписание ранее")
                        .withType(Notifications.NotificationType.ERROR)
                        .show();
            }
        }
    }

    private boolean isNotDublicateOpenPosition(OpenPosition singleSelected) {
        List<StaffCurrent> staffCurrents = dataManager.load(StaffCurrent.class)
                .list();

        for (StaffCurrent staffCurrent : staffCurrents) {
            if (staffCurrent.equals(singleSelected)) {
                return false;
            }
        }

        return true;
    }
}