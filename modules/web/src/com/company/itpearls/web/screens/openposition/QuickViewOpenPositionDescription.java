package com.company.itpearls.web.screens.openposition;

import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_QuickViewOpenPositionDescription")
@UiDescriptor("quick-view-open-position-description.xml")
public class QuickViewOpenPositionDescription extends Screen {
    String jobDescription = "";
    String projectDescription = "";
    @Inject
    private RichTextArea jobDesxriptionRichTextArea;
    @Inject
    private RichTextArea projectDescriptionRichTextArea;

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void reloadDescriptions() {
        if (jobDescription != null && !jobDescription.equals(""))
            jobDesxriptionRichTextArea.setValue(jobDescription);

        if (projectDescription != null && !projectDescription.equals(""))
            projectDescriptionRichTextArea.setValue(projectDescription);
    }
}