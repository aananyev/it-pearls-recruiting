package com.company.itpearls.web.screens;

import com.haulmont.cuba.gui.components.ProgressBar;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_ProgressBarScreen")
@UiDescriptor("progress-bar-screen.xml")
public class ProgressBarScreen extends Screen {
    @Inject
    private ProgressBar progressBar;

    private int maxValue = 0;

    public void setProgress(int value) {
        if(maxValue != 0) {
            progressBar.setValue((double) (value / maxValue));
        }
    }

    public void setMaxValue(int value) {
        this.maxValue = value;
    }
}