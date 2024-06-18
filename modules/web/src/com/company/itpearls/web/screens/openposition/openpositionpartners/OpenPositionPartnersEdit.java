package com.company.itpearls.web.screens.openposition.openpositionpartners;

import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.openposition.OpenPositionEdit;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionPartners.edit")
@UiDescriptor("open-position-partners-edit.xml")
public class OpenPositionPartnersEdit extends OpenPositionEdit {
    @Inject
    private Button windowCommitAndCloseButton;

    @Subscribe
    public void onBeforeShow2(BeforeShowEvent event) {
        windowCommitAndCloseButton.setVisible(false);
    }
}