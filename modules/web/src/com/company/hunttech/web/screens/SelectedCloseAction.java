package com.company.hunttech.web.screens;

import com.company.hunttech.entity.CandidateCV;
import com.haulmont.cuba.gui.screen.StandardCloseAction;

public class SelectedCloseAction extends StandardCloseAction {
    private CandidateCV.SelectedCloseActionType result;

    public SelectedCloseAction(CandidateCV.SelectedCloseActionType result) {
        super("selectedCloseAction");
        this.result = result;
    }

    public CandidateCV.SelectedCloseActionType getResult() {
        return result;
    }
}
