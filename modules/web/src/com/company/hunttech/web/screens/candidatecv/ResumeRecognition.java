package com.company.hunttech.web.screens.candidatecv;

import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("hunttech_ResumeRecognition")
@UiDescriptor("resume-recognition.xml")
public class ResumeRecognition extends Screen {
    @Inject
    private TextArea<String> originalCvTextAred;

    public void setOriginalText(String text) {
        if(text != null) {
            originalCvTextAred.setValue(Jsoup.parse(text).text());
        }
    }
}