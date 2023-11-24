package com.company.itpearls.web.screens.openposition.openpositionviews;

import com.company.itpearls.core.ParseCVService;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.screen.*;
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
    private String cvRequirement = "";

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
    @Inject
    private RichTextArea cvRequirementsRichTextArea;
    @Named("jobDescriptionViewTab.cvRequirementsTab")
    private VBoxLayout cvRequirementsTab;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private TextArea<String> jobDesxriptionTextArea;

    @Subscribe
    public void onInit(InitEvent event) {
        jobDescriptionViewTab.addSelectedTabChangeListener(e -> {
            setDisableCopyToClipboardButton();
        });
    }

    public void setCvRequirement(String cvRequirement) {
        this.cvRequirement = cvRequirement;
    }

    public String getCvRequirement() {
        return cvRequirement;
    }

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

            if (!jobDescriptionEngRichTextArea.getValue().equals(jobDesxriptionRichTextArea.getValue())) {
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

        if (cvRequirement != null && !cvRequirement.equals("")) {
            cvRequirementsRichTextArea.setValue(cvRequirement);
            cvRequirementsTab.setVisible(true);
        } else {
            cvRequirementsTab.setVisible(false);
        }
    }

    private Boolean setDisableCopyToClipboardButton() {
        Boolean retBool = false;

        if (copyAllToClipboardCheckBox.getValue() != true) {
            switch (jobDescriptionViewTab.getSelectedTab().getName()) {
                case "jobDescriptionTab":
                    retBool = jobDesxriptionRichTextArea.getValue() != null ? true : false;
                    break;
                case "jobDescriptionEngTab":
                    retBool = jobDescriptionEngRichTextArea.getValue() != null ? true : false;
                    break;
                case "projectDescription":
                    retBool = projectDescriptionRichTextArea.getValue() != null ? true : false;
                    break;
                case "workingConditionsTab":
                    retBool = companyWorkingConditionsRichTextArea.getValue() != null ? true : false;
                    break;
                case "companyDescriptionTab":
                    retBool = companyDescriptionRichTextArea.getValue() != null ? true : false;
                    break;
                case "cvRequirementsTab":
                    retBool = cvRequirementsRichTextArea.getValue() != null ? true : false;
                    break;
                default:
                    break;
            }
        } else {
            retBool = true;
        }

        return retBool;
    }

    public void setClipboardContents(String string) {
        StringSelection stringSelection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);
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
            strToCopy = parseCVService.br2nl((new StringBuilder()
                    .append(textVacansyDescription)
                    .append(breakStr)
                    .append(jobDesxriptionRichTextArea.getValue())
                    .append(breakStr3)
                    .append(jobDescriptionEngRichTextArea.getValue())
                    .append(breakStr3)
                    .append(textProjectDescription)
                    .append(breakStr)
                    .append(projectDescriptionRichTextArea.getValue())
                    .append(breakStr3)
                    .append(textCompanyDescription)
                    .append(breakStr)
                    .append(companyDescriptionRichTextArea.getValue())
                    .append(breakStr3)
                    .append(textCompanyWorkingConditions)
                    .append(breakStr)
                    .append(companyWorkingConditionsRichTextArea.getValue()).toString())
            ).replaceAll("\\<.*?\\>", "");

        } else {
            switch (jobDescriptionViewTab.getSelectedTab().getName()) {
                case "jobDescriptionTab":
                    strToCopy = new StringBuilder()
                            .append(textVacansyDescription)
                            .append(breakStr)
                            .append(parseCVService.br2nl(jobDesxriptionRichTextArea
                                    .getValue()))
                            .toString()
                            .replaceAll("\\<.*?\\>", "");
                    break;
                case "jobDescriptionEngTab":
                    if (jobDescriptionEngRichTextArea.getValue() != null) {
                        strToCopy = new StringBuilder()
                                .append(textVacansyDescriptionEng)
                                .append(breakStr)
                                .append(parseCVService.br2nl(jobDescriptionEngRichTextArea.getValue())).toString()
                                .replaceAll("\\<.*?\\>", "");
                    }

                    break;
                case "projectDescription":
                    if (projectDescriptionRichTextArea.getValue() != null) {
                        strToCopy = new StringBuilder()
                                .append(textProjectDescription)
                                .append(breakStr)
                                .append(parseCVService.br2nl(projectDescriptionRichTextArea.getValue()))
                                .toString()
                                .replaceAll("\\<.*?\\>", "");
                    }
                    break;
                case "workingConditionsTab":
                    if (companyWorkingConditionsRichTextArea != null) {
                        strToCopy = new StringBuilder()
                                .append(textCompanyWorkingConditions)
                                .append(breakStr)
                                .append(parseCVService.br2nl(companyWorkingConditionsRichTextArea.getValue()))
                                .toString()
                                .replaceAll("\\<.*?\\>", "");
                    }
                    break;
                case "companyDescriptionTab":
                    if (companyDescriptionRichTextArea.getValue() != null) {
                        strToCopy = new StringBuilder()
                                .append(textCompanyDescription)
                                .append(breakStr)
                                .append(parseCVService.br2nl(companyDescriptionRichTextArea.getValue()))
                                .toString()
                                .replaceAll("\\<.*?\\>", "");
                    }

                    break;
                default:
                    break;
            }
        }

        if (strToCopy != null) {

            setClipboardContents(strToCopy);

            notifications.create(Notifications.NotificationType.SYSTEM)
                    .withCaption(messageBundle.getMessage("msgInfo"))
                    .withDescription("Скопировано в буфер обмена")
                    .show();
        } else {
            notifications.create(Notifications.NotificationType.SYSTEM)
                    .withCaption(messageBundle.getMessage("msgInfo"))
                    .withDescription("Нечего копировать в буфер обмена")
                    .show();
        }
    }

    @Subscribe("copyToClipboard")
    public void onCopyToClipboardClick(Button.ClickEvent event) {
        notifications.create().withCaption("Copied to clipboard").show();
    }

    @Subscribe("jobDesxriptionRichTextArea")
    public void onJobDesxriptionRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        jobDesxriptionTextArea.setValue(
                Jsoup.parse(
                        jobDescriptionEngRichTextArea.getValue() != null
                                ? parseCVService.br2nl(companyDescriptionRichTextArea.getValue())
                                : "").text());
    }
}