package com.company.itpearls.web.screens.openposition.openpositionfragments;

import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.company.itpearls.web.screens.openposition.openpositionfragments.OpenPositionDetailScreenFragment;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionOutstaffDetailScreenFragment")
@UiDescriptor("open-position-outstaff-detail-screen-fragment.xml")
public class OpenPositionOutstaffDetailScreenFragment extends OpenPositionDetailScreenFragment {
    @Inject
    private Label<String> outstaffingCostLabel;
    @Inject
    private Label<String> outstaffingCostValueLabel;
    @Inject
    private MessageBundle messageBundle;

    @Override
    public void setLabels() {
        super.setLabels();

        if (!(openPosition.getSalaryCandidateRequest() == null
                ? false : openPosition.getSalaryCandidateRequest())) {
            if (openPosition.getOutstaffingCost() != null) {
                outstaffingCostLabel.setVisible(true);
                outstaffingCostValueLabel.setVisible(true);

                outstaffingCostValueLabel.setValue(String.valueOf(openPosition.getOutstaffingCost()));
            } else {
                outstaffingCostLabel.setVisible(false);
                outstaffingCostValueLabel.setVisible(false);
            }
        } else {
            outstaffingCostLabel.setVisible(true);
            outstaffingCostValueLabel.setVisible(true);

            outstaffingCostValueLabel.setValue(messageBundle.getMessage("msgSalaryCandidateRequest"));
        }
    }
}