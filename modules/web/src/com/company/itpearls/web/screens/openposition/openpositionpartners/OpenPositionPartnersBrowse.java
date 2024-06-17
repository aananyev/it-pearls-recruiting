package com.company.itpearls.web.screens.openposition.openpositionpartners;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.MessageBundle;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
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
    private Button createBtn;
    @Inject
    private Button removeBtn;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Button editBtn;

    @Subscribe
    public void onAfterShow3(AfterShowEvent event) {
        setOpenPositionDl();

//        createBtn.setVisible(false);
//        editBtn.setVisible(false);
//        removeBtn.setVisible(false);
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
                            .build()
                            .show();
                })
                .withCaption("");
        editButton.setAction(editAction);
        return editButton;
    }
}