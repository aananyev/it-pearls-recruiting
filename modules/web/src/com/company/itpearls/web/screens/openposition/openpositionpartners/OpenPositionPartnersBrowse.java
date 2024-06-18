package com.company.itpearls.web.screens.openposition.openpositionpartners;

import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.web.screens.openposition.openpositionfragments.OpenPositionDetailScreenFragment;
import com.company.itpearls.web.screens.openposition.openpositionfragments.OpenPositionPartnersDetailScreenFragment;
import com.haulmont.cuba.gui.Fragments;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.web.screens.openposition.OpenPositionBrowse;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;

@UiController("itpearls_OpenPositionPartners.browse")
@UiDescriptor("open-position-partners-browse.xml")
public class OpenPositionPartnersBrowse extends OpenPositionBrowse {
    @Inject
    private CollectionLoader<OpenPosition> openPositionsDl;
    @Inject
    private UserSession userSession;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Fragments fragments;

    @Subscribe
    public void onAfterShow3(AfterShowEvent event) {
        setOpenPositionDl();
    }

    private void setOpenPositionDl() {
        openPositionsDl.setParameter("login", userSession.getUser().getLogin());
        openPositionsDl.setParameter("currentDate", new Date());
        openPositionsDl.load();
    }

    protected Component createEditButton(OpenPosition entity) {
        Button editButton = uiComponents.create(Button.class);
        editButton.setCaption(messageBundle.getMessage("msgEdit"));
        editButton.setIcon(CubaIcon.EDIT.iconName());
        editButton.setAlignment(Component.Alignment.TOP_RIGHT);

        BaseAction editAction = new BaseAction("edit")
                .withHandler(actionPerformedEvent -> {
                    screenBuilders.editor(OpenPosition.class, this)
                            .withScreenClass(OpenPositionPartnersEdit.class)
                            .editEntity(entity)
                            .withOpenMode(OpenMode.DIALOG)
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
    }

    protected OpenPositionDetailScreenFragment createOpenPositionDetailScreenFragment(OpenPosition entity) {
        OpenPositionDetailScreenFragment openPositionDetailScreenFragment =
                fragments.create(this, OpenPositionPartnersDetailScreenFragment.class);

        openPositionDetailScreenFragment.setOpenPosition(entity);
        openPositionDetailScreenFragment.setLabels();
        openPositionDetailScreenFragment.setDefaultCompanyLogo();

        return openPositionDetailScreenFragment;
    }

    protected Component createOpenCloseButton(OpenPosition entity) {
        Button btn = (Button) super.createOpenCloseButton(entity);
        btn.setVisible(false);
        return btn;
    }

    protected Component createPriorityField(OpenPosition openPosition) {
        LookupField field = (LookupField) super.createPriorityField(openPosition);
        field.setEnabled(false);
        return field;
    }

    protected Component createSendedCandidatesButton(OpenPosition entity) {
        Button btn = (Button) super.createSendedCandidatesButton(entity);
        btn.setVisible(false);
        return btn;
    }
}