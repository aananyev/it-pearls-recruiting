package com.company.hunttech.web.screens.corporateyandexcalendar;

import com.company.hunttech.entity.CorporateYandexCalendar;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.List;

@UiController("hunttech_CorporateYandexCalendar.edit")
@UiDescriptor("corporate-yandex-calendar-edit.xml")
@EditedEntityContainer("corporateCalendarDc")
@LoadDataBeforeShow
public class CorporateYandexCalendarEdit extends StandardEditor<CorporateYandexCalendar> {

    @Inject
    private Label<String> sidebarStatusVal;
    @Inject
    private Label<String> sidebarDefaultVal;
    @Inject
    private CheckBox activeField;
    @Inject
    private CheckBox isDefaultField;
    @Inject
    private Messages messages;
    @Inject
    private DataManager dataManager;

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        if (Boolean.TRUE.equals(getEditedEntity().getIsDefault())) {
            List<CorporateYandexCalendar> otherDefaults = dataManager.load(CorporateYandexCalendar.class)
                    .query("select e from hunttech_CorporateYandexCalendar e where e.isDefault = true and e.id <> :id")
                    .parameter("id", getEditedEntity().getId())
                    .list();
            for (CorporateYandexCalendar other : otherDefaults) {
                other.setIsDefault(false);
                event.getDataContext().merge(other);
            }
        }
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        updateSidebarSummary();
    }

    @Subscribe("activeField")
    public void onActiveFieldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        updateSidebarSummary();
    }

    @Subscribe("isDefaultField")
    public void onIsDefaultFieldValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        updateSidebarSummary();
    }

    private void updateSidebarSummary() {
        if (sidebarStatusVal != null) {
            boolean active = Boolean.TRUE.equals(activeField.getValue());
            sidebarStatusVal.setValue(active ? messages.getMessage(getClass(), "msgActive") : messages.getMessage(getClass(), "msgInactive"));
        }
        if (sidebarDefaultVal != null) {
            boolean isDefault = Boolean.TRUE.equals(isDefaultField.getValue());
            sidebarDefaultVal.setValue(isDefault ? messages.getMessage(getClass(), "msgYes") : messages.getMessage(getClass(), "msgNo"));
        }
    }
}
