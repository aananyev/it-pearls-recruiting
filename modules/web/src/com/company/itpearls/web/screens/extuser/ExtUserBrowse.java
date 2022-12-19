package com.company.itpearls.web.screens.extuser;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import javax.swing.text.html.parser.Entity;

@UiController("itpearls_ExtUserBrowse")
@UiDescriptor("ext-user-browse.xml")
public class ExtUserBrowse extends Screen {

    @Inject
    private UiComponents uiComponents;
    @Inject
    private GroupTable<User> usersTable;

    @Subscribe
    public void onInit(InitEvent event) {
        setColumnIconGenerator();
    }

    private void setColumnIconGenerator() {

    }

    public Component userFaceColumn(ExtUser entity) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        Image retImage = uiComponents.create(Image.class);

        retBox.setWidthFull();
        retBox.setHeightFull();
        retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        retImage.setWidthFull();
        retImage.setHeightFull();
        retImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        retBox.add(retImage);

        return retBox;
    }
}