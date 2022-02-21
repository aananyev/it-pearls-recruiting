package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionDetailScreenFragment")
@UiDescriptor("open-position-detail-screen-fragment.xml")
public class OpenPositionDetailScreenFragment extends ScreenFragment {
    private OpenPosition openPosition = null;
    @Inject
    private Label<String> needExeciseLabel;
    @Inject
    private Label<String> needLetterLabel;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
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
    }
}