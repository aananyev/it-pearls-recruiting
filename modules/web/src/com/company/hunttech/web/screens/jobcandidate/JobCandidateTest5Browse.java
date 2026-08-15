package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_JobCandidateTest5.browse")
@UiDescriptor("job-candidate-test5-browse.xml")
@LookupComponent("candidatesDataGrid")
@LoadDataBeforeShow
public class JobCandidateTest5Browse extends StandardLookup<JobCandidate> {

    @Inject
    private DataGrid<JobCandidate> candidatesDataGrid;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(Screen.InitEvent event) {
        // Устанавливаем генератор раскрывающихся деталей строки (Row Details)
        candidatesDataGrid.setItemClickAction(candidatesDataGrid.getAction("edit"));
        candidatesDataGrid.setDetailsGenerator(candidate -> {
            VBoxLayout detailsBox = uiComponents.create(VBoxLayout.class);
            detailsBox.setStyleName("edit-card");
            detailsBox.setWidthFull();
            detailsBox.setSpacing(true);

            Label<String> title = uiComponents.create(Label.TYPE_STRING);
            title.setValue("Детальные контакты и история кандидата: " + (candidate.getFullName() != null ? candidate.getFullName() : ""));
            title.setStyleName("h3");

            HBoxLayout contacts = uiComponents.create(HBoxLayout.class);
            contacts.setSpacing(true);

            Label<String> phoneLbl = uiComponents.create(Label.TYPE_STRING);
            phoneLbl.setValue("Телефон: " + (candidate.getPhone() != null ? candidate.getPhone() : "-"));

            Label<String> emailLbl = uiComponents.create(Label.TYPE_STRING);
            emailLbl.setValue("Email: " + (candidate.getEmail() != null ? candidate.getEmail() : "-"));

            Label<String> tgLbl = uiComponents.create(Label.TYPE_STRING);
            tgLbl.setValue("Telegram: " + (candidate.getTelegramName() != null ? candidate.getTelegramName() : "-"));

            contacts.add(phoneLbl);
            contacts.add(emailLbl);
            contacts.add(tgLbl);

            Label<String> interactionsLbl = uiComponents.create(Label.TYPE_STRING);
            int count = candidate.getIteractionList() != null ? candidate.getIteractionList().size() : 0;
            interactionsLbl.setValue("Зарегистрировано взаимодействий по вакансиям: " + count);

            detailsBox.add(title);
            detailsBox.add(contacts);
            detailsBox.add(interactionsLbl);

            return detailsBox;
        });
    }
}
