package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.ParseCVService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.vaadin.ui.JavaScript;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

@UiController("itpearls_QuickViewOpenPositionDescription")
@UiDescriptor("quick-view-open-position-description.xml")
public class QuickViewOpenPositionDescription extends Screen {
    String jobDescription = "";
    String projectDescription = "";
    String companyWorkConditions = "";

    @Inject
    private RichTextArea jobDesxriptionRichTextArea;
    @Inject
    private RichTextArea projectDescriptionRichTextArea;
    @Inject
    private Notifications notifications;
    @Inject
    private TabSheet jobDescriptionViewTab;
    @Inject
    private CheckBox copyAllToClipboardCheckBox;
    @Inject
    private RichTextArea companyWorkingConditionsRichTextArea;
    @Inject
    private ParseCVService parseCVService;

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public void setCompanyWorkConditions(String companyCondidions) {
        this.companyWorkConditions = companyCondidions;
    }

    public String getCompanyWorkConditions() {
        return companyWorkConditions;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void reloadDescriptions() {
        if (jobDescription != null && !jobDescription.equals(""))
            jobDesxriptionRichTextArea.setValue(jobDescription);

        if (projectDescription != null && !projectDescription.equals(""))
            projectDescriptionRichTextArea.setValue(projectDescription);

        if (companyWorkConditions != null && !companyWorkConditions.equals(""))
            companyWorkingConditionsRichTextArea.setValue(companyWorkConditions);
    }

    public void copyToClipboard() {
        String strToCopy = "";
        String breakStr = "<br>";
        String breakStr3 = "<br><br><br>";
        String textVacansyDescription = "ОПИСАНИЕ ВАКАНСИИ";
        String textProjectDescription = "ОПИСАНИЕ ПРОЕКТА";
        String textCompanyWorkingConditions = "ОПИСАНИЕ ПРЕИМУЩЕСТВ РАБОТЫ В КОМПАНИИ";

        if (copyAllToClipboardCheckBox.getValue()) {
            strToCopy = parseCVService.br2nl((textVacansyDescription + breakStr
                    + jobDesxriptionRichTextArea.getValue()
                    + breakStr3
                    + textProjectDescription + breakStr
                    + projectDescriptionRichTextArea.getValue()
                    + breakStr3
                    + textCompanyWorkingConditions + breakStr
                    + companyWorkingConditionsRichTextArea.getValue())).replaceAll("\\<.*?\\>", "");

        } else {
            switch (jobDescriptionViewTab.getSelectedTab().getName()) {
                case "jobDescriptionTab":
                    strToCopy = textVacansyDescription
                            + parseCVService.br2nl(jobDesxriptionRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                case "projectDescription":
                    strToCopy = textProjectDescription
                            + parseCVService.br2nl(projectDescriptionRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                case "workingConditionsTab":
                    strToCopy = textCompanyWorkingConditions
                            + parseCVService.br2nl(companyWorkingConditionsRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                default:
                    break;
            }
        }

        StringSelection selec = new StringSelection(strToCopy.replaceAll("&nbsp", ""));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selec, selec);

        notifications.create(Notifications.NotificationType.WARNING)
                .withCaption("Информация")
                .withDescription("Скопировано в буфер обмена")
                .show();
    }
}