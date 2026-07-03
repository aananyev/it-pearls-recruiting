package com.company.hunttech.web.screens.candidatecv;

import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("hunttech_ViewerTextScreen")
@UiDescriptor("viewer-text-screen.xml")
public class ViewerTextScreen extends Screen {
    @Inject
    private RichTextArea textViewerRichTextArea;

    public void setTextToArea(String textToArea) {
        textViewerRichTextArea.setValue(textToArea);
    }
}