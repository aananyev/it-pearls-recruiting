package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.core.ParseCVService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.VBoxLayout;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.vaadin.ui.JavaScript;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

@UiController("itpearls_QuickViewOpenPositionDescription")
@UiDescriptor("quick-view-open-position-description.xml")
public class QuickViewOpenPositionDescription extends Screen {
    private String jobDescription = "";
    private String projectDescription = "";
    private String companyWorkConditions = "";
    private String companyDescription = "";
    private String jobDescriptionEng = "";

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
    @Inject
    private RichTextArea companyDescriptionRichTextArea;
    @Inject
    private RichTextArea jobDescriptionEngRichTextArea;
    @Named("jobDescriptionViewTab.jobDescriptionEngTab")
    private VBoxLayout jobDescriptionEngTab;
    @Named("jobDescriptionViewTab.projectDescriptionTab")
    private VBoxLayout projectDescriptionTab;
    @Named("jobDescriptionViewTab.workingConditionsTab")
    private VBoxLayout workingConditionsTab;
    @Named("jobDescriptionViewTab.companyDescriptionTab")
    private VBoxLayout companyDescriptionTab;

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

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

    public void setJobDescriptionEng(String jobDescriptionEng) {
        this.jobDescriptionEng = jobDescriptionEng;
    }

    public String getJobDescriptionEng() {
        return jobDescriptionEng;
    }

    public void reloadDescriptions() {
        if (jobDescription != null && !jobDescription.startsWith("нет")) {
            jobDesxriptionRichTextArea.setValue(jobDescription);
        } else {
            if (jobDescriptionEng != null && !jobDescriptionEng.equals("")) {
                jobDesxriptionRichTextArea.setValue(jobDescriptionEng);
                jobDescriptionEngTab.setVisible(false);
            }
        }

        if (jobDescriptionEng != null && !jobDescriptionEng.equals("")) {
            jobDescriptionEngRichTextArea.setValue(jobDescriptionEng);

            if(!jobDescriptionEngRichTextArea.getValue().equals(jobDesxriptionRichTextArea.getValue())) {
               jobDescriptionEngTab.setVisible(true);
            }
        } else {
            jobDescriptionEngTab.setVisible(false);
        }

        if (projectDescription != null && !projectDescription.equals("")) {
            projectDescriptionRichTextArea.setValue(projectDescription);
            projectDescriptionTab.setVisible(true);
        } else {
            projectDescriptionTab.setVisible(false);
        }

        if (companyWorkConditions != null && !companyWorkConditions.equals("")) {
            companyWorkingConditionsRichTextArea.setValue(companyWorkConditions);
            workingConditionsTab.setVisible(true);
        } else {
            workingConditionsTab.setVisible(false);
        }

        if (companyDescription != null && !companyDescription.equals("")) {
            companyDescriptionRichTextArea.setValue(companyDescription);
            companyDescriptionTab.setVisible(true);
        } else {
            companyDescriptionTab.setVisible(false);
        }
    }

    public void copyToClipboard() {
        String strToCopy = "";
        String breakStr = "<br>";
        String breakStr3 = "<br><br><br>";
        String textVacansyDescription = "ОПИСАНИЕ ВАКАНСИИ";
        String textVacansyDescriptionEng = "ОПИСАНИЕ ВАКАНСИИ (Eng)";
        String textProjectDescription = "ОПИСАНИЕ ПРОЕКТА";
        String textCompanyDescription = "ОПИСАНИЕ КОМПАНИИ";
        String textCompanyWorkingConditions = "ОПИСАНИЕ ПРЕИМУЩЕСТВ РАБОТЫ В КОМПАНИИ";

        if (copyAllToClipboardCheckBox.getValue()) {
            strToCopy = parseCVService.br2nl((textVacansyDescription + breakStr
                    + jobDesxriptionRichTextArea.getValue()
                    + breakStr3
                    + jobDescriptionEngRichTextArea.getValue()
                    + breakStr3
                    + textProjectDescription + breakStr
                    + projectDescriptionRichTextArea.getValue()
                    + breakStr3
                    + textCompanyDescription + breakStr
                    + companyDescriptionRichTextArea.getValue()
                    + breakStr3
                    + textCompanyWorkingConditions + breakStr
                    + companyWorkingConditionsRichTextArea.getValue())).replaceAll("\\<.*?\\>", "");

        } else {
            switch (jobDescriptionViewTab.getSelectedTab().getName()) {
                case "jobDescriptionTab":
                    strToCopy = textVacansyDescription + breakStr
                            + parseCVService.br2nl(jobDesxriptionRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                case "jobDescriptionEngTab":
                    strToCopy = textVacansyDescriptionEng + breakStr
                            + parseCVService.br2nl(jobDescriptionEngRichTextArea
                    .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                case "projectDescription":
                    strToCopy = textProjectDescription + breakStr
                            + parseCVService.br2nl(projectDescriptionRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                case "workingConditionsTab":
                    strToCopy = textCompanyWorkingConditions + breakStr
                            + parseCVService.br2nl(companyWorkingConditionsRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
                    break;
                case "companyDescriptionTab":
                    strToCopy = textCompanyDescription + breakStr
                            + parseCVService.br2nl(companyDescriptionRichTextArea
                            .getValue()).replaceAll("\\<.*?\\>", "");
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