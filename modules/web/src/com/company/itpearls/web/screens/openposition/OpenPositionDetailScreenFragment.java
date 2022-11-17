package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionDetailScreenFragment")
@UiDescriptor("open-position-detail-screen-fragment.xml")
public class OpenPositionDetailScreenFragment extends ScreenFragment {
    private OpenPosition openPosition = null;
    @Inject
    private Label<String> needExeciseLabel;
    @Inject
    private Label<String> needLetterLabel;
    @Inject
    private Label<String> salaryComment1;
    @Inject
    private Label<String> salaryComment2;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setLabels() {
        if (openPosition != null) {
            if (openPosition.getNeedExercise() != null) {
                if (openPosition.getNeedExercise()) {
                    needExeciseLabel.setVisible(true);
                } else {
                    needExeciseLabel.setVisible(false);
                }
            }

            if (openPosition.getNeedLetter() != null) {
                if (openPosition.getNeedLetter()) {
                    needLetterLabel.setVisible(true);
                } else {
                    needLetterLabel.setVisible(false);
                }
            }
        }

        if (openPosition.getSalaryComment() != null) {
            salaryComment1.setValue("\ud83d\udcc3");
            salaryComment2.setValue(" \ud83d\udcc3");

            salaryComment1.setDescription(openPosition.getSalaryComment());
            salaryComment2.setDescription(openPosition.getSalaryComment());
        }
    }
}