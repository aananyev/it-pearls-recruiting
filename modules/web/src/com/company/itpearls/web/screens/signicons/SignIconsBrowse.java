package com.company.itpearls.web.screens.signicons;

import com.company.itpearls.core.SignIconService;
import com.company.itpearls.entity.ExtUser;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionChangeType;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.SignIcons;
import com.haulmont.cuba.security.global.UserSession;
import com.vaadin.server.Page;

import javax.inject.Inject;
import javax.swing.text.Style;

@UiController("itpearls_SignIcons.browse")
@UiDescriptor("sign-icons-browse.xml")
@LookupComponent("signIconsesTable")
@LoadDataBeforeShow
public class SignIconsBrowse extends StandardLookup<SignIcons> {
    @Inject
    private CollectionContainer<SignIcons> signIconsesDc;
    @Inject
    private UserSession userSession;
    @Inject
    private SignIconService signIconService;
    @Inject
    private CollectionLoader<SignIcons> signIconsesDl;
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        signIconsesDl.setParameter("user", userSession.getUser());
        signIconsesDl.load();

/*        if (checkUserIcons()) {
            createDefaultIcons();

            notifications.create(Notifications.NotificationType.TRAY)
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .withCaption(messageBundle.getMessage("msgInfo"))
                    .withDescription(messageBundle.getMessage("msgCreateDefaultSing"))
                    .show();
        } */
    }

    private void createDefaultIcons() {
        final String icon[] = {CubaIcon.STAR.source(), CubaIcon.STAR.source(), CubaIcon.STAR.source(),
                CubaIcon.FLAG.source(), CubaIcon.FLAG.source(), CubaIcon.FLAG.source()};
        signIconService.createDefaultIcons((ExtUser) userSession.getUser(), icon);
        signIconsesDl.load();
    }

    private boolean checkUserIcons() {
        if (signIconsesDc.getItems().size() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public Component iconSampleGenerator(Entity entity) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);

        retHbox.setWidthFull();
        retHbox.setHeightAuto();

        Label signLabel = uiComponents.create(Label.class);
        signLabel.setWidthAuto();
        signLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        signLabel.setIcon(((SignIcons) entity).getIconName());
        signLabel.setStyleName("pic-center-large-" + ((SignIcons) entity).getIconColor());

        retHbox.add(signLabel);

        return retHbox;
    }

    protected void injectColorCss(String color) {
        Page.Styles styles = Page.getCurrent().getStyles();
        String style = String.format(
                ".pic-center-large-%s {" +
                        "color: #%s;" +
                        "text-align: center;" +
                        "text-color: gray;" +
                        "font-size: large;" +
                        "margin: 0 auto;" +
                        "}",
                color, color);

        styles.add(style);
    }

    @Install(to = "signIconsesTable", subject = "styleProvider")
    private String signIconsesTableStyleProvider(SignIcons entity, String property) {
        if ("color".equals(property) && entity.getIconColor() != null) {
            return "pic-center-large-" + entity.getIconColor();
        }

        return null;
    }

    @Subscribe(id = "signIconsesDc", target = Target.DATA_CONTAINER)
    public void onSignIconsesDcCollectionChange(CollectionContainer.CollectionChangeEvent<SignIcons> event) {
        if (event.getChangeType() == CollectionChangeType.REFRESH) {
            for (SignIcons icons : signIconsesDc.getItems()) {
                injectColorCss(icons.getIconColor());
            }
        }
    }
}