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

            Label<String> title = uiComponents.create(Label.TYPE_HTML);
            String name = candidate.getFullName() != null ? candidate.getFullName() : "Без имени";
            String pos = candidate.getPersonPosition() != null ? candidate.getPersonPosition().getPositionRuName() : "Специалист";
            title.setValue("<div style='font-size: 15px; font-weight: 700; color: #2c3e50; margin-bottom: 4px;'>" +
                    "👤 " + name + " <span style='font-size: 12px; color: #7f8c8d; font-weight: normal;'>(" + pos + ")</span>" +
                    "</div>");

            HBoxLayout mainHBox = uiComponents.create(HBoxLayout.class);
            mainHBox.setWidthFull();
            mainHBox.setSpacing(true);

            // Левый блок: Контакты и Навыки
            VBoxLayout leftBox = uiComponents.create(VBoxLayout.class);
            leftBox.setSpacing(true);
            leftBox.setWidthFull();

            Label<String> contactsLbl = uiComponents.create(Label.TYPE_HTML);
            contactsLbl.setValue("<div style='font-size: 12px; line-height: 1.6;'>" +
                    "<b>📞 Телефон:</b> " + (candidate.getPhone() != null ? candidate.getPhone() : "-") + "<br/>" +
                    "<b>✉️ Email:</b> " + (candidate.getEmail() != null ? candidate.getEmail() : "-") + "<br/>" +
                    "<b>✈️ Telegram:</b> " + (candidate.getTelegramName() != null ? candidate.getTelegramName() : "-") +
                    "</div>");

            Label<String> skillsLbl = uiComponents.create(Label.TYPE_HTML);
            skillsLbl.setValue("<div style='margin-top: 4px;'>" +
                    "<b style='font-size: 11px; color: #95a5a6;'>КЛЮЧЕВЫЕ НАВЫКИ:</b><br/>" +
                    "<div style='display: flex; gap: 4px; margin-top: 2px;'>" +
                    "<span style='background: #e8f4f8; color: #2980b9; padding: 2px 6px; border-radius: 3px; font-size: 11px;'>Java 17</span>" +
                    "<span style='background: #e8f4f8; color: #2980b9; padding: 2px 6px; border-radius: 3px; font-size: 11px;'>CUBA / Jmix</span>" +
                    "<span style='background: #e8f4f8; color: #2980b9; padding: 2px 6px; border-radius: 3px; font-size: 11px;'>PostgreSQL</span>" +
                    "</div></div>");

            leftBox.add(contactsLbl);
            leftBox.add(skillsLbl);

            // Правый блок: Статистика и кнопки
            VBoxLayout rightBox = uiComponents.create(VBoxLayout.class);
            rightBox.setSpacing(true);
            rightBox.setWidthFull();

            Label<String> statsLbl = uiComponents.create(Label.TYPE_HTML);
            int count = candidate.getIteractionList() != null ? candidate.getIteractionList().size() : 0;
            rightBox.add(statsLbl);
            statsLbl.setValue("<div style='background: #f8f9fa; padding: 8px 12px; border-radius: 6px; border-left: 3px solid #3498db; font-size: 12px;'>" +
                    "<b>📊 История активности:</b><br/>" +
                    "Зарегистрировано взаимодействий: <b>" + count + "</b><br/>" +
                    "Текущий статус: <span style='color: #27ae60; font-weight: 600;'>Рассматривается</span>" +
                    "</div>");

            mainHBox.add(leftBox);
            mainHBox.add(rightBox);

            detailsBox.add(title);
            detailsBox.add(mainHBox);

            return detailsBox;
        });
    }
}
